package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaan.watchlist.BuildConfig
import com.kaan.watchlist.data.api.MediaDto
import com.kaan.watchlist.data.api.TvSeasonResponseDto
import com.kaan.watchlist.data.api.VideoDto
import com.kaan.watchlist.data.api.WatchProviderCountryDto
import com.kaan.watchlist.data.api.TmdbApi
import com.kaan.watchlist.domain.model.Announcement
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
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
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
    private val announcementRepository = AnnouncementRepository(context)

    private val genreTranslations = mapOf(
        "Talk" to "Talk Show",
        "Reality" to "Gerçeklik",
        "News" to "Haber",
        "Soap" to "Pembe Dizi",
        "Kids" to "Çocuk",
        "Sci-Fi & Fantasy" to "Bilim Kurgu & Fantastik",
        "Action & Adventure" to "Aksiyon & Macera",
        "War & Politics" to "Savaş & Politika"
    )

    private fun MediaItem.sanitized(): MediaItem {
        val g: List<String>? = genres
        val c: List<String>? = cast
        val pc: List<String>? = productionCountries
        val we: Map<String, Long>? = watchedEpisodes
        val t: String? = title
        val y: String? = year
        val o: String? = overview
        return copy(
            genres = g ?: emptyList(),
            cast = c ?: emptyList(),
            productionCountries = pc ?: emptyList(),
            watchedEpisodes = we ?: emptyMap(),
            title = t ?: "Bilinmeyen",
            year = y ?: "",
            overview = o ?: ""
        )
    }

    private val _myList = MutableStateFlow<List<MediaItem>>(emptyList())
    val myList: StateFlow<List<MediaItem>> = _myList.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _notes = MutableStateFlow<Map<Int, String>>(emptyMap())
    val notes: StateFlow<Map<Int, String>> = _notes.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    private val db get() = FirebaseDatabase.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var activeUid: String? = null

    private var myListListener: ValueEventListener? = null
    private var favsListener: ValueEventListener? = null
    private var notesListener: ValueEventListener? = null

    init {
        loadLocalData()
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val newUid = if (user != null && !user.isAnonymous) user.uid else null

            Log.d("FirebaseSync", "AUTH STATE CHANGED: signedIn=${user != null}, anonymous=${user?.isAnonymous}, uid=$newUid")

            if (newUid != activeUid) {
                detachFirebaseListeners()
                activeUid = newUid

                if (newUid != null) {
                    attachFirebaseListeners(newUid)
                } else {
                    // Logged out or anonymous
                    loadLocalData()
                }
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun detachFirebaseListeners() {
        val oldUid = activeUid ?: return
        myListListener?.let { db.getReference("users/$oldUid/myList").removeEventListener(it) }
        favsListener?.let { db.getReference("users/$oldUid/favorites").removeEventListener(it) }
        notesListener?.let { db.getReference("users/$oldUid/notes").removeEventListener(it) }

        myListListener = null
        favsListener = null
        notesListener = null
        activeUid = null
        Log.d("FirebaseSync", "DETACH LISTENERS for old uid=$oldUid")
    }

    private fun attachFirebaseListeners(uid: String) {
        Log.d("FirebaseSync", "ATTACH LISTENERS for uid=$uid")

        // 1. My List Listener
        myListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseSync", "READ myList: exists=${snapshot.exists()}, count=${snapshot.childrenCount}")
                if (snapshot.exists() && snapshot.value != null) {
                    val type = object : TypeToken<List<MediaItem>>() {}.type
                    val jsonStr = gson.toJson(snapshot.value)
                    val list: List<MediaItem>? = gson.fromJson(jsonStr, type)
                    if (list != null) {
                        _myList.value = list.map { it.sanitized() }
                        saveLocalData()
                    }
                } else {
                    // If Firebase is empty but local has data, upload local data for first sync
                    if (_myList.value.isNotEmpty()) {
                        Log.d("FirebaseSync", "Firebase myList empty, uploading local myList count=${_myList.value.size}")
                        db.getReference("users/$uid/myList").setValue(_myList.value)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "READ ERROR myList: ${error.message}", error.toException())
            }
        }
        db.getReference("users/$uid/myList").addValueEventListener(myListListener!!)

        // 2. Favorites Listener
        favsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseSync", "READ favorites: exists=${snapshot.exists()}, count=${snapshot.childrenCount}")
                if (snapshot.exists() && snapshot.value != null) {
                    val type = object : TypeToken<List<MediaItem>>() {}.type
                    val jsonStr = gson.toJson(snapshot.value)
                    val list: List<MediaItem>? = gson.fromJson(jsonStr, type)
                    if (list != null) {
                        _favorites.value = list.map { it.sanitized() }
                        saveLocalData()
                    }
                } else {
                    if (_favorites.value.isNotEmpty()) {
                        Log.d("FirebaseSync", "Firebase favorites empty, uploading local favorites count=${_favorites.value.size}")
                        db.getReference("users/$uid/favorites").setValue(_favorites.value)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "READ ERROR favorites: ${error.message}", error.toException())
            }
        }
        db.getReference("users/$uid/favorites").addValueEventListener(favsListener!!)

        // 3. Notes Listener
        notesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseSync", "READ notes: exists=${snapshot.exists()}, count=${snapshot.childrenCount}")
                if (snapshot.exists() && snapshot.value != null) {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val jsonStr = gson.toJson(snapshot.value)
                    val map: Map<String, String>? = gson.fromJson(jsonStr, type)
                    if (map != null) {
                        val intMap = map.mapKeys { it.key.toIntOrNull() ?: -1 }.filterKeys { it != -1 }
                        _notes.value = intMap
                        saveLocalData()
                    }
                } else {
                    if (_notes.value.isNotEmpty()) {
                        Log.d("FirebaseSync", "Firebase notes empty, uploading local notes count=${_notes.value.size}")
                        val stringNotes = _notes.value.mapKeys { it.key.toString() }
                        db.getReference("users/$uid/notes").setValue(stringNotes)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "READ ERROR notes: ${error.message}", error.toException())
            }
        }
        db.getReference("users/$uid/notes").addValueEventListener(notesListener!!)
    }

    private fun updateFirebase() {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            val uid = user.uid
            Log.d("FirebaseSync", "WRITE REQUEST: uid=$uid, myListCount=${_myList.value.size}, favsCount=${_favorites.value.size}, notesCount=${_notes.value.size}")

            db.getReference("users/$uid/myList").setValue(_myList.value)
                .addOnSuccessListener { Log.d("FirebaseSync", "WRITE SUCCESS: users/$uid/myList") }
                .addOnFailureListener { e -> Log.e("FirebaseSync", "WRITE FAILURE: users/$uid/myList", e) }

            db.getReference("users/$uid/favorites").setValue(_favorites.value)
                .addOnSuccessListener { Log.d("FirebaseSync", "WRITE SUCCESS: users/$uid/favorites") }
                .addOnFailureListener { e -> Log.e("FirebaseSync", "WRITE FAILURE: users/$uid/favorites", e) }

            val stringNotes = _notes.value.mapKeys { it.key.toString() }
            db.getReference("users/$uid/notes").setValue(stringNotes)
                .addOnSuccessListener { Log.d("FirebaseSync", "WRITE SUCCESS: users/$uid/notes") }
                .addOnFailureListener { e -> Log.e("FirebaseSync", "WRITE FAILURE: users/$uid/notes", e) }
        } else {
            Log.d("FirebaseSync", "WRITE SKIPPED: User is guest or anonymous")
        }
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
        
        _myList.value = list.map { it.sanitized() }
        _favorites.value = favs.map { it.sanitized() }
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
        updateFirebase()
    }

    fun removeNote(mediaId: Int) {
        val currentNotes = _notes.value.toMutableMap()
        currentNotes.remove(mediaId)
        _notes.value = currentNotes
        saveLocalData()
        updateFirebase()
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    suspend fun fetchAnnouncement(): Announcement? {
        return announcementRepository.fetchAnnouncement()
    }

    fun dismissAnnouncement(id: String) {
        announcementRepository.dismissAnnouncement(id)
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

    suspend fun getTrending(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = tmdbApi.getTrendingWeek(apiKey)
            res.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { dto ->
                val type = if (dto.mediaType == "movie") MediaType.MOVIE else MediaType.TV
                mapDtoToMediaItem(dto, type)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getNowPlaying(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = tmdbApi.getNowPlayingMovies(apiKey)
            res.results.map { dto -> mapDtoToMediaItem(dto, MediaType.MOVIE) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUpcoming(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = tmdbApi.getUpcomingMovies(apiKey)
            res.results.map { dto -> mapDtoToMediaItem(dto, MediaType.MOVIE) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRecommendations(mediaId: Int, type: MediaType): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = if (type == MediaType.MOVIE) {
                tmdbApi.getMovieRecommendations(mediaId, apiKey)
            } else {
                tmdbApi.getTvRecommendations(mediaId, apiKey)
            }
            res.results.take(15).map { dto -> mapDtoToMediaItem(dto, type) }
        } catch (e: Exception) { emptyList() }
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

    suspend fun getTrailerKey(mediaId: Int, type: MediaType): String? {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return null
        
        suspend fun fetch(lang: String?): List<VideoDto>? {
            return try {
                val res = if (type == MediaType.MOVIE) {
                    tmdbApi.getMovieVideos(mediaId, apiKey, lang)
                } else {
                    tmdbApi.getTvVideos(mediaId, apiKey, lang)
                }
                Log.d("TRAILER", "Fetched videos lang=$lang, count=${res.results?.size ?: 0}")
                res.results
            } catch (e: Exception) {
                Log.e("TRAILER", "Error fetching videos lang=$lang: ${e.message}")
                null
            }
        }

        var videos = fetch("tr-TR")
        if (videos.isNullOrEmpty()) videos = fetch("en-US")
        if (videos.isNullOrEmpty()) videos = fetch(null)

        if (videos.isNullOrEmpty()) {
            Log.d("TRAILER", "No videos found for mediaId=$mediaId, type=$type")
            return null
        }

        val officialTrailer = videos.firstOrNull { it.type == "Trailer" && it.site == "YouTube" && it.official == true }
        if (officialTrailer != null) {
            Log.d("TRAILER", "Selected official trailer key=${officialTrailer.key}")
            return officialTrailer.key
        }

        val anyTrailer = videos.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }
        if (anyTrailer != null) {
            Log.d("TRAILER", "Selected trailer key=${anyTrailer.key}")
            return anyTrailer.key
        }

        val teaser = videos.firstOrNull { it.type == "Teaser" && it.site == "YouTube" }
        if (teaser != null) {
            Log.d("TRAILER", "Selected teaser key=${teaser.key}")
            return teaser.key
        }

        val anyYoutube = videos.firstOrNull { it.site == "YouTube" }
        Log.d("TRAILER", "Selected fallback youtube key=${anyYoutube?.key}")
        return anyYoutube?.key
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
            isWatched = isWatched,
            voteAverage = dto.voteAverage ?: existingList?.voteAverage ?: existingFav?.voteAverage,
            addedAt = existingList?.addedAt ?: existingFav?.addedAt,
            watchedAt = existingList?.watchedAt ?: existingFav?.watchedAt,
            userRating = existingList?.userRating ?: existingFav?.userRating,
            watchedEpisodes = existingList?.watchedEpisodes ?: existingFav?.watchedEpisodes ?: emptyMap()
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
            val now = System.currentTimeMillis()
            currentFavs.add(item.copy(isFavorite = true, addedAt = item.addedAt ?: now))
        }
        _favorites.value = currentFavs
        saveLocalData()
        updateListIfNecessary(item.id, isFavorite = exists == null)
        updateFirebase()
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
            val now = System.currentTimeMillis()
            currentList.add(item.copy(isInList = true, isWatched = false, addedAt = item.addedAt ?: now))
        }
        _myList.value = currentList
        saveLocalData()
        updateFavIfNecessary(item.id, isInList = exists == null)
        updateFirebase()
    }

    fun addToList(item: MediaItem, watched: Boolean) {
        val currentList = _myList.value.toMutableList()
        val exists = currentList.find { it.id == item.id }
        if (exists != null) {
            return // Zaten listedeyse hiçbir şey yapma
        }

        val now = System.currentTimeMillis()
        val newItem = item.copy(
            isInList = true,
            isWatched = watched,
            addedAt = now,
            watchedAt = if (watched) now else null
        )
        currentList.add(newItem)
        _myList.value = currentList

        val currentFavs = _favorites.value.toMutableList()
        val favIndex = currentFavs.indexOfFirst { it.id == item.id }
        if (favIndex != -1) {
            currentFavs[favIndex] = currentFavs[favIndex].copy(
                isInList = true,
                isWatched = watched,
                addedAt = now,
                watchedAt = if (watched) now else null
            )
            _favorites.value = currentFavs
        }

        saveLocalData()
        updateFirebase()
    }

    fun toggleWatchedStatus(item: MediaItem) {
        val newWatchedStatus = !item.isWatched
        val now = System.currentTimeMillis()
        val newWatchedAt = if (newWatchedStatus) now else null

        val currentList = _myList.value.toMutableList()
        val listIndex = currentList.indexOfFirst { it.id == item.id }
        if (listIndex != -1) {
            currentList[listIndex] = currentList[listIndex].copy(
                isWatched = newWatchedStatus,
                watchedAt = newWatchedAt
            )
            _myList.value = currentList
        }

        val currentFavs = _favorites.value.toMutableList()
        val favIndex = currentFavs.indexOfFirst { it.id == item.id }
        if (favIndex != -1) {
            currentFavs[favIndex] = currentFavs[favIndex].copy(
                isWatched = newWatchedStatus,
                watchedAt = newWatchedAt
            )
            _favorites.value = currentFavs
        }

        saveLocalData()
        updateFirebase()
    }

    fun setUserRating(item: MediaItem, rating: Int?) {
        val now = System.currentTimeMillis()
        val inList = _myList.value.any { it.id == item.id }
        val inFav = _favorites.value.any { it.id == item.id }

        if (!inList && !inFav) {
            addToList(item, watched = true)
        }

        val currentList = _myList.value.toMutableList()
        val listIndex = currentList.indexOfFirst { it.id == item.id }
        if (listIndex != -1) {
            currentList[listIndex] = currentList[listIndex].copy(userRating = rating)
            _myList.value = currentList
        }

        val currentFavs = _favorites.value.toMutableList()
        val favIndex = currentFavs.indexOfFirst { it.id == item.id }
        if (favIndex != -1) {
            currentFavs[favIndex] = currentFavs[favIndex].copy(userRating = rating)
            _favorites.value = currentFavs
        }

        saveLocalData()
        updateFirebase()
    }

    fun setEpisodeWatched(item: MediaItem, season: Int, episode: Int, watched: Boolean) {
        val inList = _myList.value.any { it.id == item.id }
        if (!inList) {
            addToList(item, watched = false)
        }

        val key = "S${season}_E${episode}"
        val targetItem = _myList.value.find { it.id == item.id } ?: item
        val newEpisodes = targetItem.watchedEpisodes.toMutableMap()

        if (watched) {
            newEpisodes[key] = System.currentTimeMillis()
        } else {
            newEpisodes.remove(key)
        }

        val updatedItem = targetItem.copy(
            watchedEpisodes = newEpisodes,
            lastWatchedSeason = if (watched) season else targetItem.lastWatchedSeason,
            lastWatchedEpisode = if (watched) episode else targetItem.lastWatchedEpisode
        )

        updateListIfNecessary(item.id, isInList = true, updatedItem = updatedItem)
        updateFavIfNecessary(item.id, isInList = true, updatedItem = updatedItem)
        saveLocalData()
        updateFirebase()
    }

    fun setSeasonWatched(item: MediaItem, season: Int, episodeCount: Int, watched: Boolean) {
        val inList = _myList.value.any { it.id == item.id }
        if (!inList) {
            addToList(item, watched = false)
        }

        val targetItem = _myList.value.find { it.id == item.id } ?: item
        val newEpisodes = targetItem.watchedEpisodes.toMutableMap()
        val now = System.currentTimeMillis()

        for (ep in 1..episodeCount) {
            val key = "S${season}_E${ep}"
            if (watched) {
                newEpisodes[key] = now
            } else {
                newEpisodes.remove(key)
            }
        }

        val updatedItem = targetItem.copy(
            watchedEpisodes = newEpisodes,
            lastWatchedSeason = if (watched) season else targetItem.lastWatchedSeason,
            lastWatchedEpisode = if (watched) episodeCount else targetItem.lastWatchedEpisode
        )

        updateListIfNecessary(item.id, isInList = true, updatedItem = updatedItem)
        updateFavIfNecessary(item.id, isInList = true, updatedItem = updatedItem)
        saveLocalData()
        updateFirebase()
    }
    
    private fun updateListIfNecessary(id: Int, isFavorite: Boolean? = null, isInList: Boolean? = null, updatedItem: MediaItem? = null) {
        val currentList = _myList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val base = updatedItem ?: currentList[index]
            val withFav = if (isFavorite != null) base.copy(isFavorite = isFavorite) else base
            currentList[index] = if (isInList != null) withFav.copy(isInList = isInList) else withFav
            _myList.value = currentList
            saveLocalData()
        }
    }

    private fun updateFavIfNecessary(id: Int, isInList: Boolean? = null, isFavorite: Boolean? = null, updatedItem: MediaItem? = null) {
        val currentFavs = _favorites.value.toMutableList()
        val index = currentFavs.indexOfFirst { it.id == id }
        if (index != -1) {
            val base = updatedItem ?: currentFavs[index]
            val withList = if (isInList != null) base.copy(isInList = isInList) else base
            currentFavs[index] = if (isFavorite != null) withList.copy(isFavorite = isFavorite) else withList
            _favorites.value = currentFavs
            saveLocalData()
        }
    }
    
    suspend fun getTvSeasonDetails(tvId: Int, seasonNumber: Int): TvSeasonResponseDto? {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return null
        return try {
            tmdbApi.getTvSeasonDetails(tvId, seasonNumber, apiKey)
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error fetching tv season details: ${e.message}")
            null
        }
    }

    suspend fun getWatchProviders(mediaId: Int, type: MediaType): WatchProviderCountryDto? {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return null
        return try {
            val response = if (type == MediaType.MOVIE) {
                tmdbApi.getMovieWatchProviders(mediaId, apiKey)
            } else {
                tmdbApi.getTvWatchProviders(mediaId, apiKey)
            }
            response.results?.get("TR")
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error fetching watch providers: ${e.message}")
            null
        }
    }

    suspend fun fetchMediaDetails(item: MediaItem): MediaItem {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return item
        return try {
            val dto = if (item.type == MediaType.MOVIE) {
                tmdbApi.getMovieDetails(item.id, apiKey)
            } else {
                tmdbApi.getTvDetails(item.id, apiKey)
            }
            
            val newGenres = dto.genres?.mapNotNull { it.name }?.map { genreTranslations[it] ?: it } ?: item.genres
            val newCountries = dto.productionCountries?.mapNotNull { it.name } ?: item.productionCountries
            val newCast = dto.credits?.cast?.take(5)?.mapNotNull { it.name } ?: item.cast
            val newDirector = dto.credits?.crew?.firstOrNull { it.job == "Director" }?.name ?: item.director
            val newVoteAverage = dto.voteAverage ?: item.voteAverage
            val newRuntime = dto.runtime ?: dto.episodeRunTime?.firstOrNull() ?: item.runtime
            
            val videoList = dto.videos?.results ?: emptyList()
            val officialTrailer = videoList.firstOrNull { it.type == "Trailer" && it.site == "YouTube" && it.official == true }?.key
                ?: videoList.firstOrNull { it.type == "Trailer" && it.site == "YouTube" }?.key
                ?: videoList.firstOrNull { it.type == "Teaser" && it.site == "YouTube" }?.key
                ?: videoList.firstOrNull { it.site == "YouTube" }?.key

            val newVideoKey = officialTrailer ?: getTrailerKey(item.id, item.type) ?: item.videoKey
            Log.d("TRAILER", "fetchMediaDetails for mediaId=${item.id}, type=${item.type}, resolvedVideoKey=$newVideoKey")
            
            item.copy(
                originalTitle = dto.originalTitle ?: dto.originalName ?: item.originalTitle,
                voteAverage = newVoteAverage,
                runtime = newRuntime,
                episodeRuntime = dto.episodeRunTime?.firstOrNull() ?: item.episodeRuntime,
                genres = newGenres,
                productionCountries = newCountries,
                director = newDirector,
                cast = newCast,
                videoKey = newVideoKey,
                totalSeasons = dto.numberOfSeasons ?: item.totalSeasons,
                totalEpisodes = dto.numberOfEpisodes ?: item.totalEpisodes
            )
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error fetching details for id=${item.id}: ${e.message}")
            item
        }
    }
}
