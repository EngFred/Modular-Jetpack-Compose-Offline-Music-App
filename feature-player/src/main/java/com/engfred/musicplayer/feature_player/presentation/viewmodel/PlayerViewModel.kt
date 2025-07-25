package com.engfred.musicplayer.feature_player.presentation.viewmodel

import android.util.Log
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
import com.engfred.musicplayer.core.domain.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.PlayerController
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    // Initial default value. This will be immediately overwritten by setInitialPlayerLayout
    private val _playerLayoutState = MutableStateFlow<PlayerLayout?>(null)
    val playerLayoutState: StateFlow<PlayerLayout?> = _playerLayoutState.asStateFlow()

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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Use .first() to get the current value and then immediately cancel collection
                val savedLayout = settingsRepository.getAppSettings().first().selectedPlayerLayout
                _playerLayoutState.value = savedLayout
                Log.d("PlayerViewModel", "Player Layout initialized from settings: $savedLayout")
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to load player layout from settings: ${e.message}", e)
                // Fallback to default if loading fails
                _playerLayoutState.value = PlayerLayout.ETHEREAL_FLOW
            }
        }
    }

    fun onEvent(event: PlayerEvent) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                when (event) {
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
                    }
                    is PlayerEvent.RemoveFromFavorites -> {
                        favoritesRepository.removeFavoriteAudioFile(event.audioFileId)
                        _uiState.update { it.copy(isFavorite = false) }
                    }
                    is PlayerEvent.SetSeeking -> {
                        _uiState.update { it.copy(isSeeking = event.seeking) }
                    }
                    is PlayerEvent.PlayAudioFile -> {
                        playerController.initiatePlayback(event.audioFile.uri)
                    }
                    is PlayerEvent.SelectPlayerLayout -> {
                        _playerLayoutState.value = event.layout
                        settingsRepository.updatePlayerLayout(event.layout)
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
}