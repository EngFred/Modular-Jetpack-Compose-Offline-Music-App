package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow // Import
import kotlinx.coroutines.flow.StateFlow // Import
import kotlinx.coroutines.flow.asStateFlow // Import
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update // Import
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Favorites screen.
 * Manages the state of favorite audio files and interactions with the FavoritesRepository.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playerController: PlayerController
) : ViewModel() {

    // Change from 'var uiState by mutableStateOf' to MutableStateFlow
    private val _uiState = MutableStateFlow(FavoritesScreenState())
    val uiState: StateFlow<FavoritesScreenState> = _uiState.asStateFlow()

    init {
        // Load favorite audio files
        loadFavoriteAudioFiles()
    }

    fun onEvent(event: FavoritesEvent) {
        viewModelScope.launch {
            when (event) {
                is FavoritesEvent.RemoveFavorite -> {
                    try {
                        favoritesRepository.removeFavoriteAudioFile(event.audioFileId)
                        Log.d("FavoritesViewModel", "Removed favorite audio file ID: ${event.audioFileId}")
                        // No need to manually update uiState here as it's observed from the repository flow
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Error removing favorite: ${e.message}") }
                        Log.e("FavoritesViewModel", "Error removing favorite: ${e.message}", e)
                    }
                }
                is FavoritesEvent.OnAudioFileClick -> {
                    Log.d("FavoritesViewModel", "Clicked on audio file: ${event.audioFile.title}")
                    if (uiState.value.favoriteAudioFiles.isNotEmpty()) { // Access value for StateFlow
                        // Update SharedAudioDataSource with favorite songs for playback queue
                        val favoriteSongs = uiState.value.favoriteAudioFiles // Access value for StateFlow
                        sharedAudioDataSource.clearAudioFiles()
                        sharedAudioDataSource.setAudioFiles(favoriteSongs)
                        Log.d("FavoritesViewModel", "Set playback queue to ${favoriteSongs.size} favorite songs.")
                        playerController.initiatePlayback(event.audioFile.uri)
                    } else {
                        Log.d("FavoritesViewModel", "No favorite audio files to play.")
                    }
                }
            }
        }
    }

    private fun loadFavoriteAudioFiles() {
        _uiState.update { it.copy(isLoading = true, error = null) } // Set loading state
        favoritesRepository.getFavoriteAudioFiles().onEach { favoriteAudioFiles ->
            _uiState.update {
                it.copy(
                    favoriteAudioFiles = favoriteAudioFiles,
                    isLoading = false,
                    error = null
                )
            }
            Log.d("FavoritesViewModel", "Loaded ${favoriteAudioFiles.size} favorite audio files.")
        }.launchIn(viewModelScope)
    }
}

sealed class FavoritesEvent {
    data class RemoveFavorite(val audioFileId: Long) : FavoritesEvent()
    data class OnAudioFileClick(val audioFile: AudioFile) : FavoritesEvent()
}

// Ensure this data class is in a separate file or within the same file if you prefer
// package com.engfred.musicplayer.feature_favorites.presentation.viewmodel
data class FavoritesScreenState(
    val favoriteAudioFiles: List<AudioFile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)