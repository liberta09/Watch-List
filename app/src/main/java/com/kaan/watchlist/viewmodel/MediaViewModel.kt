package com.kaan.watchlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaan.watchlist.data.repository.MediaRepository
import com.kaan.watchlist.domain.model.Announcement
import com.kaan.watchlist.domain.model.MediaItem
import kotlinx.coroutines.Job
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

    private val _isDiscoverLoading = MutableStateFlow(true)
    val isDiscoverLoading = _isDiscoverLoading.asStateFlow()

    private val _discoverError = MutableStateFlow<String?>(null)
    val discoverError = _discoverError.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    val myList = repository.myList
    val favorites = repository.favorites
    val notes = repository.notes
    val notificationsEnabled = repository.notificationsEnabled
    
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
            try {
                val movies = repository.getPopularMovies()
                val tvShows = repository.getPopularTvShows()
                
                if (movies.isEmpty() && tvShows.isEmpty()) {
                    _discoverError.value = "İçerikler yüklenemedi. Lütfen internet bağlantınızı kontrol edin."
                } else {
                    _popularMovies.value = movies
                    _popularTvShows.value = tvShows
                }
            } catch (e: Exception) {
                _discoverError.value = "Bir hata oluştu: ${e.localizedMessage}"
            } finally {
                _isDiscoverLoading.value = false
            }
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
                _searchResults.value = results
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

    fun toggleFavorite(item: MediaItem) {
        repository.toggleFavorite(item)
    }

    fun toggleList(item: MediaItem) {
        repository.toggleList(item)
    }

    fun toggleWatchedStatus(item: MediaItem) {
        repository.toggleWatchedStatus(item)
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
