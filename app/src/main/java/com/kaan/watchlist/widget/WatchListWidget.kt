package com.kaan.watchlist.widget

import com.kaan.watchlist.R
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.Action
import android.content.ComponentName
import com.kaan.watchlist.MainActivity

class WatchListWidget : GlanceAppWidget() {
    
    companion object {
        private val SMALL_SQUARE = DpSize(180.dp, 110.dp)
        private val LARGE_SQUARE = DpSize(250.dp, 220.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL_SQUARE, LARGE_SQUARE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetData.loadWidgetData(context)

        provideContent {
            val size = LocalSize.current
            val isSmall = size.height < 180.dp

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(android.graphics.Color.parseColor("#0F172A")))
                    .padding(12.dp)
                    .cornerRadius(16.dp)
            ) {
                // Header
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.parseColor("#3B82F6")),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.fillMaxWidth().clickable(
                        actionStartActivity(
                            Intent(LocalContext.current, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    )
                )
                
                Spacer(modifier = GlanceModifier.height(8.dp))

                val context = LocalContext.current
                // This Week Section
                SectionHeader(context.getString(R.string.widget_this_week))
                if (state.thisWeek.isEmpty()) {
                    EmptyText(context.getString(R.string.widget_empty_upcoming))
                } else {
                    state.thisWeek.take(if (isSmall) 2 else 3).forEach { row ->
                        MediaRow(row)
                    }
                }

                // Continue Watching Section
                if (!isSmall) {
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    SectionHeader(context.getString(R.string.widget_continue_watching))
                    if (state.continueWatching.isEmpty()) {
                        EmptyText(context.getString(R.string.widget_empty_continue))
                    } else {
                        state.continueWatching.take(2).forEach { row ->
                            MediaRow(row)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            style = TextStyle(
                color = ColorProvider(android.graphics.Color.WHITE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)
        )
    }

    @Composable
    private fun MediaRow(row: WidgetRow) {
        val intent = Intent(LocalContext.current, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_media_id", row.id)
        }
        
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable(actionStartActivity(intent))
        ) {
            Text(
                text = row.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.WHITE),
                    fontSize = 13.sp
                )
            )
            Text(
                text = row.subtitle,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(android.graphics.Color.parseColor("#3B82F6")),
                    fontSize = 11.sp
                )
            )
        }
    }

    @Composable
    private fun EmptyText(text: String) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(android.graphics.Color.GRAY),
                fontSize = 11.sp
            ),
            modifier = GlanceModifier.padding(top = 2.dp)
        )
    }
}
