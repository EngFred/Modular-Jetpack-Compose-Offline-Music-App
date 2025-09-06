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
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    private val _playerLayoutState = MutableStateFlow<PlayerLayout?>(null)
    val playerLayoutState: StateFlow<PlayerLayout?> = _playerLayoutState.asStateFlow()

    init {
        viewModelScope.launch {
            playbackController.getPlaybackState().onEach { playbackState ->
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
                        // ---------------------------------------------
                        // NEW: Remove reliance on local _repeatMode and _shuffleMode, using PlaybackState directly
                        // since PlaybackControllerImpl now reliably sets these from MainActivity.
                        // ---------------------------------------------
                    )
                }
            }.launchIn(this)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Use .first() to get the current value and then immediately cancel collection
                val appSettings = settingsRepository.getAppSettings().first()
                _playerLayoutState.value = appSettings.selectedPlayerLayout
                Log.d("PlayerViewModel", "Player Layout initialized from settings: ${appSettings.selectedPlayerLayout}")
                // ---------------------------------------------
                // NEW: Remove initialization of _repeatMode and _shuffleMode from settingsRepository
                // since PlaybackControllerImpl already handles this via MainActivity calls.
                // ---------------------------------------------
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to load player settings from settings: ${e.message}", e)
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
                        playbackController.playPause()
                    }
                    PlayerEvent.SkipToNext -> {
                        playbackController.skipToNext()
                    }
                    PlayerEvent.SkipToPrevious -> {
                        playbackController.skipToPrevious()
                    }
                    is PlayerEvent.SeekTo -> {
                        playbackController.seekTo(event.positionMs)
                    }
                    is PlayerEvent.SetRepeatMode -> {
                        playbackController.setRepeatMode(event.mode)
                        settingsRepository.updateRepeatMode(event.mode)
                        // ---------------------------------------------
                        // NEW: Remove update to _repeatMode since it's no longer used; PlaybackState.repeatMode
                        // is updated by PlaybackControllerImpl and reflected in uiState.
                        // ---------------------------------------------
                        Log.d("PlayerViewModel", "Repeat mode set to ${event.mode}")
                    }
                    is PlayerEvent.SetShuffleMode -> {
                        playbackController.setShuffleMode(event.mode)
                        settingsRepository.updateShuffleMode(event.mode)
                        // ---------------------------------------------
                        // NEW: Remove update to _shuffleMode since it's no longer used; PlaybackState.shuffleMode
                        // is updated by PlaybackControllerImpl and reflected in uiState.
                        // ---------------------------------------------
                        Log.d("PlayerViewModel", "Shuffle mode set to ${event.mode}")
                    }
                    PlayerEvent.ReleasePlayer -> {
                        playbackController.releasePlayer()
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
                        playbackController.initiatePlayback(event.audioFile.uri)
                    }
                    is PlayerEvent.SelectPlayerLayout -> {
                        _playerLayoutState.value = event.layout
                        settingsRepository.updatePlayerLayout(event.layout)
                    }
                    is PlayerEvent.RemovedFromQueue -> {
                        playbackController.removeFromQueue(event.audioFile)
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