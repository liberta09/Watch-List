package com.kaan.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import com.kaan.watchlist.domain.model.MediaType
import java.util.Locale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun MediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    subtitle: String? = null,
    showYear: Boolean = true,
    topBadge: String? = null,
    onToggleWatched: ((MediaItem) -> Unit)? = null,
    onToggleList: ((MediaItem) -> Unit)? = null
) {
    var showPreview by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(120.dp))
            .tvFocusable(
                shape = RoundedCornerShape(8.dp),
                onClick = onClick,
                onLongClick = { showPreview = true }
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
            ) {
                AsyncImage(
                    model = media.posterUrl,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // TMDB Rating Badge (Top Right)
                if (media.voteAverage != null && media.voteAverage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.height(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", media.voteAverage),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Top Left Badges
                Column(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    if (media.userRating != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BlueAccent)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Sen: ${media.userRating}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (topBadge != null) {
                        if (media.userRating != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE50914))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = topBadge,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                
                // YENI Badge (Bottom Left)
                if (media.isInList && media.type == MediaType.TV && media.lastAiredSeason != null && media.lastAiredEpisode != null) {
                    val lastAiredKey = "S${media.lastAiredSeason}_E${media.lastAiredEpisode}"
                    if (!media.watchedEpisodes.containsKey(lastAiredKey) && media.watchedEpisodes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE50914))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YENİ",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = media.title,
                color = LightText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showYear) {
                Text(
                    text = media.year,
                    color = LightText.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = BlueAccent,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Latest watched episode for TV shows
            if (media.type == MediaType.TV && (media.lastWatchedSeason != null && media.lastWatchedEpisode != null)) {
                Text(
                    text = "S${media.lastWatchedSeason} B${media.lastWatchedEpisode}",
                    color = BlueAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            
            if (onToggleWatched != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (media.isWatched) DarkNavy else BlueAccent)
                        .tvFocusable(shape = RoundedCornerShape(4.dp), scaleOnFocus = 1.02f, onClick = { onToggleWatched(media) })
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (media.isWatched) "✓ İZLENDİ" else "🔖 İZLENECEK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else if (media.isInList) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (media.isWatched) DarkNavy else BlueAccent)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (media.isWatched) "✓ İZLENDİ" else "🔖 İZLENECEK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = media.title,
                            color = LightText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = media.year, color = LightText.copy(alpha = 0.7f), fontSize = 13.sp)
                            if (media.voteAverage != null && media.voteAverage > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.height(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", media.voteAverage),
                                    color = LightText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (media.genres.isNotEmpty()) {
                            Text(
                                text = media.genres.take(2).joinToString(", "),
                                color = LightText.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            text = {
                Text(
                    text = if (media.overview.isNotBlank()) media.overview else "Açıklama bulunmuyor.",
                    color = LightText.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showPreview = false
                            onClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = {
                                showPreview = false
                                onClick()
                            })
                    ) {
                        Text("Detaya Git", fontSize = 13.sp)
                    }

                    if (onToggleList != null) {
                        Button(
                            onClick = {
                                onToggleList(media)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (media.isInList) DarkNavy else BlueAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .tvFocusable(shape = RoundedCornerShape(8.dp), onClick = { onToggleList(media) })
                        ) {
                            Text(if (media.isInList) "✓ Listede" else "+ Listeye Ekle", fontSize = 13.sp)
                        }
                    }
                }
            }
        )
    }
}
