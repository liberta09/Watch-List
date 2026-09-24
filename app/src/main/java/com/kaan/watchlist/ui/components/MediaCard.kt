package com.kaan.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@Composable
fun MediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    onToggleWatched: ((MediaItem) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .tvFocusable(shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
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
            Text(
                text = media.year,
                color = LightText.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1
            )
            
            if (onToggleWatched != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (media.isWatched) DarkNavy else BlueAccent)
                        .tvFocusable(shape = RoundedCornerShape(4.dp), scaleOnFocus = 1.02f)
                        .clickable { onToggleWatched(media) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (media.isWatched) "İzlendi" else "İzlenecek",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
