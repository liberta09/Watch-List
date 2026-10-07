package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kaan.watchlist.data.api.AnnouncementApi
import com.kaan.watchlist.domain.model.Announcement
import com.kaan.watchlist.util.UpdateConfig
import com.kaan.watchlist.R
import java.util.Locale
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AnnouncementRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)

    private val announcementApi: AnnouncementApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
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
        val url = "https://raw.githubusercontent.com/${UpdateConfig.GITHUB_OWNER}/${UpdateConfig.GITHUB_REPO}/master/announcement.json"
        val lastDismissedId = getDismissedAnnouncementId()

        return try {
            val dto = announcementApi.getAnnouncement(url)
            val id = dto.id ?: "announcement_001"
            val enabled = dto.enabled ?: true
            
            val isEnglish = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().let {
                if (it.isEmpty) Locale.getDefault().language == "en" else it.get(0)?.language == "en"
            }
            
            val finalTitle = if (isEnglish && !dto.titleEn.isNullOrBlank()) dto.titleEn else dto.title
            val finalMessage = if (isEnglish && !dto.messageEn.isNullOrBlank()) dto.messageEn else dto.message
            val finalButton = if (isEnglish && !dto.buttonTextEn.isNullOrBlank()) dto.buttonTextEn else dto.buttonText

            if (enabled && id != lastDismissedId) {
                Announcement(
                    id = id,
                    title = finalTitle ?: context.getString(R.string.announcement_fallback_title),
                    message = finalMessage ?: context.getString(R.string.announcement_fallback_message),
                    buttonText = finalButton ?: context.getString(R.string.announcement_fallback_btn)
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
                    title = context.getString(R.string.announcement_fallback_title),
                    message = context.getString(R.string.announcement_fallback_message),
                    buttonText = context.getString(R.string.announcement_fallback_btn)
                )
            } else {
                null
            }
        }
    }
}
