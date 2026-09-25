package com.kaan.watchlist.data.repository

import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.data.api.UpdateApi
import com.kaan.watchlist.data.api.enableTlsChainFallback
import com.kaan.watchlist.util.UpdateConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class UpdateAvailable(val version: String, val downloadUrl: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class UpdateRepository {

    private val updateApi: UpdateApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .enableTlsChainFallback()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApi::class.java)
    }

    suspend fun checkForUpdates(): UpdateStatus {
        if (!UpdateConfig.isConfigured) {
            return UpdateStatus.Error("Güncelleme sunucusu henüz yapılandırılmamış (GitHub deposu bekleniyor).")
        }

        return try {
            val release = updateApi.getLatestRelease(UpdateConfig.GITHUB_OWNER, UpdateConfig.GITHUB_REPO)
            val rawTag = release.tagName ?: release.name ?: ""
            val latestVersion = rawTag.removePrefix("v").removePrefix("V").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

            if (isNewerVersion(currentVersion, latestVersion)) {
                val apkAsset = release.assets?.find { it.name?.endsWith(".apk", ignoreCase = true) == true }
                val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl ?: ""
                if (downloadUrl.isBlank()) {
                    UpdateStatus.Error("Yeni $rawTag sürümü bulundu fakat APK indirme adresi bulunamadı.")
                } else {
                    UpdateStatus.UpdateAvailable(rawTag, downloadUrl)
                }
            } else {
                UpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            UpdateStatus.Error("Güncelleme kontrolü yapılamadı. İnternet bağlantınızı kontrol edin.")
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        if (latest.isBlank()) return false
        return try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(currentParts.size, latestParts.size)

            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            current != latest
        }
    }
}
