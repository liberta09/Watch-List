package com.kaan.watchlist.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceUtils {
    fun isTv(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
               (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    }
}