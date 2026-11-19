package com.bunkerstock.app.data

import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

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
    private const val GCM_TAG_BITS = 128

    fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun computeStateHash(items: List<InventoryItem>, deletedIds: Map<String, Long>): String {
        val canonical = sanitizeForUpload(items).sortedBy { it.id }
        val deletedSorted = deletedIds.toSortedMap()
        return sha256Hex(
            InventoryLogic.json.encodeToString(canonical) + "|" + InventoryLogic.json.encodeToString(deletedSorted)
        )
    }

    fun generateSecretKey(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generateRoomId(): String {
        val bytes = ByteArray(12)
        SecureRandom().nextBytes(bytes)
        return "bs_" + bytes.joinToString("") { "%02x".format(it) }
    }

    private fun deriveAesKey(secretKey: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(secretKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    private fun gunzip(bytes: ByteArray): ByteArray =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }

    fun encryptSnapshot(snapshot: FamilySyncSnapshot, secretKey: String): String {
        val compressed = gzip(InventoryLogic.json.encodeToString(snapshot).toByteArray(Charsets.UTF_8))
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(secretKey), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(compressed)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return "v2:" + Base64.getEncoder().encodeToString(combined)
    }

    fun decryptSnapshot(rawPayload: String, secretKey: String): FamilySyncSnapshot? = runCatching {
        val isGzipped = rawPayload.startsWith("v2:")
        val b64 = if (isGzipped) rawPayload.removePrefix("v2:") else rawPayload
        val combined = Base64.getDecoder().decode(b64)
        if (combined.size <= GCM_IV_BYTES) return null
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(secretKey), GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(encrypted)
        val jsonBytes = if (isGzipped) gunzip(plainBytes) else plainBytes
        InventoryLogic.json.decodeFromString<FamilySyncSnapshot>(jsonBytes.toString(Charsets.UTF_8))
    }.getOrNull()

    fun buildInviteLink(config: FamilyGroupConfig): String {
        val encName = URLEncoder.encode(config.groupName.ifBlank { "Familia" }, "UTF-8")
        val base = "bunkerstock://join?r=${URLEncoder.encode(config.roomId, "UTF-8")}&k=${URLEncoder.encode(config.secretKey, "UTF-8")}&g=$encName"
        return if (config.serverBaseUrl != DEFAULT_SERVER && config.serverBaseUrl.isNotBlank()) {
            "$base&s=${URLEncoder.encode(config.serverBaseUrl, "UTF-8")}"
        } else {
            base
        }
    }

    fun parseInviteLink(raw: String): FamilyGroupConfig? {
        val trimmed = raw.trim()
        val startIdx = trimmed.indexOf("bunkerstock://join", ignoreCase = true)
        if (startIdx < 0) return null
        val link = trimmed.substring(startIdx).split(Regex("\\s+")).firstOrNull() ?: return null
        val query = link.substringAfter("?", "")
        if (query.isBlank()) return null
        val params = query.split("&").mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0) null
            else pair.substring(0, eq) to URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
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

    private fun writeCipherToEndpoint(endpoint: String, cipherText: String) {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "text/plain")
            setRequestProperty("Accept", "text/plain, */*")
            setRequestProperty("User-Agent", "BunkerStock-Android/3.1")
        }
        try {
            conn.outputStream.use { it.write(cipherText.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                error("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun readCipherFromEndpoint(endpoint: String): String? {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "BunkerStock-Android/3.1")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
            if (raw.isBlank() || raw == "null") return null
            // textdb.dev returns a JSON-quoted string; custom workers may return JSON {"data":{"cipher":"..."}}
            when {
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
        } finally {
            conn.disconnect()
        }
    }

    suspend fun createGroup(
        groupName: String,
        memberName: String,
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        serverBaseUrl: String = DEFAULT_SERVER
    ): Result<FamilyGroupConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
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

    /**
     * Diff-aware family sync:
     * 1. Fetches remote encrypted blob and hashes it; if neither remote nor local changed since last sync, skips merge & upload.
     * 2. Merges via LWW when any diff exists, updates local only if changed, and uploads only if merged state differs from remote.
     */
    suspend fun syncGroupDiff(
        config: FamilyGroupConfig,
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        isInitialJoin: Boolean = false,
        lastRemoteHash: String = "",
        lastLocalHash: String = ""
    ): Result<FamilySyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "${config.serverBaseUrl.trimEnd('/')}/${config.roomId}"
            val cipherIn = readCipherFromEndpoint(endpoint)
                ?: error("Grupo no encontrado en el servidor")
            val remoteCipherHash = sha256Hex(cipherIn)
            val currentLocalHash = computeStateHash(localItems, localDeleted)

            // Fast path: zero remote diffs AND zero local diffs since last sync!
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

            val now = System.currentTimeMillis()
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
}
