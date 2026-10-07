package com.kaan.watchlist.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.MainActivity
import com.kaan.watchlist.R
import java.text.SimpleDateFormat
import com.kaan.watchlist.data.api.TmdbApi
import com.kaan.watchlist.data.api.MediaDetailsDto
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.domain.model.MediaType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar
import java.util.Locale

class UpcomingWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    override suspend fun doWork(): Result = coroutineScope {
        val prefs = context.getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        
        if (!notificationsEnabled) return@coroutineScope Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return@coroutineScope Result.success()
        }

        val gson = Gson()
        val listJson = prefs.getString("my_list", "[]")
        val favJson = prefs.getString("favorites", "[]")
        val type = object : TypeToken<List<MediaItem>>() {}.type
        
        val myList: List<MediaItem> = try { gson.fromJson(listJson, type) } catch (e: Exception) { emptyList() }
        val favList: List<MediaItem> = try { gson.fromJson(favJson, type) } catch (e: Exception) { emptyList() }
        
        val allItems = (myList + favList).distinctBy { it.id }.take(40)
        if (allItems.isEmpty()) return@coroutineScope Result.success()

        val apiKey = BuildConfig.TMDB_API_KEY
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") {
            return@coroutineScope Result.success()
        }

        val defaultLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().let {
            if (it.isEmpty) Locale.getDefault() else it.get(0) ?: Locale.getDefault()
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormatter = SimpleDateFormat("d MMMM yyyy", defaultLocale)

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayMs = cal.timeInMillis
        val yesterdayMs = todayMs - 24L * 60 * 60 * 1000L
        val sevenDaysMs = todayMs + 7L * 24 * 60 * 60 * 1000L
        val sixtyDaysAgoMs = todayMs - 60L * 24 * 60 * 60 * 1000L

        // Bildirim geçmişini temizle ve yükle
        val notifiedKeys = prefs.getStringSet("notified_keys", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val keysToRemove = mutableSetOf<String>()
        for (key in notifiedKeys) {
            try {
                val parts = key.split("_")
                if (parts.size >= 2) {
                    val datePart = parts[1]
                    val date = formatter.parse(datePart)
                    if (date != null && date.time < sixtyDaysAgoMs) {
                        keysToRemove.add(key)
                    }
                }
            } catch (e: Exception) {}
        }
        notifiedKeys.removeAll(keysToRemove)

        val fetchedResults = mutableListOf<Pair<MediaItem, MediaDetailsDto>>()

        for (chunk in allItems.chunked(5)) {
            val jobs = chunk.map { item ->
                async {
                    try {
                        val dto = if (item.type == MediaType.MOVIE) {
                            tmdbApi.getMovieDetails(item.id, apiKey)
                        } else {
                            tmdbApi.getTvDetails(item.id, apiKey)
                        }
                        Pair(item, dto)
                    } catch (e: Exception) {
                        Log.e("UpcomingWorker", "Hata: ${e.message}")
                        null
                    }
                }
            }
            jobs.awaitAll().filterNotNull().forEach { fetchedResults.add(it) }
        }

        for ((item, dto) in fetchedResults) {
            var notifyTitle: String? = null
            var notifyBody: String? = null
            var notifyKey: String? = null

            if (item.type == MediaType.TV && dto.nextEpisodeToAir != null) {
                val airDateStr = dto.nextEpisodeToAir.airDate
                val s = dto.nextEpisodeToAir.seasonNumber ?: 1
                val e = dto.nextEpisodeToAir.episodeNumber ?: 1
                val name = dto.title ?: dto.name ?: item.title
                
                val isDaily = dto.genres?.any { it.name == "Talk" || it.name == "News" } == true ||
                              item.genres.any { it == "Talk Show" || it == "Haber" }

                if (!airDateStr.isNullOrBlank()) {
                    val airDate = formatter.parse(airDateStr)
                    if (airDate != null) {
                        if (airDate.time == todayMs || airDate.time == yesterdayMs) {
                            notifyKey = "${item.id}_${airDateStr}_tv_today"
                            
                            if (e == 1) {
                                notifyTitle = if (airDate.time == todayMs) context.getString(R.string.notify_new_season_today) else context.getString(R.string.notify_new_season_started)
                                notifyBody = context.getString(R.string.notify_new_season_body, name, s)
                            } else if (!isDaily) {
                                notifyTitle = if (airDate.time == todayMs) context.getString(R.string.notify_new_episode_today) else context.getString(R.string.notify_new_episode_started)
                                notifyBody = context.getString(R.string.notify_new_episode_body, name, s, e)
                            }
                        } else if (airDate.time == sevenDaysMs && e == 1) {
                            notifyKey = "${item.id}_${airDateStr}_tv_7days"
                            val d = displayFormatter.format(airDate)
                            notifyTitle = context.getString(R.string.notify_new_season_7days)
                            notifyBody = context.getString(R.string.notify_new_season_7days_body, name, s, d)
                        }
                    }
                }
            } else if (item.type == MediaType.MOVIE) {
                val releaseDateStr = dto.releaseDate
                val name = dto.title ?: dto.originalTitle ?: item.title
                
                if (!releaseDateStr.isNullOrBlank()) {
                    val relDate = formatter.parse(releaseDateStr)
                    if (relDate != null && relDate.time == todayMs) {
                        notifyKey = "${item.id}_${releaseDateStr}_movie_today"
                        notifyTitle = context.getString(R.string.notify_movie_today)
                        notifyBody = name
                    }
                }
            }

            if (notifyKey != null && notifyTitle != null && notifyBody != null && !notifiedKeys.contains(notifyKey)) {
                sendNotification(item.id, notifyTitle!!, notifyBody!!)
                notifiedKeys.add(notifyKey)
            }
        }

        prefs.edit().putStringSet("notified_keys", notifiedKeys).apply()
        
        com.kaan.watchlist.widget.WidgetUpdater.requestUpdate(applicationContext)
        
        Result.success()
    }

    private fun sendNotification(mediaId: Int, title: String, body: String) {
        val channelId = "upcoming_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Yaklaşan İçerikler", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_media_id", mediaId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            mediaId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_watchlist_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(mediaId, notification)
    }
}