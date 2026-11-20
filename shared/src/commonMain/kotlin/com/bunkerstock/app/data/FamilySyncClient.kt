package com.bunkerstock.app.data

import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.PlatformCrypto
import com.bunkerstock.app.platform.PlatformHttp
import com.bunkerstock.app.platform.currentTimeMillis
import com.bunkerstock.app.platform.isWebPlatform
import com.bunkerstock.app.platform.urlDecodeUtf8
import com.bunkerstock.app.platform.urlEncodeUtf8
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FamilySyncResult(
    val items: List<InventoryItem>,
    val deletedIds: Map<String, Long>,
    val config: FamilyGroupConfig,
    val localChanged: Boolean,
    val remoteUploaded: Boolean,
    val remoteCipherHash: String,
    val localStateHash: String
)

object FamilySyncClient {

    const val DEFAULT_SERVER = "https://textdb.dev/api/data"
    private const val GCM_IV_BYTES = 12

    suspend fun sha256Hex(input: String): String = PlatformCrypto.sha256Hex(input)

    suspend fun computeStateHash(items: List<InventoryItem>, deletedIds: Map<String, Long>): String {
        val canonical = sanitizeForUpload(items).sortedBy { it.id }
        val deletedSorted = deletedIds.entries.sortedBy { it.key }.associate { it.key to it.value }
        return sha256Hex(
            InventoryLogic.json.encodeToString(canonical) + "|" + InventoryLogic.json.encodeToString(deletedSorted)
        )
    }

    fun generateSecretKey(): String {
        val bytes = PlatformCrypto.randomBytes(16)
        return PlatformCrypto.base64UrlEncodeNoPad(bytes)
    }

