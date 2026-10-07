package com.kaan.watchlist.screens.tabs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kaan.watchlist.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaan.watchlist.data.repository.UpdateStatus
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.ui.components.MediaCard
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel

@Composable
fun DiscoverTab(viewModel: MediaViewModel, onMediaClick: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val popMovies by viewModel.popularMovies.collectAsState()
    val popTvShows by viewModel.popularTvShows.collectAsState()
    val trending by viewModel.trending.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val upcoming by viewModel.upcoming.collectAsState()
    val upcomingForUser by viewModel.upcomingForUser.collectAsState()
    val personalRecommendations by viewModel.personalRecommendations.collectAsState()
    val recommendationReasons by viewModel.recommendationReasons.collectAsState()
    val myList by viewModel.myList.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    
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

    val onTelegramClick = {
        val telegramUrl = "https://t.me/+o-RFlV4U3UY5NGU8"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Telegram'ı veya tarayıcıyı açacak bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Telegram'ı veya tarayıcıyı açacak bir uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            val activity = context as? ComponentActivity
            val updateStatus by viewModel.updateStatus.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
                    modifier = Modifier.tvFocusable(
                        shape = CircleShape,
                        onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = LightText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Watch List",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueAccent
                )

                Spacer(modifier = Modifier.weight(1f))

                // Refresh Update Status
                if (updateStatus is UpdateStatus.Checking) {
                    CircularProgressIndicator(
                        color = BlueAccent,
                        modifier = Modifier.padding(end = 8.dp).size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            viewModel.checkForUpdates()
                            viewModel.loadHomeData()
                            if (updateStatus is UpdateStatus.UpToDate) {
                                Toast.makeText(context, "Uygulamanız güncel.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.tvFocusable(
                            shape = CircleShape,
                            onClick = {
                                viewModel.checkForUpdates()
                                viewModel.loadHomeData()
                                if (updateStatus is UpdateStatus.UpToDate) {
                                    Toast.makeText(context, "Uygulamanız güncel.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Güncellemeleri Kontrol Et",
                            tint = LightText
                        )
                    }
                }
            }
        }
        
        if (upcomingForUser.isNotEmpty()) {
            item {
                SectionTitle("📅 " + stringResource(R.string.discover_upcoming))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(upcomingForUser) { media ->
                        val userMedia = viewModel.withUserState(media, myList, favorites)
                        
                        val days = com.kaan.watchlist.util.DateUtils.getDaysUntil(
                            if (userMedia.type == com.kaan.watchlist.domain.model.MediaType.TV) userMedia.nextEpisodeAirDate else userMedia.releaseDate
                        )
                        val daysStr = when (days) {
                            0 -> "Bugün"
                            1 -> "Yarın"
                            2, 3, 4, 5, 6 -> "$days gün sonra"
                            else -> com.kaan.watchlist.util.DateUtils.formatShortDate(
                                if (userMedia.type == com.kaan.watchlist.domain.model.MediaType.TV) userMedia.nextEpisodeAirDate else userMedia.releaseDate
                            )
                        }
                        
                        val topBadge = if (userMedia.type == com.kaan.watchlist.domain.model.MediaType.TV && userMedia.nextEpisodeNumber == 1) "YENİ SEZON" else null
                        
                        val subtitle = if (userMedia.type == com.kaan.watchlist.domain.model.MediaType.TV) {
                            val season = userMedia.nextEpisodeSeason ?: 1
                            val ep = userMedia.nextEpisodeNumber ?: 1
                            "S$season B$ep · $daysStr"
                        } else {
                            "Vizyon · $daysStr"
                        }
                        
                        MediaCard(
                            media = userMedia, 
                            onClick = { onMediaClick(userMedia) },
                            subtitle = subtitle,
                            showYear = false,
                            topBadge = topBadge,
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        if (personalRecommendations.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.discover_for_you))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(personalRecommendations) { media ->
                        val userMedia = viewModel.withUserState(media, myList, favorites)
                        val reason = recommendationReasons[media.id]
                        MediaCard(
                            media = userMedia, 
                            onClick = { onMediaClick(userMedia) },
                            subtitle = if (reason != null) "$reason izlediğin için" else null,
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        if (myList.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.home_tab_my_list))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myList) { media ->
                        val userMedia = viewModel.withUserState(media, myList, favorites)
                        MediaCard(
                            media = userMedia, 
                            onClick = { onMediaClick(userMedia) },
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (trending.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.discover_trending), onTelegramClick = onTelegramClick)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trending) { media ->
                        val userMedia = viewModel.withUserState(media, myList, favorites)
                        MediaCard(
                            media = userMedia, 
                            onClick = { onMediaClick(userMedia) },
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (nowPlaying.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.discover_now_playing))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(nowPlaying) { media ->
                        val userMedia = viewModel.withUserState(media, myList, favorites)
                        MediaCard(
                            media = userMedia, 
                            onClick = { onMediaClick(userMedia) },
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.discover_upcoming))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(upcoming) { media ->
                        val userMedia = viewModel.withUserState(media, myList, favorites)
                        MediaCard(
                            media = userMedia, 
                            onClick = { onMediaClick(userMedia) },
                            onToggleList = { 
                                if (!it.isInList) viewModel.addToList(it, watched = false) 
                                else viewModel.toggleList(it) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            SectionTitle(stringResource(R.string.discover_popular_movies))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(popMovies) { media ->
                    val userMedia = viewModel.withUserState(media, myList, favorites)
                    MediaCard(
                        media = userMedia, 
                        onClick = { onMediaClick(userMedia) },
                        onToggleList = { 
                            if (!it.isInList) viewModel.addToList(it, watched = false) 
                            else viewModel.toggleList(it) 
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionTitle(stringResource(R.string.discover_popular_tv))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(popTvShows) { media ->
                    val userMedia = viewModel.withUserState(media, myList, favorites)
                    MediaCard(
                        media = userMedia, 
                        onClick = { onMediaClick(userMedia) },
                        onToggleList = { 
                            if (!it.isInList) viewModel.addToList(it, watched = false) 
                            else viewModel.toggleList(it) 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    onTelegramClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightText
        )

        if (onTelegramClick != null) {
            Row(
                modifier = Modifier
                    .tvFocusable(
                        shape = RoundedCornerShape(8.dp),
                        onClick = onTelegramClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Telegram",
                    tint = BlueAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "t.me/WatchList",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BlueAccent
                )
            }
        }
    }
}
