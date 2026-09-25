package com.kaan.watchlist.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object UpdateHelper {
    fun downloadAndInstallApk(context: Context, downloadUrl: String, fileName: String = "WatchList.apk") {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Watch List Güncellemesi")
                setDescription("Yeni sürüm indiriliyor...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                manager.enqueue(request)
                Toast.makeText(context, "Güncelleme indiriliyor... İndirme tamamlandığında bildirimden kurabilirsiniz.", Toast.LENGTH_LONG).show()
            } else {
                openInBrowser(context, downloadUrl)
            }
        } catch (e: Exception) {
            openInBrowser(context, downloadUrl)
        }
    }

    private fun openInBrowser(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                setDataAndType(Uri.parse(downloadUrl), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "İndirme adresi açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
