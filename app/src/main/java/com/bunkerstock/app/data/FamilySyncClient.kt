package com.bunkerstock.app.data

import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object FamilySyncClient {

    const val DEFAULT_SERVER = "https://api.restful-api.dev/objects"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    fun generateSecretKey(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun deriveAesKey(secretKey: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(secretKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    fun encryptSnapshot(snapshot: FamilySyncSnapshot, secretKey: String): String {
        val plain = InventoryLogic.json.encodeToString(snapshot).toByteArray(Charsets.UTF_8)
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveAesKey(secretKey), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain)
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decryptSnapshot(cipherBase64: String, secretKey: String): FamilySyncSnapshot? = runCatching {
        val combined = Base64.getDecoder().decode(cipherBase64)
        if (combined.size <= GCM_IV_BYTES) return null
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(secretKey), GCMParameterSpec(GCM_TAG_BITS, iv))
        val plain = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        InventoryLogic.json.decodeFromString<FamilySyncSnapshot>(plain)
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

    /**
     * Strips device-local filesystem photo paths so sync stays tiny & private.
     * Remote devices resolve product photos automatically via Open Food Facts / web lookup.
     */
    private fun sanitizeForUpload(items: List<InventoryItem>): List<InventoryItem> =
        items.map { item ->
            val remoteImg = item.imageUri?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            item.copy(imageUri = remoteImg)
        }

    /**
     * Conflict-free per-item Last-Write-Wins merge preserving local custom photos.
     */
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
                    // Preserve local custom replaced image on this device
                    val localCustomImg = l.imageUri?.takeIf { !it.startsWith("http://") && !it.startsWith("https://") }
                    if (localCustomImg != null) chosen.copy(imageUri = localCustomImg) else chosen
                }
                l != null -> l
                r != null -> r
                else -> continue
            }

            if (delTs > winner.updatedAt) {
                continue
            }
            result.add(winner)
        }

        return result.sortedByDescending { it.updatedAt } to mergedDeleted
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
            val secret = generateSecretKey()
            val stamped = sanitizeForUpload(localItems).map {
                if (it.updatedAt == 0L) it.copy(updatedAt = now, updatedBy = memberName) else it
            }
            val snapshot = FamilySyncSnapshot(
                groupName = groupName.trim().ifBlank { "Familia" },
                items = stamped,
                deletedIds = localDeleted,
                updatedAt = now
            )
            val cipher = encryptSnapshot(snapshot, secret)
            val bodyJson = """{"name":"bs-e2e","data":{"v":1,"cipher":"$cipher"}}"""

            val conn = (URL(serverBaseUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "BunkerStock-Android/3.0")
            }
            try {
                conn.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) {
                    error("HTTP ${conn.responseCode}")
                }
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val root = InventoryLogic.json.parseToJsonElement(resp).jsonObject
                val roomId = root["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: error("Missing room id")
                FamilyGroupConfig(
                    roomId = roomId,
                    secretKey = secret,
                    groupName = snapshot.groupName,
                    memberName = memberName.trim(),
                    serverBaseUrl = serverBaseUrl,
                    lastSyncedAt = now
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun syncGroup(
        config: FamilyGroupConfig,
        localItems: List<InventoryItem>,
        localDeleted: Map<String, Long>,
        isInitialJoin: Boolean = false
    ): Result<Triple<List<InventoryItem>, Map<String, Long>, FamilyGroupConfig>> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = config.serverBaseUrl.trimEnd('/')
            val endpoint = "$baseUrl/${config.roomId}"

            // 1. GET encrypted remote snapshot
            val getConn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "BunkerStock-Android/3.0")
            }
            val remoteSnapshot: FamilySyncSnapshot? = try {
                if (getConn.responseCode in 200..299) {
                    val resp = getConn.inputStream.bufferedReader().use { it.readText() }
                    val root = InventoryLogic.json.parseToJsonElement(resp) as? JsonObject
                    val data = root?.get("data") as? JsonObject
                    val cipher = data?.get("cipher")?.jsonPrimitive?.contentOrNull
                    cipher?.let { decryptSnapshot(it, config.secretKey) }
                } else {
                    null
                }
            } finally {
                getConn.disconnect()
            }

            if (remoteSnapshot == null) {
                error("Could not decrypt or reach family group")
            }

            // On initial join, adopt family stock cleanly if local only had default demo items
            val effectiveLocal = if (isInitialJoin && localItems.all { it.id.startsWith("item-") && it.id.length <= 7 }) {
                emptyList()
            } else {
                localItems
            }

            val (mergedItems, mergedDeleted) = mergeSnapshots(effectiveLocal, localDeleted, remoteSnapshot)
            val now = System.currentTimeMillis()
            val updatedSnapshot = FamilySyncSnapshot(
                groupName = remoteSnapshot.groupName.ifBlank { config.groupName },
                items = sanitizeForUpload(mergedItems),
                deletedIds = mergedDeleted,
                updatedAt = now
            )

            // 2. PUT merged encrypted snapshot back
            val cipher = encryptSnapshot(updatedSnapshot, config.secretKey)
            val bodyJson = """{"name":"bs-e2e","data":{"v":1,"cipher":"$cipher"}}"""
            val putConn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "BunkerStock-Android/3.0")
            }
            try {
                putConn.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
                if (putConn.responseCode !in 200..299) {
                    error("Sync upload HTTP ${putConn.responseCode}")
                }
            } finally {
                putConn.disconnect()
            }

            Triple(
                mergedItems,
                mergedDeleted,
                config.copy(
                    groupName = updatedSnapshot.groupName,
                    lastSyncedAt = now
                )
            )
        }
    }
}
