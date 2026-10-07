package com.kaan.watchlist.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.kaan.watchlist.R
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object UpdateHelper {

    sealed class DownloadState {
        object Idle : DownloadState()
        object Downloading : DownloadState()
        data class Completed(val apkFile: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var pendingInstallFile: File? = null
    var isAppInForeground: Boolean = false

    fun resetDownloadState() {
        Log.d("UpdateDebug", "State değişti: Idle")
        _downloadState.value = DownloadState.Idle
    }

    fun downloadAndInstallApk(context: Context, downloadUrl: String, fileName: String = "WatchList_Update.apk") {
        if (downloadUrl.isBlank()) {
            Log.d("UpdateDebug", "State değişti: Error (Geçersiz URL)")
            _downloadState.value = DownloadState.Error(context.getString(R.string.update_err_invalid_url))
            return
        }

        try {
            val destinationFile = File(context.getExternalFilesDir(null), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                Log.d("UpdateDebug", "State değişti: Error (İndirme yöneticisi başlatılamadı)")
                _downloadState.value = DownloadState.Error(context.getString(R.string.update_err_dm_failed))
                return
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(context.getString(R.string.update_title))
                setDescription(context.getString(R.string.update_desc))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, null, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = downloadManager.enqueue(request)
            Log.d("UpdateDebug", "İndirme başladı, downloadId: $downloadId")
            
            Log.d("UpdateDebug", "State değişti: Downloading")
            _downloadState.value = DownloadState.Downloading

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                    Log.d("UpdateDebug", "Broadcast alındı, intent downloadId: $id, beklenen: $downloadId")
                    
                    if (id == downloadId) {
                        try {
                            recvContext?.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e("UpdateDebug", "Hata: unregisterReceiver başarısız", e)
                        }

                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                                val apkFile = File(recvContext?.getExternalFilesDir(null), fileName)
                                if (isAppInForeground) {
                                    Log.d("UpdateDebug", "State değişti: Completed")
                                    _downloadState.value = DownloadState.Completed(apkFile)
                                } else {
                                    Log.d("UpdateDebug", "State değişti: Idle (Arka plan kurulum beklemesi)")
                                    pendingInstallFile = apkFile
                                    _downloadState.value = DownloadState.Idle
                                }
                            } else {
                                if (isAppInForeground) {
                                    Log.d("UpdateDebug", "State değişti: Error (İndirme başarısız oldu)")
                                    _downloadState.value = DownloadState.Error(recvContext?.getString(R.string.update_err_download_failed) ?: "Download failed.")
                                }
                            }
                            cursor.close()
                        }
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(
                context.applicationContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )

        } catch (e: Exception) {
            Log.e("UpdateDebug", "Hata: İndirme başlatılamadı", e)
            Log.d("UpdateDebug", "State değişti: Error")
            _downloadState.value = DownloadState.Error(context.getString(R.string.update_err_start_failed, e.localizedMessage ?: ""))
        }
    }

    fun promptInstall(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, context.getString(R.string.toast_install_file_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        // Android 8.0+ (API 26+) Unknown Sources Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            pendingInstallFile = apkFile
            Toast.makeText(
                context,
                context.getString(R.string.update_toast_permission),
                Toast.LENGTH_LONG
            ).show()

            try {
                val permissionIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permissionIntent)
            } catch (e: Exception) {
                Log.e("UpdateDebug", "Hata: ACTION_MANAGE_UNKNOWN_APP_SOURCES açılamadı", e)
                try {
                    val permissionIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                } catch (ex: Exception) {
                    Log.e("UpdateDebug", "Hata: ACTION_SECURITY_SETTINGS açılamadı", ex)
                }
            }
            return
        }

        // Permission is granted (or API < 26): Launch PackageInstaller
        try {
            pendingInstallFile = null
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("UpdateDebug", "Hata: Kurulum ekranı açılamadı", e)
            Toast.makeText(context, context.getString(R.string.toast_open_install_failed, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndPromptPendingInstall(context: Context) {
        val file = pendingInstallFile
        if (file != null && file.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                promptInstall(context, file)
            }
        }
    }
}
