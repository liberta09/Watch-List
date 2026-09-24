package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.data.api.MediaDto
import com.kaan.watchlist.data.api.TmdbApi
import com.kaan.watchlist.domain.model.MediaItem
import com.kaan.watchlist.domain.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MediaRepository(private val context: Context) {

    private val tmdbApi: TmdbApi by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    private val apiKey = BuildConfig.TMDB_API_KEY
    private val prefs: SharedPreferences = context.getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _myList = MutableStateFlow<List<MediaItem>>(emptyList())
    val myList: StateFlow<List<MediaItem>> = _myList.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _notes = MutableStateFlow<Map<Int, String>>(emptyMap())
    val notes: StateFlow<Map<Int, String>> = _notes.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    init {
        loadLocalData()
    }

    private fun loadLocalData() {
        val listJson = prefs.getString("my_list", "[]")
        val favJson = prefs.getString("favorites", "[]")
        val notesJson = prefs.getString("notes", "{}")
        
        val type = object : TypeToken<List<MediaItem>>() {}.type
        val notesType = object : TypeToken<Map<Int, String>>() {}.type
        
        val list: List<MediaItem> = gson.fromJson(listJson, type) ?: emptyList()
        val favs: List<MediaItem> = gson.fromJson(favJson, type) ?: emptyList()
        val loadedNotes: Map<Int, String> = gson.fromJson(notesJson, notesType) ?: emptyMap()
        
        _myList.value = list
        _favorites.value = favs
        _notes.value = loadedNotes
        _notificationsEnabled.value = prefs.getBoolean("notifications_enabled", true)
    }

    private fun saveLocalData() {
        prefs.edit().apply {
            putString("my_list", gson.toJson(_myList.value))
            putString("favorites", gson.toJson(_favorites.value))
            putString("notes", gson.toJson(_notes.value))
            apply()
        }
    }

    fun saveNote(mediaId: Int, note: String) {
        val currentNotes = _notes.value.toMutableMap()
        currentNotes[mediaId] = note
        _notes.value = currentNotes
        saveLocalData()
    }

    fun removeNote(mediaId: Int) {
        val currentNotes = _notes.value.toMutableMap()
        currentNotes.remove(mediaId)
        _notes.value = currentNotes
        saveLocalData()
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    suspend fun getPopularMovies(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") throw Exception("API Key bulunamadı veya geçersiz. Lütfen local.properties dosyasını güncelleyin.")
        val response = tmdbApi.getPopularMovies(apiKey)
        return response.results.map { dto -> 
            mapDtoToMediaItem(dto, MediaType.MOVIE) 
        }
    }

    suspend fun getPopularTvShows(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") throw Exception("API Key bulunamadı veya geçersiz. Lütfen local.properties dosyasını güncelleyin.")
        val response = tmdbApi.getPopularTvShows(apiKey)
        return response.results.map { dto -> 
            mapDtoToMediaItem(dto, MediaType.TV) 
        }
    }

    suspend fun search(query: String): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") throw Exception("API Key bulunamadı veya geçersiz. Lütfen local.properties dosyasını güncelleyin.")
        if (query.isBlank()) return emptyList()
        val response = tmdbApi.searchMulti(apiKey, query)
        return response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { dto -> 
            val type = if (dto.mediaType == "movie") MediaType.MOVIE else MediaType.TV
            mapDtoToMediaItem(dto, type)
        }
    }

    private fun mapDtoToMediaItem(dto: MediaDto, fallbackType: MediaType): MediaItem {
        val existingFav = _favorites.value.find { it.id == dto.id }
        val existingList = _myList.value.find { it.id == dto.id }
        
        val isFav = existingFav != null
        val inList = existingList != null
        val isWatched = existingList?.isWatched ?: existingFav?.isWatched ?: false
        
        val rawDate = dto.releaseDate ?: dto.firstAirDate ?: ""
        val year = if (rawDate.length >= 4) rawDate.substring(0, 4) else ""
        
        return MediaItem(
            id = dto.id,
            title = dto.title ?: dto.name ?: "Bilinmeyen",
            posterPath = dto.posterPath,
            backdropPath = dto.backdropPath,
            year = year,
            overview = dto.overview ?: "",
            type = fallbackType,
            isFavorite = isFav,
            isInList = inList,
            isWatched = isWatched
        )
    }

    fun toggleFavorite(item: MediaItem) {
        val currentFavs = _favorites.value.toMutableList()
        val exists = currentFavs.find { it.id == item.id }
        if (exists != null) {
            currentFavs.remove(exists)
            val stillInList = _myList.value.any { it.id == item.id }
            if (!stillInList) {
                removeNote(item.id)
            }
        } else {
            currentFavs.add(item.copy(isFavorite = true))
        }
        _favorites.value = currentFavs
        saveLocalData()
        updateListIfNecessary(item.id, isFavorite = exists == null)
    }

    fun toggleList(item: MediaItem) {
        val currentList = _myList.value.toMutableList()
        val exists = currentList.find { it.id == item.id }
        if (exists != null) {
            currentList.remove(exists)
            val stillInFavs = _favorites.value.any { it.id == item.id }
            if (!stillInFavs) {
                removeNote(item.id)
            }
        } else {
            currentList.add(item.copy(isInList = true, isWatched = false))
        }
        _myList.value = currentList
        saveLocalData()
        updateFavIfNecessary(item.id, isInList = exists == null)
    }
    
    fun toggleWatchedStatus(item: MediaItem) {
        val newWatchedStatus = !item.isWatched

        val currentList = _myList.value.toMutableList()
        val listIndex = currentList.indexOfFirst { it.id == item.id }
        if (listIndex != -1) {
            currentList[listIndex] = currentList[listIndex].copy(isWatched = newWatchedStatus)
            _myList.value = currentList
        }

        val currentFavs = _favorites.value.toMutableList()
        val favIndex = currentFavs.indexOfFirst { it.id == item.id }
        if (favIndex != -1) {
            currentFavs[favIndex] = currentFavs[favIndex].copy(isWatched = newWatchedStatus)
            _favorites.value = currentFavs
        }

        saveLocalData()
    }
    
    private fun updateListIfNecessary(id: Int, isFavorite: Boolean) {
        val currentList = _myList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isFavorite = isFavorite)
            _myList.value = currentList
            saveLocalData()
        }
    }

    private fun updateFavIfNecessary(id: Int, isInList: Boolean) {
        val currentFavs = _favorites.value.toMutableList()
        val index = currentFavs.indexOfFirst { it.id == id }
        if (index != -1) {
            currentFavs[index] = currentFavs[index].copy(isInList = isInList)
            _favorites.value = currentFavs
            saveLocalData()
        }
    }
}
