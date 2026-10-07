package com.kaan.watchlist.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val defaultLocale: Locale
        get() {
            val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            return if (appLocales.isEmpty) {
                Locale.getDefault()
            } else {
                appLocales.get(0) ?: Locale.getDefault()
            }
        }

    fun getDaysUntil(dateStr: String?): Int? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Gelen string ISO formatında
            val target = formatter.parse(dateStr) ?: return null
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val diff = target.time - now.time
            val days = (diff / (1000 * 60 * 60 * 24)).toInt()
            days
        } catch (e: Exception) {
            null
        }
    }

    fun formatDisplayDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayFormatter = SimpleDateFormat("d MMMM yyyy", defaultLocale)
            val date = formatter.parse(dateStr) ?: return dateStr
            displayFormatter.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatShortDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val shortFormatter = SimpleDateFormat("d MMM", defaultLocale)
            val date = formatter.parse(dateStr) ?: return dateStr
            shortFormatter.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}