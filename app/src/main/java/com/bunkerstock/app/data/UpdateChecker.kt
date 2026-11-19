package com.bunkerstock.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.bunkerstock.app.BuildConfig
import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val RELEASES_API =
        "https://api.github.com/repos/Emefedez/bunker-stock-android/releases/latest"

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = remoteTag.trimStart('v', 'V').split(".").mapNotNull { it.toIntOrNull() }
        val localParts = currentVersion.trimStart('v', 'V').split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    /**
     * Read-only check against GitHub Releases. Never uploads any user or inventory data.
     */
    suspend fun checkForUpdate(currentVersion: String = BuildConfig.VERSION_NAME): AppUpdateInfo? =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "BunkerStock-Android/$currentVersion")
                }
                if (conn.responseCode !in 200..299) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = InventoryLogic.json.parseToJsonElement(body) as? JsonObject ?: return@withContext null
                val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@withContext null
                if (!isNewerVersion(tag, currentVersion)) return@withContext null

                val htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val assets = root["assets"] as? JsonArray
                val apkUrl = assets
                    ?.mapNotNull { it as? JsonObject }
                    ?.firstOrNull {
                        it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true
                    }
                    ?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
                    .orEmpty()

                if (apkUrl.isBlank()) return@withContext null
                AppUpdateInfo(
                    versionName = tag.trimStart('v', 'V'),
                    releaseNotes = notes,
                    apkDownloadUrl = apkUrl,
                    htmlUrl = htmlUrl
                )
            } catch (_: Exception) {
                null
            } finally {
                conn?.disconnect()
            }
        }

    suspend fun downloadAndInstallApk(context: Context, update: AppUpdateInfo): Boolean =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val dir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
                val apkFile = File(dir, "BunkerStock-v${update.versionName}.apk")
                conn = (URL(update.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 10000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "BunkerStock-Android/${BuildConfig.VERSION_NAME}")
                }
                if (conn.responseCode !in 200..299) return@withContext false
                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permIntent)
                    return@withContext true
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            } finally {
                conn?.disconnect()
            }
        }
}
