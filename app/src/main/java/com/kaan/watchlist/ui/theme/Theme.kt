package com.kaan.watchlist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AppColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    background = DarkBackground,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    secondary = DarkNavy,
    onSecondary = LightText
)

@Composable
fun WatchListTheme(
    darkTheme: Boolean = true, // Force dark theme as requested
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the custom dark/blue theme
    content: @Composable () -> Unit
) {
    val colorScheme = AppColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
