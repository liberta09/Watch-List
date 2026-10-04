package com.kaan.watchlist.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.domain.model.MediaType
import com.kaan.watchlist.ui.components.tvFocusable
import com.kaan.watchlist.ui.theme.BlueAccent
import com.kaan.watchlist.ui.theme.DarkNavy
import com.kaan.watchlist.ui.theme.DarkSurface
import com.kaan.watchlist.ui.theme.LightText
import com.kaan.watchlist.viewmodel.MediaViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController, viewModel: MediaViewModel) {
    val myList by viewModel.myList.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val watchedMovies = myList.filter { it.isWatched && it.type == MediaType.MOVIE }
    val watchedTvShows = myList.filter { it.isWatched && it.type == MediaType.TV }
    val totalEpisodesWatched = myList.sumOf { it.watchedEpisodes.size }

    // Total Watch Time Calculation
    var movieMinutes = 0
    var missingMovieTimeCount = 0
    watchedMovies.forEach {
        if (it.runtime != null && it.runtime > 0) {
            movieMinutes += it.runtime
        } else {
            missingMovieTimeCount++
        }
    }

    var tvMinutes = 0
    myList.filter { it.type == MediaType.TV }.forEach { show ->
        val epRuntime = show.episodeRuntime ?: 45 // fallback 45 mins if unknown
        tvMinutes += show.watchedEpisodes.size * epRuntime
    }

    val totalMinutes = movieMinutes + tvMinutes
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val durationText = "${hours} saat ${mins} dk"

    // This Year Stats
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val thisYearWatched = myList.filter { item ->
        val cal = Calendar.getInstance()
        item.watchedAt?.let { cal.timeInMillis = it; cal.get(Calendar.YEAR) == currentYear } ?: false
    }

    // Top Genres
    val allGenres = myList.filter { it.isWatched }.flatMap { it.genres }
    val genreCounts = allGenres.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
    val totalGenreCount = allGenres.size.coerceAtLeast(1)

    // Top Cast
    val allCast = myList.filter { it.isWatched }.flatMap { it.cast }
    val castCounts = allCast.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)

    // Ratings
    val ratedItems = myList.mapNotNull { it.userRating?.let { r -> Pair(it, r) } }
    val avgRating = if (ratedItems.isNotEmpty()) ratedItems.map { it.second }.average() else 0.0
    val topRated = ratedItems.sortedByDescending { it.second }.take(3).map { it.first }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İstatistikler", color = LightText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.tvFocusable(shape = CircleShape, onClick = { navController.popBackStack() })
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = LightText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Özet Kartları
            item {
                Text("İzleme Özeti", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LightText)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox("Filmler", "${watchedMovies.size}", Modifier.weight(1f))
                    StatBox("Diziler", "${watchedTvShows.size}", Modifier.weight(1f))
                    StatBox("Bölümler", "$totalEpisodesWatched", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Toplam İzleme Süresi", fontSize = 13.sp, color = LightText.copy(alpha = 0.7f))
                        Text(durationText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                        if (missingMovieTimeCount > 0) {
                            Text(
                                "$missingMovieTimeCount filmin süre bilgisi eksik",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bu Yıl Kartı
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkNavy)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$currentYear Yılında", fontSize = 14.sp, color = LightText.copy(alpha = 0.8f))
                            Text("${thisYearWatched.size} içerik izlendi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LightText)
                        }
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                    }
                }
            }

            // Aylık Grafik (Canvas)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Aylık İzleme Grafiği (Son 12 Ay)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightText)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Monthly calculation
                        val months = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Ekim", "Kas", "Aral")
                        val monthCounts = FloatArray(12) { 0f }
                        
                        myList.filter { it.isWatched }.forEach { item ->
                            item.watchedAt?.let { ts ->
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = ts
                                val m = cal.get(Calendar.MONTH)
                                if (m in 0..11) monthCounts[m] += 1f
                            }
                        }

                        val maxVal = (monthCounts.maxOrNull() ?: 1f).coerceAtLeast(1f)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val barWidth = (size.width - (12 * 8)) / 12
                            monthCounts.forEachIndexed { index, count ->
                                val x = index * (barWidth + 8) + 4
                                val barHeight = (count / maxVal) * (size.height - 20)
                                val y = size.height - barHeight - 20

                                drawRoundRect(
                                    color = BlueAccent,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            months.forEach { m ->
                                Text(m, fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // En Çok İzlediğin Türler
            if (genreCounts.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("En Çok İzlenen Türler", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightText)
                            Spacer(modifier = Modifier.height(12.dp))

                            genreCounts.forEach { entry ->
                                val ratio = entry.value.toFloat() / totalGenreCount
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(entry.key, fontSize = 13.sp, color = LightText)
                                        Text("${entry.value} içerik", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = BlueAccent,
                                        trackColor = DarkNavy
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // En Çok İzlenen Oyuncular
            if (castCounts.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("En Sık Sahnede Gören Oyuncular", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightText)
                            Spacer(modifier = Modifier.height(8.dp))

                            castCounts.forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.key, fontSize = 13.sp, color = LightText)
                                    Text("${entry.value} yapım", fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Puan Ortalaması ve En Yüksek Puanlılar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Puan Durumu", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ortalama Puanın: ", fontSize = 13.sp, color = LightText.copy(alpha = 0.8f))
                            Text(
                                if (avgRating > 0) String.format(Locale.US, "%.1f / 10", avgRating) else "Henüz puan yok",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueAccent
                            )
                        }

                        if (topRated.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("En Yüksek Puan Verdiğin Yapımlar:", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(topRated) { media ->
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkNavy)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = media.posterUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(2f / 3f)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Text(
                                                "★ ${media.userRating}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD700),
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Liste Genel Durumu
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Koleksiyon Durumu", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LightText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Toplam Kayıt: ${myList.size}", fontSize = 13.sp, color = LightText)
                        Text("• İzlenenler: ${myList.count { it.isWatched }}", fontSize = 13.sp, color = LightText)
                        Text("• İzlenecekler: ${myList.count { !it.isWatched }}", fontSize = 13.sp, color = LightText)
                        Text("• Favoriler: ${favorites.size}", fontSize = 13.sp, color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LightText)
            Text(title, fontSize = 11.sp, color = LightText.copy(alpha = 0.7f))
        }
    }
}
