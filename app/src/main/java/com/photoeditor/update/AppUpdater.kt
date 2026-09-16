package com.photoeditor.update

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.photoeditor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AppUpdater(private val activity: ComponentActivity) {

    data class UpdateInfo(
        val buildNumber: Int,
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    private val prefs = activity.getSharedPreferences("app_updater", Context.MODE_PRIVATE)
    private var downloadId: Long = -1L
    private var downloadReceiver: BroadcastReceiver? = null

    fun checkForUpdates() {
        activity.lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { fetchLatestRelease() } ?: return@launch
            if (info.buildNumber <= BuildConfig.VERSION_CODE || activity.isFinishing) return@launch

            AlertDialog.Builder(activity)
                .setTitle("Update available")
                .setMessage(
                    "A newer Azeem's Editor build is available.\n\n" +
                        "Installed build: ${BuildConfig.VERSION_CODE}\n" +
                        "Latest build: ${info.buildNumber}" +
                        if (info.releaseNotes.isNotBlank()) "\n\n${info.releaseNotes}" else ""
                )
                .setNegativeButton("Later", null)
                .setPositiveButton("Update") { _, _ -> downloadAndInstall(info) }
                .show()
        }
    }

    fun installPendingIfAllowed() {
        val path = prefs.getString(KEY_PENDING_APK, null) ?: return
        val file = File(path)
        if (!file.exists()) {
            prefs.edit().remove(KEY_PENDING_APK).apply()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(settingsIntent)
            Toast.makeText(
                activity,
                "Allow installs from this app, then return to continue the update.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val apkUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(installIntent)
    }

    fun dispose() {
        downloadReceiver?.let {
            runCatching { activity.unregisterReceiver(it) }
        }
        downloadReceiver = null
    }

    private suspend fun fetchLatestRelease(): UpdateInfo? {
        return runCatching {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Azeems-Editor/${BuildConfig.VERSION_NAME}")
            }

            try {
                if (connection.responseCode !in 200..299) return null
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONObject(json)
                val body = release.optString("body")
                val buildNumber = BUILD_NUMBER_REGEX.find(body)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?: return null

                val assets = release.optJSONArray("assets") ?: return null
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.equals(RELEASE_APK_NAME, ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        if (name.equals(RELEASE_APK_NAME, ignoreCase = true)) break
                    }
                }

                UpdateInfo(
                    buildNumber = buildNumber,
                    versionName = release.optString("name", "Build $buildNumber"),
                    downloadUrl = downloadUrl ?: return null,
                    releaseNotes = body
                        .lineSequence()
                        .filterNot { it.startsWith("Build number:", ignoreCase = true) }
                        .filterNot { it.startsWith("Automatically rebuilt", ignoreCase = true) }
                        .joinToString("\n")
                        .trim()
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun downloadAndInstall(info: UpdateInfo) {
        val downloadsDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: activity.filesDir
        val apkFile = File(downloadsDir, "Azeems-Editor-update-${info.buildNumber}.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("Azeem's Editor update")
            .setDescription("Downloading build ${info.buildNumber}")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(apkFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)
        prefs.edit().putString(KEY_PENDING_APK, apkFile.absolutePath).apply()

        downloadReceiver?.let { runCatching { activity.unregisterReceiver(it) } }
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return

                val query = DownloadManager.Query().setFilterById(downloadId)
                val succeeded = manager.query(query)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use false
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL
                } ?: false

                if (succeeded && apkFile.exists()) {
                    installPendingIfAllowed()
                } else {
                    prefs.edit().remove(KEY_PENDING_APK).apply()
                    Toast.makeText(activity, "Update download failed.", Toast.LENGTH_LONG).show()
                }
            }
        }

        ContextCompat.registerReceiver(
            activity,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        Toast.makeText(activity, "Downloading update…", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/Mhd-Azeem/Photoeditor/releases/latest"
        private const val RELEASE_APK_NAME = "Azeems-Editor.apk"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val KEY_PENDING_APK = "pending_apk"
        private val BUILD_NUMBER_REGEX = Regex("Build number:\\s*(\\d+)", RegexOption.IGNORE_CASE)
    }
}
