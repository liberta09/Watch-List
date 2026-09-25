package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kaan.watchlist.data.api.AnnouncementApi
import com.kaan.watchlist.data.api.enableTlsChainFallback
import com.kaan.watchlist.domain.model.Announcement
import com.kaan.watchlist.util.UpdateConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AnnouncementRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)

    private val announcementApi: AnnouncementApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .enableTlsChainFallback()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnnouncementApi::class.java)
    }

    private fun getDismissedAnnouncementId(): String? {
        return prefs.getString("last_dismissed_announcement_id", null)
    }

    fun dismissAnnouncement(id: String) {
        prefs.edit().putString("last_dismissed_announcement_id", id).apply()
    }

    suspend fun fetchAnnouncement(): Announcement? {
        val url = "https://raw.githubusercontent.com/${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/main/announcement.json"
        val lastDismissedId = getDismissedAnnouncementId()

        return try {
            val dto = announcementApi.getAnnouncement(url)
            val id = dto.id ?: "announcement_001"
            val enabled = dto.enabled ?: true

            if (enabled && id != lastDismissedId) {
                Announcement(
                    id = id,
                    title = dto.title ?: "📢 Watch List Güncellendi",
                    message = dto.message ?: "Uygulamamız güncellendi! 🎉\n\nHepiniz hoş geldiniz.\nWatch List'i daha iyi hale getirmek için çalışmalarımıza devam ediyoruz.\nYeni sürümümüzü keyifle kullanmanız dileğiyle. ❤️",
                    buttonText = dto.buttonText ?: "Tamam"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Fallback: If network fails on first launch and announcement_001 was never dismissed
            val fallbackId = "announcement_001"
            if (fallbackId != lastDismissedId) {
                Announcement(
                    id = fallbackId,
                    title = "📢 Watch List Güncellendi",
                    message = "Uygulamamız güncellendi! 🎉\n\nHepiniz hoş geldiniz.\nWatch List'i daha iyi hale getirmek için çalışmalarımıza devam ediyoruz.\nYeni sürümümüzü keyifle kullanmanız dileğiyle. ❤️",
                    buttonText = "Tamam"
                )
            } else {
                null
            }
        }
    }
}
