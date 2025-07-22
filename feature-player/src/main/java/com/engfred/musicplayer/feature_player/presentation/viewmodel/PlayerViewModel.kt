package com.engfred.musicplayer.feature_player.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.domain.model.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState // Correct import
import com.engfred.musicplayer.core.domain.model.repository.PlayerController

object PlayerArgs {
    const val AUDIO_FILE_URI = "audioFileUri"
}

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerController.getPlaybackState().onEach { playbackState ->
                _uiState.update { currentState ->
                    val isFavorite = if (playbackState.currentAudioFile != null) {
                        favoritesRepository.isFavorite(playbackState.currentAudioFile!!.id)
                    } else {
                        false
                    }
                    playbackState.copy(
                        isLoading = if (playbackState.currentAudioFile != currentState.currentAudioFile) {
                            playbackState.isLoading
                        } else {
                            currentState.isLoading
                        },
                        isFavorite = isFavorite,
                        isSeeking = currentState.isSeeking
                    )
                }
            }.launchIn(this)
        }

        savedStateHandle.get<String>(PlayerArgs.AUDIO_FILE_URI)?.let { uriString ->
            val initialAudioFileUri = Uri.decode(uriString).toUri()
            viewModelScope.launch {
                // Skip initiatePlayback only if fromMiniPlayer=true and URI matches
                if (_uiState.value.currentAudioFile == null) {
                    try {
                        playerController.initiatePlayback(initialAudioFileUri)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = "Failed to initiate playback: ${e.message}",
                                isLoading = false
                            )
                        }
                        Log.e("PlayerViewModel", "Playback initiation failed: ${e.message}", e)
                    }
                } else {
                    Log.d("PlayerViewModel", "Song already playing, skipping initiatePlayback for URI: $initialAudioFileUri from MiniPlayer")
                }
            }
        } ?: run {
            // If no URI is provided, rely on existing playback state
            if (_uiState.value.currentAudioFile == null) {
                _uiState.update {
                    it.copy(
                        error = "No audio file URI provided to play.",
                        isLoading = false
                    )
                }
                Log.w("PlayerViewModel", "No audio file URI provided via navigation.")
            } else {
                Log.d("PlayerViewModel", "No URI provided, continuing with current song: ${_uiState.value.currentAudioFile?.title}")
            }
        }
    }
    fun onEvent(event: PlayerEvent) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                when (event) {
                    is PlayerEvent.PlayAudioFile -> {
                        val fromMiniPlayer = event.fromMiniPlayer ?: false
                        if (!fromMiniPlayer) {
                            playerController.initiatePlayback(event.audioFile.uri)
                            Log.d(
                                "PlayerViewModel",
                                "PlayAudioFile event: Initiating playback for ${event.audioFile.title}, fromMiniPlayer: $fromMiniPlayer"
                            )
                        } else {
                            Log.d(
                                "PlayerViewModel",
                                "PlayAudioFile event: Song already playing, skipping for ${event.audioFile.title}"
                            )
                        }
                    }
                    PlayerEvent.PlayPause -> {
                        playerController.playPause()
                    }
                    PlayerEvent.SkipToNext -> {
                        playerController.skipToNext()
                    }
                    PlayerEvent.SkipToPrevious -> {
                        playerController.skipToPrevious()
                    }
                    is PlayerEvent.SeekTo -> {
                        playerController.seekTo(event.positionMs)
                    }
                    is PlayerEvent.SetRepeatMode -> {
                        playerController.setRepeatMode(event.mode)
                    }
                    is PlayerEvent.SetShuffleMode -> {
                        playerController.setShuffleMode(event.mode)
                    }
                    PlayerEvent.ReleasePlayer -> {
                        playerController.releasePlayer()
                    }
                    is PlayerEvent.AddToFavorites -> {
                        favoritesRepository.addFavoriteAudioFile(event.audioFile)
                        _uiState.update { it.copy(isFavorite = true) }
                        Log.d("PlayerViewModel", "Added to favorites: ${event.audioFile.title}")
                    }
                    is PlayerEvent.RemoveFromFavorites -> {
                        favoritesRepository.removeFavoriteAudioFile(event.audioFileId)
                        _uiState.update { it.copy(isFavorite = false) }
                        Log.d("PlayerViewModel", "Removed from favorites: ID ${event.audioFileId}")
                    }
                    is PlayerEvent.SetSeeking -> { // <--- NEW: Handle SetSeeking event
                        _uiState.update { it.copy(isSeeking = event.seeking) }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Event handling failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Event handling failed: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            playerController.releasePlayer()
            Log.d("PlayerViewModel", "PlayerRepository.releasePlayer() called from onCleared")
        }
    }
}