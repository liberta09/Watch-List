package com.kaan.watchlist.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kaan.watchlist.R

object NotificationHelper {
    private const val CHANNEL_ID = "watchlist_notifications_v2"
    private const val CHANNEL_NAME = "Watch List Bildirimleri"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Watch List genel bildirim kanalı"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTestNotification(context: Context): Boolean {
        // 1. Check if notifications are enabled for the app in System Settings
        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) {
            Toast.makeText(context, "Sistem ayarlarından uygulamanın bildirimleri kapatılmış.", Toast.LENGTH_LONG).show()
            return false
        }

        // 2. Check Android 13+ (API 33) runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Lütfen cihaz bildirim iznini onaylayın.", Toast.LENGTH_LONG).show()
                return false
            }
        }

        // 3. Ensure Notification Channel exists
        createNotificationChannel(context)

        // 4. Build Notification using the app's own logo resource
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_watchlist_logo)
            .setContentTitle("Watch List")
            .setContentText("Bildirimler başarıyla çalışıyor.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        return try {
            managerCompat.notify(NOTIFICATION_ID, builder.build())
            Toast.makeText(context, "Test bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: SecurityException) {
            Toast.makeText(context, "Bildirim izni bulunamadı.", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Bildirim gönderilemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
