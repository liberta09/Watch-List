package com.kaan.watchlist.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kaan.watchlist.R
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MediaRepository(val context: Context) {

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

    private val currentLanguage: String
        get() {
            val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            return if (appLocales.isEmpty) {
                if (java.util.Locale.getDefault().language == "tr") "tr-TR" else "en-US"
            } else {
                if (appLocales.get(0)?.language == "tr") "tr-TR" else "en-US"
            }
        }

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

    data class ImportStats(
        val itemsUpdated: Int,
        val itemsAdded: Int
    )

    fun importItems(items: List<MediaItem>): ImportStats {
        val currentList = _myList.value.toMutableList()
        var updatedCount = 0
        var addedCount = 0

        for (newItem in items) {
            val existingIndex = currentList.indexOfFirst { it.id == newItem.id && it.type == newItem.type }
            if (existingIndex >= 0) {
                val existing = currentList[existingIndex]
                
                // Merge episodes
                val mergedEpisodes = existing.watchedEpisodes.toMutableMap()
                for ((key, time) in newItem.watchedEpisodes) {
                    val existingTime = mergedEpisodes[key]
                    if (existingTime == null || time > existingTime) {
                        mergedEpisodes[key] = time
                    }
                }

                val merged = existing.copy(
                    isWatched = existing.isWatched || newItem.isWatched,
                    userRating = existing.userRating ?: newItem.userRating,
                    watchedAt = existing.watchedAt ?: newItem.watchedAt,
                    addedAt = existing.addedAt ?: newItem.addedAt,
                    watchedEpisodes = mergedEpisodes,
                    lastWatchedSeason = newItem.lastWatchedSeason ?: existing.lastWatchedSeason,
                    lastWatchedEpisode = newItem.lastWatchedEpisode ?: existing.lastWatchedEpisode,
                    isInList = true
                ).sanitized()
                
                if (merged != existing) {
                    currentList[existingIndex] = merged
                    updatedCount++
                }
            } else {
                currentList.add(newItem.copy(isInList = true).sanitized())
                addedCount++
            }
        }

        if (updatedCount > 0 || addedCount > 0) {
            _myList.value = currentList
            saveLocalData()
            updateFirebase()
        }

        return ImportStats(updatedCount, addedCount)
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
        com.kaan.watchlist.widget.WidgetUpdater.requestUpdate(context)
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
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") throw Exception(context.getString(R.string.err_api_key_missing))
        val response = tmdbApi.getPopularMovies(apiKey, language = currentLanguage)
        return response.results.map { dto -> 
            mapDtoToMediaItem(dto, MediaType.MOVIE) 
        }
    }

    suspend fun getPopularTvShows(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") throw Exception(context.getString(R.string.err_api_key_missing))
        val response = tmdbApi.getPopularTvShows(apiKey, language = currentLanguage)
        return response.results.map { dto -> 
            mapDtoToMediaItem(dto, MediaType.TV) 
        }
    }

    suspend fun getTrending(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = tmdbApi.getTrendingWeek(apiKey, language = currentLanguage)
            res.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }.map { dto ->
                val type = if (dto.mediaType == "movie") MediaType.MOVIE else MediaType.TV
                mapDtoToMediaItem(dto, type)
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getNowPlaying(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = tmdbApi.getNowPlayingMovies(apiKey, language = currentLanguage)
            res.results.map { dto -> mapDtoToMediaItem(dto, MediaType.MOVIE) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUpcoming(): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = tmdbApi.getUpcomingMovies(apiKey, language = currentLanguage)
            res.results.map { dto -> mapDtoToMediaItem(dto, MediaType.MOVIE) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRecommendations(mediaId: Int, type: MediaType): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return emptyList()
        return try {
            val res = if (type == MediaType.MOVIE) {
                tmdbApi.getMovieRecommendations(mediaId, apiKey, language = currentLanguage)
            } else {
                tmdbApi.getTvRecommendations(mediaId, apiKey, language = currentLanguage)
            }
            res.results.take(15).map { dto -> mapDtoToMediaItem(dto, type) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun search(query: String): List<MediaItem> {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") throw Exception(context.getString(R.string.err_api_key_missing))
        if (query.isBlank()) return emptyList()
        val response = tmdbApi.searchMulti(apiKey, query, language = currentLanguage)
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

        var videos = fetch(currentLanguage)
        if (videos.isNullOrEmpty() && currentLanguage != "en-US") videos = fetch("en-US")
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

    private fun enqueueTraktSyncBatch(items: List<SyncQueueItem>) {
        if (items.isEmpty()) return
        val traktRepo = TraktRepository(context, this)
        if (traktRepo.isConnected() && traktRepo.isAutoPushEnabled() && AuthRepository.isSignedIn) {
            CoroutineScope(Dispatchers.IO).launch {
                val filteredItems = mutableListOf<SyncQueueItem>()
                for (item in items) {
                    val key = when (item.actionType) {
                        SyncActionType.WATCHED_MOVIE -> TraktPushedStore.movieKey(item.tmdbId)
                        SyncActionType.WATCHED_EPISODES -> {
                            if (item.season != null && item.episode != null) {
                                TraktPushedStore.episodeKey(item.tmdbId, item.season, item.episode)
                            } else null
                        }
                        else -> null
                    }
                    if (key == null || !TraktPushedStore.isPushed(context, key)) {
                        filteredItems.add(item)
                    }
                }
                if (filteredItems.isNotEmpty()) {
                    for (item in filteredItems) {
                        TraktSyncQueue.enqueue(context, item)
                    }
                    com.kaan.watchlist.util.TraktSyncWorker.enqueue(context)
                }
            }
        }
    }

    private fun enqueueTraktSync(item: SyncQueueItem) {
        enqueueTraktSyncBatch(listOf(item))
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
            enqueueTraktSync(
                SyncQueueItem(
                    actionType = SyncActionType.WATCHLIST,
                    mediaType = item.type,
                    tmdbId = item.id
                )
            )
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

        if (watched) {
            if (item.type == MediaType.MOVIE) {
                enqueueTraktSync(
                    SyncQueueItem(
                        actionType = SyncActionType.WATCHED_MOVIE,
                        mediaType = MediaType.MOVIE,
                        tmdbId = item.id,
                        watchedAt = now
                    )
                )
            }
        } else {
            enqueueTraktSync(
                SyncQueueItem(
                    actionType = SyncActionType.WATCHLIST,
                    mediaType = item.type,
                    tmdbId = item.id
                )
            )
        }
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

        if (newWatchedStatus) {
            if (item.type == MediaType.MOVIE) {
                enqueueTraktSync(
                    SyncQueueItem(
                        actionType = SyncActionType.WATCHED_MOVIE,
                        mediaType = MediaType.MOVIE,
                        tmdbId = item.id,
                        watchedAt = now
                    )
                )
            }
        }
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

        if (rating != null) {
            enqueueTraktSync(
                SyncQueueItem(
                    actionType = SyncActionType.RATING,
                    mediaType = item.type,
                    tmdbId = item.id,
                    rating = rating,
                    ratedAt = now
                )
            )
        }
    }

    fun setEpisodeWatched(item: MediaItem, season: Int, episode: Int, watched: Boolean) {
        val inList = _myList.value.any { it.id == item.id }
        if (!inList) {
            addToList(item, watched = false)
        }

        val key = "S${season}_E${episode}"
        val targetItem = _myList.value.find { it.id == item.id } ?: item
        val newEpisodes = targetItem.watchedEpisodes.toMutableMap()
        val now = System.currentTimeMillis()

        if (watched) {
            newEpisodes[key] = now
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

        if (watched) {
            enqueueTraktSync(
                SyncQueueItem(
                    actionType = SyncActionType.WATCHED_EPISODES,
                    mediaType = MediaType.TV,
                    tmdbId = item.id,
                    season = season,
                    episode = episode,
                    watchedAt = now
                )
            )
        }
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

        if (watched) {
            val syncItems = mutableListOf<SyncQueueItem>()
            for (ep in 1..episodeCount) {
                val epKey = "S${season}_E${ep}"
                if (!targetItem.watchedEpisodes.containsKey(epKey)) {
                    syncItems.add(
                        SyncQueueItem(
                            actionType = SyncActionType.WATCHED_EPISODES,
                            mediaType = MediaType.TV,
                            tmdbId = item.id,
                            season = season,
                            episode = ep,
                            watchedAt = now
                        )
                    )
                }
            }
            if (syncItems.isNotEmpty()) {
                enqueueTraktSyncBatch(syncItems)
            }
        }
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
            tmdbApi.getTvSeasonDetails(tvId, seasonNumber, apiKey, language = currentLanguage)
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

    private fun mergeTmdbFields(current: MediaItem, fresh: MediaItem): MediaItem {
        return current.copy(
            originalTitle = fresh.originalTitle,
            voteAverage = fresh.voteAverage,
            runtime = fresh.runtime,
            episodeRuntime = fresh.episodeRuntime,
            genres = fresh.genres,
            productionCountries = fresh.productionCountries,
            director = fresh.director,
            cast = fresh.cast,
            videoKey = fresh.videoKey,
            totalSeasons = fresh.totalSeasons,
            totalEpisodes = fresh.totalEpisodes,
            showStatus = fresh.showStatus,
            nextEpisodeAirDate = fresh.nextEpisodeAirDate,
            nextEpisodeSeason = fresh.nextEpisodeSeason,
            nextEpisodeNumber = fresh.nextEpisodeNumber,
            nextEpisodeName = fresh.nextEpisodeName,
            lastAiredSeason = fresh.lastAiredSeason,
            lastAiredEpisode = fresh.lastAiredEpisode,
            releaseDate = fresh.releaseDate
        )
    }

    suspend fun fetchMediaDetails(item: MediaItem, persist: Boolean = true): MediaItem {
        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") return item
        return try {
            val dto = if (item.type == MediaType.MOVIE) {
                tmdbApi.getMovieDetails(item.id, apiKey, language = currentLanguage)
            } else {
                tmdbApi.getTvDetails(item.id, apiKey, language = currentLanguage)
            }
            
            val newGenres = dto.genres?.mapNotNull { it.name }?.map {
                if (currentLanguage.startsWith("tr")) genreTranslations[it] ?: it else it
            } ?: item.genres
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
            
            val freshItem = item.copy(
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
                totalEpisodes = dto.numberOfEpisodes ?: item.totalEpisodes,
                showStatus = dto.status,
                nextEpisodeAirDate = dto.nextEpisodeToAir?.airDate,
                nextEpisodeSeason = dto.nextEpisodeToAir?.seasonNumber,
                nextEpisodeNumber = dto.nextEpisodeToAir?.episodeNumber,
                nextEpisodeName = dto.nextEpisodeToAir?.name,
                lastAiredSeason = dto.lastEpisodeToAir?.seasonNumber,
                lastAiredEpisode = dto.lastEpisodeToAir?.episodeNumber,
                releaseDate = if (item.type == MediaType.MOVIE) dto.releaseDate else item.releaseDate
            )
            
            var finalItem = freshItem
            val inList = _myList.value.any { it.id == item.id }
            val inFav = _favorites.value.any { it.id == item.id }
            if (inList || inFav) {
                val latest = _myList.value.find { it.id == item.id } ?: _favorites.value.find { it.id == item.id } ?: item
                finalItem = mergeTmdbFields(latest, freshItem)
                
                updateListIfNecessary(item.id, updatedItem = finalItem)
                updateFavIfNecessary(item.id, updatedItem = finalItem)
                if (persist) {
                    saveLocalData()
                    updateFirebase()
                }
            }
            
            finalItem
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error fetching details for id=${item.id}: ${e.message}")
            item
        }
    }

    suspend fun refreshUpcomingInfo(): List<MediaItem> = coroutineScope {
        val allItems = (_myList.value + _favorites.value).distinctBy { it.id }.take(40)
        if (allItems.isEmpty()) return@coroutineScope emptyList()

        val lastRefresh = prefs.getLong("upcoming_last_refresh", 0L)
        val now = System.currentTimeMillis()
        // Son 12 saat içinde yenilenmişse tekrar istek atma
        if (now - lastRefresh < 12 * 60 * 60 * 1000L) {
            return@coroutineScope getUpcomingFromLocal(allItems)
        }

        if (apiKey.isBlank() || apiKey == "BURAYA_KULLANICININ_TMDB_API_KEY_DEGERI_GELECEK") {
            return@coroutineScope getUpcomingFromLocal(allItems)
        }

        val updatedList = mutableListOf<MediaItem>()
        for (chunk in allItems.chunked(5)) {
            val jobs = chunk.map { item ->
                async { fetchMediaDetails(item, persist = false) }
            }
            updatedList.addAll(jobs.awaitAll())
        }

        saveLocalData()
        updateFirebase()

        prefs.edit().putLong("upcoming_last_refresh", now).apply()
        return@coroutineScope getUpcomingFromLocal(updatedList)
    }

    private fun getUpcomingFromLocal(items: List<MediaItem>): List<MediaItem> {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("tr"))
        // Günü başlangıcına yuvarla, dünü de saymasın
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayMs = cal.timeInMillis
        val sixtyDaysMs = todayMs + 60L * 24 * 60 * 60 * 1000L

        return items.filter { item ->
            try {
                val dateStr = if (item.type == MediaType.TV) item.nextEpisodeAirDate else item.releaseDate
                if (dateStr.isNullOrBlank()) false
                else {
                    val date = formatter.parse(dateStr)
                    if (date != null) {
                        date.time in todayMs..sixtyDaysMs
                    } else false
                }
            } catch (e: Exception) {
                false
            }
        }.sortedBy { item ->
            val dateStr = if (item.type == MediaType.TV) item.nextEpisodeAirDate else item.releaseDate
            try {
                formatter.parse(dateStr!!)?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }
    }

    private fun generateShareCode(): String {
        val chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // Excluded 0, O, 1, I, L
        return (1..8).map { chars.random() }.joinToString("")
    }

    fun shareList(title: String, filter: com.kaan.watchlist.domain.model.ShareFilter, onResult: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            onResult(null)
            return
        }

        val filteredItems = _myList.value.filter { item ->
            when (filter) {
                com.kaan.watchlist.domain.model.ShareFilter.ALL -> true
                com.kaan.watchlist.domain.model.ShareFilter.WATCHLIST -> !item.isWatched
                com.kaan.watchlist.domain.model.ShareFilter.WATCHED -> item.isWatched
                com.kaan.watchlist.domain.model.ShareFilter.MOVIES -> item.type == MediaType.MOVIE
                com.kaan.watchlist.domain.model.ShareFilter.SHOWS -> item.type == MediaType.TV
            }
        }.map {
            com.kaan.watchlist.domain.model.SharedMediaItem(
                tmdbId = it.id,
                type = it.type.name,
                title = it.title,
                posterPath = it.posterPath,
                releaseDate = if (it.type == MediaType.TV) it.nextEpisodeAirDate else it.releaseDate,
                userRating = it.userRating,
                isWatched = it.isWatched
            )
        }

        val code = generateShareCode()
        val now = System.currentTimeMillis()
        val sharedList = com.kaan.watchlist.domain.model.SharedList(
            ownerUid = user.uid,
            ownerName = user.displayName ?: context.getString(R.string.default_user_name),
            title = title,
            createdAt = now,
            updatedAt = now,
            items = filteredItems
        )

        db.getReference("shared/$code").setValue(sharedList).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                db.getReference("users/${user.uid}/sharedCodes/$code").setValue(true)
                onResult(code)
            } else {
                onResult(null)
            }
        }
    }

    fun fetchSharedList(code: String, onResult: (com.kaan.watchlist.domain.model.SharedList?) -> Unit) {
        db.getReference("shared/$code").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val ownerUid = snapshot.child("ownerUid").getValue(String::class.java) ?: ""
                    val ownerName = snapshot.child("ownerName").getValue(String::class.java) ?: ""
                    val title = snapshot.child("title").getValue(String::class.java) ?: ""
                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L
                    
                    val items = mutableListOf<com.kaan.watchlist.domain.model.SharedMediaItem>()
                    for (itemSnapshot in snapshot.child("items").children) {
                        val tmdbId = itemSnapshot.child("tmdbId").getValue(Int::class.java) ?: 0
                        val type = itemSnapshot.child("type").getValue(String::class.java) ?: ""
                        val itemTitle = itemSnapshot.child("title").getValue(String::class.java) ?: ""
                        val posterPath = itemSnapshot.child("posterPath").getValue(String::class.java)
                        val releaseDate = itemSnapshot.child("releaseDate").getValue(String::class.java)
                        val userRating = itemSnapshot.child("userRating").getValue(Int::class.java)
                        val isWatched = itemSnapshot.child("isWatched").getValue(Boolean::class.java) ?: false
                        
                        items.add(
                            com.kaan.watchlist.domain.model.SharedMediaItem(
                                tmdbId = tmdbId, type = type, title = itemTitle, posterPath = posterPath,
                                releaseDate = releaseDate, userRating = userRating, isWatched = isWatched
                            )
                        )
                    }
                    
                    if (ownerUid.isBlank() && items.isEmpty()) {
                        onResult(null)
                    } else {
                        onResult(com.kaan.watchlist.domain.model.SharedList(ownerUid, ownerName, title, createdAt, updatedAt, items))
                    }
                } catch (e: Exception) {
                    onResult(null)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(null)
            }
        })
    }

    fun getMySharedLists(onResult: (List<com.kaan.watchlist.domain.model.SharedListInfo>) -> Unit) {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            onResult(emptyList())
            return
        }
        
        db.getReference("users/${user.uid}/sharedCodes").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val codes = snapshot.children.mapNotNull { it.key }
                if (codes.isEmpty()) {
                    onResult(emptyList())
                    return
                }
                
                val results = mutableListOf<com.kaan.watchlist.domain.model.SharedListInfo>()
                var pending = codes.size
                
                codes.forEach { code ->
                    db.getReference("shared/$code").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(shareSnapshot: DataSnapshot) {
                            val title = shareSnapshot.child("title").getValue(String::class.java)
                            val createdAt = shareSnapshot.child("createdAt").getValue(Long::class.java)
                            val updatedAt = shareSnapshot.child("updatedAt").getValue(Long::class.java)
                            
                            if (title != null && createdAt != null && updatedAt != null) {
                                results.add(com.kaan.watchlist.domain.model.SharedListInfo(code, title, createdAt, updatedAt))
                            } else {
                                // If shared list doesn't exist anymore, remove from user's codes
                                db.getReference("users/${user.uid}/sharedCodes/$code").removeValue()
                            }
                            
                            pending--
                            if (pending == 0) {
                                onResult(results.sortedByDescending { it.createdAt })
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            pending--
                            if (pending == 0) {
                                onResult(results.sortedByDescending { it.createdAt })
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    fun removeSharedList(code: String, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            onResult(false)
            return
        }
        
        db.getReference("shared/$code").removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                db.getReference("users/${user.uid}/sharedCodes/$code").removeValue()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    suspend fun refreshAllMediaLanguage() = coroutineScope {
        val allItems = (_myList.value + _favorites.value).distinctBy { it.id }
        if (allItems.isEmpty()) return@coroutineScope

        for (chunk in allItems.chunked(5)) {
            val jobs = chunk.map { item ->
                async { fetchMediaDetails(item, persist = false) }
            }
            jobs.awaitAll()
        }
        
        saveLocalData()
        updateFirebase()
    }
}
