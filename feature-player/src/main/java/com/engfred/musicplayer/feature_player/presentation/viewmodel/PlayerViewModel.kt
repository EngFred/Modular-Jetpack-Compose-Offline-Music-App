package com.engfred.musicplayer.feature_player.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.repository.PlayerRepository
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

object PlayerArgs {
    const val AUDIO_FILE_URI = "audioFileUri"
}

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()
    private var isInitialLoadComplete = false

    init {
        viewModelScope.launch {
            playerRepository.getPlaybackState().onEach { playbackState ->
                if (isInitialLoadComplete) {
                    // Ignore isLoading updates after initial load, except for new song playback
                    _uiState.update { currentState ->
                        playbackState.copy(
                            isLoading = if (playbackState.currentAudioFile != currentState.currentAudioFile) {
                                playbackState.isLoading
                            } else {
                                false
                            }
                        )
                    }
                } else {
                    _uiState.update { playbackState }
                    if (!playbackState.isLoading && playbackState.currentAudioFile != null) {
                        isInitialLoadComplete = true
                    }
                }
            }.launchIn(this)
        }

        if (permissionHandlerUseCase.hasAudioPermission()) {
            savedStateHandle.get<String>(PlayerArgs.AUDIO_FILE_URI)?.let { uriString ->
                val initialAudioFileUri = Uri.decode(uriString).toUri()
                viewModelScope.launch {
                    // Only initiate playback if the URI is different from the currently playing song
                    if (initialAudioFileUri != _uiState.value.currentAudioFile?.uri) {
                        try {
                            playerRepository.initiatePlayback(initialAudioFileUri)
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    error = "Failed to initiate playback: ${e.message}",
                                    isLoading = false
                                )
                            }
                            isInitialLoadComplete = true
                            Log.e("PlayerViewModel", "Playback initiation failed: ${e.message}", e)
                        }
                    } else {
                        Log.d("PlayerViewModel", "Song already playing, skipping initiatePlayback for URI: $initialAudioFileUri")
                    }
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        error = "No audio file URI provided to play.",
                        isLoading = false
                    )
                }
                isInitialLoadComplete = true
                Log.w("PlayerViewModel", "No audio file URI provided via navigation.")
            }
        } else {
            _uiState.update {
                it.copy(
                    error = "Storage permission required to play audio files.",
                    isLoading = false
                )
            }
            isInitialLoadComplete = true
            Log.w("PlayerViewModel", "Permissions not granted, skipping initialization")
        }
    }

    fun onEvent(event: PlayerEvent) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                when (event) {
                    is PlayerEvent.PlayAudioFile -> {
                        if (permissionHandlerUseCase.hasAudioPermission()) {
                            // Reset isInitialLoadComplete for new song playback
                            isInitialLoadComplete = false
                            playerRepository.initiatePlayback(event.audioFile.uri)
                            Log.d("PlayerViewModel", "PlayAudioFile event: Initiating playback for ${event.audioFile.title}")
                        } else {
                            _uiState.update {
                                it.copy(
                                    error = "Storage permission required to play audio.",
                                    isLoading = false
                                )
                            }
                            isInitialLoadComplete = true
                        }
                    }
                    PlayerEvent.PlayPause -> {
                        playerRepository.playPause()
                    }
                    PlayerEvent.SkipToNext -> {
                        playerRepository.skipToNext()
                    }
                    PlayerEvent.SkipToPrevious -> {
                        playerRepository.skipToPrevious()
                    }
                    is PlayerEvent.SeekTo -> {
                        playerRepository.seekTo(event.positionMs)
                    }
                    is PlayerEvent.SetRepeatMode -> {
                        playerRepository.setRepeatMode(event.mode)
                    }
                    is PlayerEvent.SetShuffleMode -> {
                        playerRepository.setShuffleMode(event.mode)
                    }
                    PlayerEvent.ReleasePlayer -> {
                        playerRepository.releasePlayer()
                        isInitialLoadComplete = true
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
                isInitialLoadComplete = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            playerRepository.releasePlayer()
            Log.d("PlayerViewModel", "PlayerRepository.releasePlayer() called from onCleared")
        }
    }
}