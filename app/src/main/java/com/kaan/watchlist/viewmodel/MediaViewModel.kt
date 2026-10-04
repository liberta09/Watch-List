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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.kaan.watchlist.data.repository.UpdateRepository
import com.kaan.watchlist.data.repository.UpdateStatus

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {

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
}

class MediaViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
