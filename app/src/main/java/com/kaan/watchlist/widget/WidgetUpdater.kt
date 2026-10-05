package com.kaan.watchlist.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    fun requestUpdate(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_update",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            WatchListWidget().updateAll(context)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
