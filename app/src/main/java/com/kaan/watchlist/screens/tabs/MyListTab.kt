package com.kaan.watchlist.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.ui.components.MediaCard
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

@Composable
fun MyListTab(viewModel: MediaViewModel, onMediaClick: (MediaItem) -> Unit) {
    val myList by viewModel.myList.collectAsState()
    
    val toWatch = myList.filter { !it.isWatched }
    val watched = myList.filter { it.isWatched }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                text = "Listem",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LightText
            )
            Text(
                text = "Toplam: ${myList.size}  |  İzlenen: ${watched.size}  |  İzlenecek: ${toWatch.size}",
                fontSize = 13.sp,
                color = LightText.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (myList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Listeniz şu an boş.", color = LightText.copy(alpha = 0.7f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (toWatch.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "İzlenecekler",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                    }
                    items(toWatch) { media ->
                        MediaCard(
                            media = media, 
                            onClick = { onMediaClick(media) },
                            onToggleWatched = { viewModel.toggleWatchedStatus(it) }
                        )
                    }
                }

                if (watched.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "İzlenenler",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightText,
                            modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                        )
                    }
                    items(watched) { media ->
                        MediaCard(
                            media = media, 
                            onClick = { onMediaClick(media) },
                            onToggleWatched = { viewModel.toggleWatchedStatus(it) }
                        )
                    }
                }
            }
        }
    }
}
