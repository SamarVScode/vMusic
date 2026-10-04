package com.glass.player.domain.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context,
    private val currentVersionName: String,
    private val okHttpClient: OkHttpClient = defaultHttpClient()
) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    companion object {
        private const val GITHUB_OWNER = "SamarVScode"
        private const val GITHUB_REPO = "vMusic"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        private const val ALL_RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
        private const val UPDATE_FILE_NAME = "vMusic-update.apk"
        private const val PROGRESS_INTERVAL_MS = 150L

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    suspend fun checkForUpdates(): UpdateState = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            val error = UpdateState.Error("No active network connection")
            _updateState.value = error
            return@withContext error
        }

        _updateState.value = UpdateState.Checking

        try {
            val releaseInfo = fetchLatestRelease()
            if (releaseInfo == null) {
                val error = UpdateState.Error("No release data found on GitHub")
                _updateState.value = error
                return@withContext error
            }

            if (isNewerVersion(releaseInfo.versionName, currentVersionName)) {
                val available = UpdateState.Available(
                    versionName = releaseInfo.versionName,
                    changelog = releaseInfo.body,
                    downloadUrl = releaseInfo.downloadUrl,
                    fileSizeMb = releaseInfo.fileSizeBytes / (1024.0 * 1024.0)
                )
                _updateState.value = available
                available
            } else {
                val upToDate = UpdateState.UpToDate
                _updateState.value = upToDate
                upToDate
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = UpdateState.Error("Update check failed: ${e.localizedMessage ?: "Unknown error"}", e)
            _updateState.value = error
            error
        }
    }

    suspend fun downloadUpdate(downloadUrl: String): UpdateState = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            val error = UpdateState.Error("No active network connection")
            _updateState.value = error
            return@withContext error
        }

        _updateState.value = UpdateState.Downloading(progress = 0f, downloadedMb = 0.0, totalMb = 0.0)

        val updatesDir = File(context.cacheDir, "updates").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(updatesDir, UPDATE_FILE_NAME)
        val tempFile = File(updatesDir, "$UPDATE_FILE_NAME.tmp")

        try {
            if (tempFile.exists()) tempFile.delete()
            if (targetFile.exists()) targetFile.delete()

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Accept", "application/octet-stream")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Server responded with HTTP code ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw IOException("Empty response body from download endpoint")
            val totalBytes = body.contentLength()
            val totalMb = if (totalBytes > 0) totalBytes / (1024.0 * 1024.0) else 0.0

            var bytesCopied = 0L
            var lastEmittedTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmittedTime >= PROGRESS_INTERVAL_MS) {
                            lastEmittedTime = now
                            val progress = if (totalBytes > 0) bytesCopied.toFloat() / totalBytes else 0f
                            val downloadedMb = bytesCopied / (1024.0 * 1024.0)
                            _updateState.value = UpdateState.Downloading(
                                progress = progress,
                                downloadedMb = downloadedMb,
                                totalMb = totalMb
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.renameTo(targetFile)) {
                val authority = "${context.packageName}.fileprovider"
                val apkUri = FileProvider.getUriForFile(context, authority, targetFile)
                val ready = UpdateState.ReadyToInstall(apkUri)
                _updateState.value = ready
                ready
            } else {
                throw IOException("Failed to rename temporary APK file to final target")
            }
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            val error = UpdateState.Error("Download failed: ${e.localizedMessage ?: "Unknown error"}", e)
            _updateState.value = error
            error
        }
    }

    fun launchInstallation(fileUri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }

    private fun fetchLatestRelease(): ReleaseDto? {
        val requestLatest = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            val response = okHttpClient.newCall(requestLatest).execute()
            if (response.isSuccessful && response.body != null) {
                val jsonString = response.body!!.string()
                val json = JSONObject(jsonString)
                val dto = parseReleaseObject(json)
                if (dto != null) return dto
            }
        } catch (_: Exception) {
            // Fall back to querying the releases list endpoint
        }

        val requestAll = Request.Builder()
            .url(ALL_RELEASES_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val allResponse = okHttpClient.newCall(requestAll).execute()
        if (!allResponse.isSuccessful || allResponse.body == null) {
            return null
        }

        val arrayJson = JSONArray(allResponse.body!!.string())
        for (i in 0 until arrayJson.length()) {
            val rel = arrayJson.getJSONObject(i)
            val isDraft = rel.optBoolean("draft", false)
            val isPrerelease = rel.optBoolean("prerelease", false)
            if (!isDraft && !isPrerelease) {
                val parsed = parseReleaseObject(rel)
                if (parsed != null) return parsed
            }
        }

        return null
    }

    private fun parseReleaseObject(json: JSONObject): ReleaseDto? {
        val tagName = json.optString("tag_name", "").removePrefix("v").trim()
        val name = json.optString("name", tagName)
        val body = json.optString("body", "No changelog provided.")
        val assets = json.optJSONArray("assets") ?: return null

        var apkDownloadUrl: String? = null
        var apkSize = 0L

        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val assetName = asset.optString("name", "")
            if (assetName.endsWith(".apk", ignoreCase = true)) {
                apkDownloadUrl = asset.optString("browser_download_url", "")
                apkSize = asset.optLong("size", 0L)
                break
            }
        }

        if (apkDownloadUrl.isNullOrEmpty()) return null

        return ReleaseDto(
            versionName = if (tagName.isNotEmpty()) tagName else name,
            body = body,
            downloadUrl = apkDownloadUrl,
            fileSizeBytes = apkSize
        )
    }

    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val cleanRemote = remoteVersion.trim().removePrefix("v")
        val cleanCurrent = currentVersion.trim().removePrefix("v")

        val remoteParts = cleanRemote.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private data class ReleaseDto(
        val versionName: String,
        val body: String,
        val downloadUrl: String,
        val fileSizeBytes: Long
    )
}
