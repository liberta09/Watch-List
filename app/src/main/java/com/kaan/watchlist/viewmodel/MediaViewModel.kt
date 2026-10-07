package com.kaan.watchlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaan.watchlist.data.repository.MediaRepository
import com.kaan.watchlist.data.api.TvSeasonResponseDto
import com.kaan.watchlist.data.api.WatchProviderCountryDto
import com.kaan.watchlist.domain.model.Announcement
import com.kaan.watchlist.domain.model.MediaType
import com.kaan.watchlist.domain.model.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.kaan.watchlist.data.repository.UpdateRepository
import com.kaan.watchlist.data.repository.UpdateStatus

class MediaViewModel(
    private val repository: MediaRepository,
    private val traktRepository: com.kaan.watchlist.data.repository.TraktRepository
) : ViewModel() {

    private val updateRepository = UpdateRepository()
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _currentAnnouncement = MutableStateFlow<Announcement?>(null)
    val currentAnnouncement: StateFlow<Announcement?> = _currentAnnouncement.asStateFlow()

    private var searchJob: Job? = null

    private val _popularMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val popularMovies: StateFlow<List<MediaItem>> = _popularMovies.asStateFlow()

    private val _popularTvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    val popularTvShows: StateFlow<List<MediaItem>> = _popularTvShows.asStateFlow()

    private val _trending = MutableStateFlow<List<MediaItem>>(emptyList())
    val trending: StateFlow<List<MediaItem>> = _trending.asStateFlow()

    private val _nowPlaying = MutableStateFlow<List<MediaItem>>(emptyList())
    val nowPlaying: StateFlow<List<MediaItem>> = _nowPlaying.asStateFlow()

    private val _upcoming = MutableStateFlow<List<MediaItem>>(emptyList())
    val upcoming: StateFlow<List<MediaItem>> = _upcoming.asStateFlow()

    private val _upcomingForUser = MutableStateFlow<List<MediaItem>>(emptyList())
    val upcomingForUser: StateFlow<List<MediaItem>> = _upcomingForUser.asStateFlow()

    private val _personalRecommendations = MutableStateFlow<List<MediaItem>>(emptyList())
    val personalRecommendations: StateFlow<List<MediaItem>> = _personalRecommendations.asStateFlow()

    private val _recommendationReasons = MutableStateFlow<Map<Int, String>>(emptyMap())
    val recommendationReasons: StateFlow<Map<Int, String>> = _recommendationReasons.asStateFlow()

    private val _recommendations = MutableStateFlow<List<MediaItem>>(emptyList())
    val recommendations: StateFlow<List<MediaItem>> = _recommendations.asStateFlow()

    private val _isDiscoverLoading = MutableStateFlow(true)
    val isDiscoverLoading = _isDiscoverLoading.asStateFlow()

    private val _discoverError = MutableStateFlow<String?>(null)
    val discoverError = _discoverError.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia = _selectedMedia.asStateFlow()
    
    private val _isLoadingDetails = MutableStateFlow(false)
    val isLoadingDetails = _isLoadingDetails.asStateFlow()

    private val _watchProviders = MutableStateFlow<WatchProviderCountryDto?>(null)
    val watchProviders = _watchProviders.asStateFlow()

    private val tvSeasonsCache = mutableMapOf<String, TvSeasonResponseDto>()
    private val _currentSeasonDetails = MutableStateFlow<TvSeasonResponseDto?>(null)
    val currentSeasonDetails = _currentSeasonDetails.asStateFlow()
    
    private val _currentSeasonKey = MutableStateFlow<String?>(null)
    val currentSeasonKey = _currentSeasonKey.asStateFlow()
    
    private val mediaCache = mutableMapOf<Int, MediaItem>()
    private var detailJob: Job? = null

    private val _detailNotFound = MutableStateFlow(false)
    val detailNotFound = _detailNotFound.asStateFlow()

    val myList = repository.myList
    val favorites = repository.favorites
    val notes = repository.notes
    val notificationsEnabled = repository.notificationsEnabled

    fun withUserState(item: MediaItem, myList: List<MediaItem>, favorites: List<MediaItem>): MediaItem {
        val listRecord = myList.find { it.id == item.id }
        val favRecord = favorites.find { it.id == item.id }

        val inList = listRecord != null
        val isFav = favRecord != null
        val isWatched = listRecord?.isWatched ?: favRecord?.isWatched ?: item.isWatched
        val userRating = listRecord?.userRating ?: favRecord?.userRating ?: item.userRating
        val watchedAt = listRecord?.watchedAt ?: favRecord?.watchedAt ?: item.watchedAt
        val addedAt = listRecord?.addedAt ?: favRecord?.addedAt ?: item.addedAt
        val tempEpisodes = listRecord?.watchedEpisodes ?: favRecord?.watchedEpisodes ?: item.watchedEpisodes
        val watchedEpisodes = tempEpisodes ?: emptyMap()
        val isTracked = listRecord?.isTracked ?: favRecord?.isTracked ?: item.isTracked
        val lastWatchedSeason = listRecord?.lastWatchedSeason ?: favRecord?.lastWatchedSeason ?: item.lastWatchedSeason
        val lastWatchedEpisode = listRecord?.lastWatchedEpisode ?: favRecord?.lastWatchedEpisode ?: item.lastWatchedEpisode

        return item.copy(
            isInList = inList,
            isFavorite = isFav,
            isWatched = isWatched,
            userRating = userRating,
            watchedAt = watchedAt,
            addedAt = addedAt,
            watchedEpisodes = watchedEpisodes,
            isTracked = isTracked,
            lastWatchedSeason = lastWatchedSeason,
            lastWatchedEpisode = lastWatchedEpisode
        )
    }

    fun isLoggedIn(): Boolean = repository.isLoggedIn

    fun setLoggedIn(loggedIn: Boolean) {
        repository.isLoggedIn = loggedIn
    }
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    init {
        loadHomeData()
        checkForUpdates()
        loadAnnouncement()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isDiscoverLoading.value = true
            _discoverError.value = null
            
            val popMoviesDef = async {
                try { repository.getPopularMovies() } catch (e: Exception) { emptyList<MediaItem>() }
            }
            val popTvDef = async {
                try { repository.getPopularTvShows() } catch (e: Exception) { emptyList<MediaItem>() }
            }
            val trendDef = async {
                try { repository.getTrending() } catch (e: Exception) { emptyList<MediaItem>() }
            }
            val nowPlayDef = async {
                try { repository.getNowPlaying() } catch (e: Exception) { emptyList<MediaItem>() }
            }
            val upcomingDef = async {
                try { repository.getUpcoming() } catch (e: Exception) { emptyList<MediaItem>() }
            }
            val upcomingUserDef = async {
                try { repository.refreshUpcomingInfo() } catch (e: Exception) { emptyList<MediaItem>() }
            }

            // Personal Recommendations Logic
            val currentMyList = repository.myList.value
            val currentFavorites = repository.favorites.value
            val allUserMedia = (currentMyList + currentFavorites).distinctBy { it.id }
            
            val highlyRated = allUserMedia.filter { (it.userRating ?: 0) >= 7 }
                .sortedByDescending { it.userRating }
                .take(3)
                
            val recentlyWatched = allUserMedia.filter { it.isWatched }
                .sortedByDescending { it.watchedAt ?: 0L }
                .take(3)
                
            val seeds = (highlyRated + recentlyWatched).distinctBy { it.id }.take(5)
                
            val personalRecsMap = mutableMapOf<Int, Int>()
            val personalRecsReasons = mutableMapOf<Int, String>()
            val fetchedRecs = mutableMapOf<Int, MediaItem>()
            
            if (seeds.isNotEmpty()) {
                val recJobs = seeds.map { seed ->
                    async {
                        try {
                            val recs = repository.getRecommendations(seed.id, seed.type)
                            Pair(seed.title, recs)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val results = recJobs.awaitAll().filterNotNull()
                
                val favList = repository.favorites.value
                for ((seedTitle, recs) in results) {
                    for (rec in recs) {
                        val inList = currentMyList.any { it.id == rec.id }
                        val inFav = favList.any { it.id == rec.id }
                        if (!inList && !inFav) {
                            personalRecsMap[rec.id] = personalRecsMap.getOrDefault(rec.id, 0) + 1
                            if (!personalRecsReasons.containsKey(rec.id)) {
                                personalRecsReasons[rec.id] = seedTitle
                            }
                            fetchedRecs[rec.id] = rec
                        }
                    }
                }
            }

            val sortedPersonalRecs = fetchedRecs.values.sortedWith(compareByDescending<MediaItem> { personalRecsMap[it.id] ?: 0 }
                .thenByDescending { it.voteAverage ?: 0.0 })
                .take(20)

            _personalRecommendations.value = sortedPersonalRecs.also { list -> list.forEach { mediaCache[it.id] = it } }
            _recommendationReasons.value = personalRecsReasons

            _popularMovies.value = popMoviesDef.await().also { list -> list.forEach { mediaCache[it.id] = it } }
            _popularTvShows.value = popTvDef.await().also { list -> list.forEach { mediaCache[it.id] = it } }
            _trending.value = trendDef.await().also { list -> list.forEach { mediaCache[it.id] = it } }
            _nowPlaying.value = nowPlayDef.await().also { list -> list.forEach { mediaCache[it.id] = it } }
            _upcoming.value = upcomingDef.await().also { list -> list.forEach { mediaCache[it.id] = it } }
            _upcomingForUser.value = upcomingUserDef.await().also { list -> list.forEach { mediaCache[it.id] = it } }

            if (_popularMovies.value.isEmpty() && _popularTvShows.value.isEmpty()) {
                _discoverError.value = "İçerikler yüklenemedi. Lütfen internet bağlantınızı kontrol edin."
            }
            
            _isDiscoverLoading.value = false
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        
        if (query.length < 3) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _isSearching.value = false
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(400) // Debounce for 400ms
            
            _isSearching.value = true
            _searchError.value = null
            try {
                val results = repository.search(query)
                if (results.isEmpty()) {
                    _searchError.value = "Sonuç bulunamadı."
                }
                _searchResults.value = results.also { list -> list.forEach { mediaCache[it.id] = it } }
            } catch (e: Exception) {
                _searchError.value = "Arama sırasında bir hata oluştu: ${e.localizedMessage}"
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun clearSearch() {
        _searchResults.value = emptyList()
    }
    
    fun loadMediaDetails(mediaId: Int) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _isLoadingDetails.value = true
            _detailNotFound.value = false

            val baseMedia = myList.value.find { it.id == mediaId }
                ?: favorites.value.find { it.id == mediaId }
                ?: mediaCache[mediaId]
            
            _recommendations.value = emptyList()
            _watchProviders.value = null
            _currentSeasonDetails.value = null

            if (baseMedia != null) {
                // First set what we have so UI can show it immediately
                _selectedMedia.value = baseMedia
                loadWatchProviders(baseMedia.id, baseMedia.type)
                loadRecommendations(baseMedia.id, baseMedia.type)
                // Then fetch details
                val detailedMedia = repository.fetchMediaDetails(baseMedia)
                mediaCache[detailedMedia.id] = detailedMedia
                _selectedMedia.value = detailedMedia
            } else {
                _selectedMedia.value = null
                _detailNotFound.value = true
            }
            _isLoadingDetails.value = false
        }
    }

    fun loadRecommendations(mediaId: Int, type: MediaType) {
        viewModelScope.launch {
            _recommendations.value = repository.getRecommendations(mediaId, type).also { list -> list.forEach { mediaCache[it.id] = it } }
        }
    }

    fun loadWatchProviders(mediaId: Int, type: MediaType) {
        viewModelScope.launch {
            _watchProviders.value = repository.getWatchProviders(mediaId, type)
        }
    }

    fun loadTvSeasonDetails(tvId: Int, seasonNumber: Int) {
        val cacheKey = "${tvId}_$seasonNumber"
        _currentSeasonKey.value = cacheKey
        if (tvSeasonsCache.containsKey(cacheKey)) {
            _currentSeasonDetails.value = tvSeasonsCache[cacheKey]
            return
        }

        viewModelScope.launch {
            val details = repository.getTvSeasonDetails(tvId, seasonNumber)
            if (details != null && _currentSeasonKey.value == cacheKey) {
                tvSeasonsCache[cacheKey] = details
                _currentSeasonDetails.value = details
            }
        }
    }
    
    fun clearSelectedMedia() {
        _selectedMedia.value = null
    }

    fun toggleFavorite(item: MediaItem) {
        repository.toggleFavorite(item)
    }

    fun addToList(item: MediaItem, watched: Boolean) {
        repository.addToList(item, watched)
    }

    fun toggleList(item: MediaItem) {
        repository.toggleList(item)
    }

    fun toggleWatchedStatus(item: MediaItem) {
        repository.toggleWatchedStatus(item)
    }

    fun setUserRating(item: MediaItem, rating: Int?) {
        repository.setUserRating(item, rating)
    }

    fun setEpisodeWatched(item: MediaItem, season: Int, episode: Int, watched: Boolean) {
        repository.setEpisodeWatched(item, season, episode, watched)
    }

    fun setSeasonWatched(item: MediaItem, season: Int, episodeCount: Int, watched: Boolean) {
        repository.setSeasonWatched(item, season, episodeCount, watched)
    }

    fun saveNote(mediaId: Int, note: String) {
        repository.saveNote(mediaId, note)
    }

    fun removeNote(mediaId: Int) {
        repository.removeNote(mediaId)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.toggleNotifications(enabled)
    }

    fun loadAnnouncement() {
        viewModelScope.launch {
            _currentAnnouncement.value = repository.fetchAnnouncement()
        }
    }

    fun dismissAnnouncement(id: String) {
        repository.dismissAnnouncement(id)
        _currentAnnouncement.value = null
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            _updateStatus.value = updateRepository.checkForUpdates()
        }
    }

    // --- TRAKT.TV ---
    private val _traktConnected = MutableStateFlow(false)
    val traktConnected: StateFlow<Boolean> = _traktConnected

    val traktConfigured: Boolean get() = traktRepository.isConfigured

    private val _traktImportState = MutableStateFlow<ImportState>(ImportState.Idle)
    val traktImportState: StateFlow<ImportState> = _traktImportState

    private val _traktDeviceCode = MutableStateFlow<com.kaan.watchlist.data.api.DeviceCodeResponse?>(null)
    val traktDeviceCode: StateFlow<com.kaan.watchlist.data.api.DeviceCodeResponse?> = _traktDeviceCode

    fun checkTraktConnection() {
        viewModelScope.launch {
            traktRepository.refreshIfNeeded()
            _traktConnected.value = traktRepository.isConnected()
        }
    }

    fun startTraktAuth() {
        viewModelScope.launch {
            _traktDeviceCode.value = null
            val code = traktRepository.startDeviceAuth()
            if (code != null) {
                _traktDeviceCode.value = code
                val success = traktRepository.pollForToken(code.device_code, code.interval, code.expires_in)
                _traktDeviceCode.value = null
                if (success) {
                    _traktConnected.value = true
                }
            }
        }
    }

    fun cancelTraktAuth() {
        _traktDeviceCode.value = null
    }

    fun disconnectTrakt() {
        traktRepository.disconnect()
        _traktConnected.value = false
    }

    fun resetTraktImportState() {
        _traktImportState.value = ImportState.Idle
    }

    fun importFromTrakt() {
        if (_traktImportState.value is ImportState.InProgress) return
        viewModelScope.launch {
            _traktImportState.value = ImportState.InProgress(0, 0)
            val result = traktRepository.importAll { done, total ->
                _traktImportState.value = ImportState.InProgress(done, total)
            }
            _traktImportState.value = ImportState.Completed(result)
            if (result.errorMessage == null) {
                repository.refreshUpcomingInfo()
                loadHomeData()
                com.kaan.watchlist.widget.WidgetUpdater.requestUpdate(repository.context)
            }
        }
    }

    // --- Sharing ---
    fun shareList(title: String, filter: com.kaan.watchlist.domain.model.ShareFilter, onResult: (String?) -> Unit) {
        repository.shareList(title, filter, onResult)
    }

    fun fetchSharedList(code: String, onResult: (com.kaan.watchlist.domain.model.SharedList?) -> Unit) {
        repository.fetchSharedList(code, onResult)
    }

    fun getMySharedLists(onResult: (List<com.kaan.watchlist.domain.model.SharedListInfo>) -> Unit) {
        repository.getMySharedLists(onResult)
    }

    fun removeSharedList(code: String, onResult: (Boolean) -> Unit) {
        repository.removeSharedList(code, onResult)
    }
}

sealed class ImportState {
    object Idle : ImportState()
    data class InProgress(val done: Int, val total: Int) : ImportState()
    data class Completed(val result: com.kaan.watchlist.data.repository.ImportResult) : ImportState()
}

class MediaViewModelFactory(private val repository: MediaRepository, private val traktRepository: com.kaan.watchlist.data.repository.TraktRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(repository, traktRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
