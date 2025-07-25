package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.repository.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(FavoritesScreenState())
    val uiState: StateFlow<FavoritesScreenState> = _uiState.asStateFlow()

    init {
        loadFavoriteAudioFiles()
        startObservingPlaybackState()
    }

    private fun startObservingPlaybackState() {
        playerController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                if (state.currentAudioFile != null && state.isPlaying) {
                    currentState.copy(
                        currentPlayingId = state.currentAudioFile!!.id,
                        isPlaying = true
                    )
                } else if (!state.isPlaying) {
                    // If paused or stopped, clear current playing ID if it's the same song
                    if (currentState.currentPlayingId == state.currentAudioFile?.id) {
                        currentState.copy(
                            currentPlayingId = null,
                            isPlaying = false
                        )
                    } else {
                        currentState
                    }
                } else {
                    currentState
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: FavoritesEvent) {
        viewModelScope.launch {
            when (event) {
                is FavoritesEvent.RemoveFavorite -> {
                    try {
                        favoritesRepository.removeFavoriteAudioFile(event.audioFileId)
                        Log.d("FavoritesViewModel", "Removed favorite audio file ID: ${event.audioFileId}")
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Error removing favorite: ${e.message}") }
                        Log.e("FavoritesViewModel", "Error removing favorite: ${e.message}", e)
                    }
                }
                is FavoritesEvent.PlayAudio -> {
                    startAudioPlayback(event.audioFile)
                }

                is FavoritesEvent.SwipedLeft -> {
                    startAudioPlayback(event.audioFile)
                }
                is FavoritesEvent.SwipedRight -> {
                    //pause only if the playback has an audio and it is playing
                    if (_uiState.value.currentPlayingId == event.audioFile.id && _uiState.value.isPlaying) {
                        playerController.playPause()
                    }
                }
            }
        }
    }

    private suspend fun startAudioPlayback(audioFile: AudioFile) {
        val audioFiles =  uiState.value.favoriteAudioFiles
        sharedAudioDataSource.setPlayingQueue(audioFiles)
        playerController.initiatePlayback(audioFile.uri)
    }

    private fun loadFavoriteAudioFiles() {
        _uiState.update { it.copy(isLoading = true, error = null) }
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
    data class PlayAudio(val audioFile: AudioFile) : FavoritesEvent()
    data class SwipedLeft(val audioFile: AudioFile) : FavoritesEvent()
    data class SwipedRight(val audioFile: AudioFile) : FavoritesEvent()
}

data class FavoritesScreenState(
    val favoriteAudioFiles: List<AudioFile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentPlayingId: Long? = null,
    val isPlaying: Boolean = false
)