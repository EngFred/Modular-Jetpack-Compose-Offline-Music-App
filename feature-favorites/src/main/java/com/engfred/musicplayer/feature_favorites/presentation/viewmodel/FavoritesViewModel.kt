package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    var uiState by mutableStateOf(FavoritesScreenState())
        private set

    init {
        // Load favorite audio files
        loadFavoriteAudioFiles()
    }

    fun onEvent(event: FavoritesEvent) {
        viewModelScope.launch {
            when (event) {
                is FavoritesEvent.RemoveFavorite -> {
                    favoritesRepository.removeFavoriteAudioFile(event.audioFileId)
                    Log.d("FavoritesViewModel", "Removed favorite audio file ID: ${event.audioFileId}")
                }
                is FavoritesEvent.OnAudioFileClick -> {
                    Log.d("FavoritesViewModel", "Clicked on audio file: ${event.audioFile.title}")
                    if (uiState.favoriteAudioFiles.isNotEmpty()) {
                        // Update SharedAudioDataSource with favorite songs for playback queue
                        val favoriteSongs = uiState.favoriteAudioFiles
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
        favoritesRepository.getFavoriteAudioFiles().onEach { favoriteAudioFiles ->
            uiState = uiState.copy(
                favoriteAudioFiles = favoriteAudioFiles,
                isLoading = false,
                error = null
            )
            Log.d("FavoritesViewModel", "Loaded ${favoriteAudioFiles.size} favorite audio files.")
        }.launchIn(viewModelScope)
    }
}

sealed class FavoritesEvent {
    data class RemoveFavorite(val audioFileId: Long) : FavoritesEvent()
    data class OnAudioFileClick(val audioFile: AudioFile) : FavoritesEvent()
}

data class FavoritesScreenState(
    val favoriteAudioFiles: List<AudioFile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)