    fun generateRoomId(): String {
        val bytes = PlatformCrypto.randomBytes(12)
        val hex = "0123456789abcdef"
        val sb = StringBuilder("bs_")
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hex[v ushr 4])
            sb.append(hex[v and 0xF])
        }
        return sb.toString()
    }

    suspend fun encryptSnapshot(snapshot: FamilySyncSnapshot, secretKey: String): String {
        val plain = InventoryLogic.json.encodeToString(snapshot).encodeToByteArray()
        val combined = PlatformCrypto.encryptGzipAesGcm(plain, secretKey)
        return "v2:" + PlatformCrypto.base64Encode(combined)
    }

    suspend fun decryptSnapshot(rawPayload: String, secretKey: String): FamilySyncSnapshot? = runCatching {
        val isGzipped = rawPayload.startsWith("v2:")
        val b64 = if (isGzipped) rawPayload.removePrefix("v2:") else rawPayload
        val combined = PlatformCrypto.base64Decode(b64)
        if (combined.size <= GCM_IV_BYTES) return null
        val jsonBytes = PlatformCrypto.decryptAesGcmMaybeGunzip(combined, secretKey, gunzip = isGzipped)
        InventoryLogic.json.decodeFromString<FamilySyncSnapshot>(jsonBytes.decodeToString())
    }.getOrNull()

    fun buildInviteLink(config: FamilyGroupConfig, webBaseUrl: String? = null): String {
        val encName = urlEncodeUtf8(config.groupName.ifBlank { "Familia" })
        val params = buildString {
            append("r=${urlEncodeUtf8(config.roomId)}")
            append("&k=${urlEncodeUtf8(config.secretKey)}")
            append("&g=$encName")
            if (config.serverBaseUrl != DEFAULT_SERVER && config.serverBaseUrl.isNotBlank()) {
                append("&s=${urlEncodeUtf8(config.serverBaseUrl)}")
            }
        }
        return if (!webBaseUrl.isNullOrBlank()) {
            "${webBaseUrl.trimEnd('/')}/#join?$params"
        } else {
            "bunkerstock://join?$params"
        }
    }

    fun isInviteLink(raw: String): Boolean {
        val t = raw.trim()
        return t.contains("bunkerstock://join", ignoreCase = true) ||
            t.contains("#join?", ignoreCase = true)
    }

    fun parseInviteLink(raw: String): FamilyGroupConfig? {
        val trimmed = raw.trim()
        val schemeIdx = trimmed.indexOf("bunkerstock://join", ignoreCase = true)
        val hashIdx = trimmed.indexOf("#join?", ignoreCase = true)
        val startIdx = when {
            schemeIdx >= 0 -> schemeIdx
            hashIdx >= 0 -> hashIdx
            else -> return null
        }
        val link = trimmed.substring(startIdx).split(Regex("\\s+")).firstOrNull() ?: return null
        val query = link.substringAfter("?", "")
        if (query.isBlank()) return null
        val params = query.split("&").mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0) null
            else pair.substring(0, eq) to urlDecodeUtf8(pair.substring(eq + 1))
        }.toMap()
        val roomId = params["r"]?.trim().orEmpty()
        val key = params["k"]?.trim().orEmpty()
        if (roomId.isBlank() || key.isBlank()) return null
        val groupName = params["g"]?.trim().orEmpty().ifBlank { "Familia" }
        val server = params["s"]?.trim().orEmpty().ifBlank { DEFAULT_SERVER }
        return FamilyGroupConfig(
            roomId = roomId,
            secretKey = key,
            groupName = groupName,
            serverBaseUrl = server
        )
    }

    private fun sanitizeForUpload(items: List<InventoryItem>): List<InventoryItem> =
        items.map { item ->
            val remoteImg = item.imageUri?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            item.copy(imageUri = remoteImg)
        }

    fun mergeSnapshots(
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        remote: FamilySyncSnapshot
    ): Pair<List<InventoryItem>, Map<String, Long>> {
        val mergedDeleted = localDeleted.toMutableMap()
        for ((id, ts) in remote.deletedIds) {
            val prev = mergedDeleted[id] ?: 0L
            if (ts > prev) mergedDeleted[id] = ts
        }

        val localMap = localItems.associateBy { it.id }
        val remoteMap = remote.items.associateBy { it.id }
        val allIds = (localMap.keys + remoteMap.keys).toSet()

        val result = mutableListOf<InventoryItem>()
        for (id in allIds) {
            val l = localMap[id]
            val r = remoteMap[id]
            val delTs = mergedDeleted[id] ?: 0L

            val winner = when {
                l != null && r != null -> {
                    val chosen = if (r.updatedAt > l.updatedAt) r else l
                    val localCustomImg = l.imageUri?.takeIf { !it.startsWith("http://") && !it.startsWith("https://") }
                    if (localCustomImg != null) chosen.copy(imageUri = localCustomImg) else chosen
                }
                l != null -> l
                r != null -> r
                else -> continue
            }

            if (delTs > winner.updatedAt) continue
            result.add(winner)
        }

        return result.sortedByDescending { it.updatedAt } to mergedDeleted
    }

    private suspend fun writeCipherToEndpoint(endpoint: String, cipherText: String) {
        val headers = if (isWebPlatform) emptyMap() else mapOf(
            "Accept" to "text/plain, */*",
            "User-Agent" to "BunkerStock-Android/3.4"
        )
        val ok = PlatformHttp.postText(
            url = endpoint,
            body = cipherText,
            contentType = "text/plain",
            headers = headers,
            timeoutMs = 10000
        )
        if (!ok) error("HTTP write failed")
    }

    private suspend fun readCipherFromEndpoint(endpoint: String): String? {
        val headers = if (isWebPlatform) emptyMap() else mapOf("User-Agent" to "BunkerStock-Android/3.4")
        val raw = PlatformHttp.getText(url = endpoint, headers = headers, timeoutMs = 10000)?.trim()
            ?: return null
        if (raw.isBlank() || raw == "null") return null
        return when {
            raw.startsWith("\"") && raw.endsWith("\"") ->
                runCatching { InventoryLogic.json.decodeFromString<String>(raw) }.getOrDefault(raw.trim('"'))
            raw.startsWith("{") -> {
                val obj = runCatching { InventoryLogic.json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
                val data = obj?.get("data") as? JsonObject
                data?.get("cipher")?.jsonPrimitive?.contentOrNull
                    ?: obj?.get("cipher")?.jsonPrimitive?.contentOrNull
            }
            else -> raw
        }
    }

    suspend fun createGroup(
        groupName: String,
        memberName: String,
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        serverBaseUrl: String = DEFAULT_SERVER
    ): Result<FamilyGroupConfig> = runCatching {
        val now = currentTimeMillis()
        val roomId = generateRoomId()
        val secret = generateSecretKey()
        val stamped = sanitizeForUpload(localItems).map {
            val author = it.addedBy.ifBlank { memberName.trim() }
            if (it.updatedAt == 0L) it.copy(addedBy = author, updatedAt = now, updatedBy = memberName.trim())
            else it.copy(addedBy = author)
        }
        val snapshot = FamilySyncSnapshot(
            groupName = groupName.trim().ifBlank { "Familia" },
            items = stamped,
            deletedIds = localDeleted,
            updatedAt = now
        )
        val cipher = encryptSnapshot(snapshot, secret)
        val endpoint = "${serverBaseUrl.trimEnd('/')}/$roomId"
        writeCipherToEndpoint(endpoint, cipher)

        FamilyGroupConfig(
            roomId = roomId,
            secretKey = secret,
            groupName = snapshot.groupName,
            memberName = memberName.trim(),
            serverBaseUrl = serverBaseUrl,
            lastSyncedAt = now
        )
    }

    suspend fun syncGroup(
        config: FamilyGroupConfig,
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        isInitialJoin: Boolean = false
    ): Result<Triple<List<InventoryItem>, Map<String, Long>, FamilyGroupConfig>> =
        syncGroupDiff(
            config = config,
            localItems = localItems,
            localDeleted = localDeleted,
            isInitialJoin = isInitialJoin
        ).map { Triple(it.items, it.deletedIds, it.config) }

    suspend fun syncGroupDiff(
        config: FamilyGroupConfig,
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        isInitialJoin: Boolean = false,
        lastRemoteHash: String = "",
        lastLocalHash: String = ""
    ): Result<FamilySyncResult> = runCatching {
        val endpoint = "${config.serverBaseUrl.trimEnd('/')}/${config.roomId}"
        val cipherIn = readCipherFromEndpoint(endpoint)
            ?: error("Grupo no encontrado en el servidor")
        val remoteCipherHash = sha256Hex(cipherIn)
        val currentLocalHash = computeStateHash(localItems, localDeleted)

        if (!isInitialJoin &&
            lastRemoteHash.isNotEmpty() &&
            remoteCipherHash == lastRemoteHash &&
            currentLocalHash == lastLocalHash
        ) {
            return@runCatching FamilySyncResult(
                items = localItems,
                deletedIds = localDeleted,
                config = config,
                localChanged = false,
                remoteUploaded = false,
                remoteCipherHash = remoteCipherHash,
                localStateHash = currentLocalHash
            )
        }

        val remoteSnapshot = decryptSnapshot(cipherIn, config.secretKey)
            ?: error("No se pudo descifrar el grupo (clave incorrecta)")

        val effectiveLocal = if (isInitialJoin && localItems.all { it.id.startsWith("item-") && it.id.length <= 7 }) {
            emptyList()
        } else {
            localItems
        }

        val (mergedItems, mergedDeleted) = mergeSnapshots(effectiveLocal, localDeleted, remoteSnapshot)
        val mergedStateHash = computeStateHash(mergedItems, mergedDeleted)
        val remoteStateHash = computeStateHash(remoteSnapshot.items, remoteSnapshot.deletedIds)

        val localChanged = isInitialJoin || (mergedStateHash != currentLocalHash) ||
            (mergedItems.size != localItems.size)
        val needsUpload = mergedStateHash != remoteStateHash

        val now = currentTimeMillis()
        val finalCipherHash = if (needsUpload) {
            val updatedSnapshot = FamilySyncSnapshot(
                groupName = remoteSnapshot.groupName.ifBlank { config.groupName },
                items = sanitizeForUpload(mergedItems),
                deletedIds = mergedDeleted,
                updatedAt = now
            )
            val cipherOut = encryptSnapshot(updatedSnapshot, config.secretKey)
            writeCipherToEndpoint(endpoint, cipherOut)
            sha256Hex(cipherOut)
        } else {
            remoteCipherHash
        }

        val updatedConfig = config.copy(
            groupName = remoteSnapshot.groupName.ifBlank { config.groupName },
            lastSyncedAt = if (localChanged || needsUpload || config.lastSyncedAt == 0L) now else config.lastSyncedAt
        )

        FamilySyncResult(
            items = mergedItems,
            deletedIds = mergedDeleted,
            config = updatedConfig,
            localChanged = localChanged,
            remoteUploaded = needsUpload,
            remoteCipherHash = finalCipherHash,
            localStateHash = mergedStateHash
        )
    }
}
