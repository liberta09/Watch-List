package com.kaan.watchlist.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.ui.components.MediaCard
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

@Composable
fun DiscoverTab(viewModel: MediaViewModel, onMediaClick: (MediaItem) -> Unit) {
    val popMovies by viewModel.popularMovies.collectAsState()
    val popTvShows by viewModel.popularTvShows.collectAsState()
    val myList by viewModel.myList.collectAsState()
    
    val isLoading by viewModel.isDiscoverLoading.collectAsState()
    val error by viewModel.discoverError.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BlueAccent)
        }
        return
    }

    if (error != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = error ?: "", color = LightText, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.loadHomeData() },
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
            ) {
                Text("Tekrar Dene")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Watch List",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueAccent
                )
            }
        }
        
        if (myList.isNotEmpty()) {
            item {
                SectionTitle("Listem")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myList) { media ->
                        MediaCard(media = media, onClick = { onMediaClick(media) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            SectionTitle("Popüler Filmler")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(popMovies) { media ->
                    MediaCard(media = media, onClick = { onMediaClick(media) })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle("Popüler Diziler")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(popTvShows) { media ->
                    MediaCard(media = media, onClick = { onMediaClick(media) })
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = LightText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
