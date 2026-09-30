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
import com.kaan.watchlist.data.api.TmdbApi
import com.kaan.watchlist.data.api.enableTlsChainFallback
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
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .enableTlsChainFallback()
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
                        _myList.value = list
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
                        _favorites.value = list
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
        updateFirebase()
    }

    fun addToList(item: MediaItem, watched: Boolean) {
        val currentList = _myList.value.toMutableList()
        val exists = currentList.find { it.id == item.id }
        if (exists != null) {
            return // Zaten listedeyse hiçbir şey yapma
        }

        currentList.add(item.copy(isInList = true, isWatched = watched))
        _myList.value = currentList

        val currentFavs = _favorites.value.toMutableList()
        val favIndex = currentFavs.indexOfFirst { it.id == item.id }
        if (favIndex != -1) {
            currentFavs[favIndex] = currentFavs[favIndex].copy(isInList = true, isWatched = watched)
            _favorites.value = currentFavs
        }

        saveLocalData()
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
            currentList.add(item.copy(isInList = true, isWatched = false))
        }
        _myList.value = currentList
        saveLocalData()
        updateFavIfNecessary(item.id, isInList = exists == null)
        updateFirebase()
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
        updateFirebase()
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
