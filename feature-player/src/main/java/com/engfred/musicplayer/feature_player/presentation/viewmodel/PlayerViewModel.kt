package com.engfred.musicplayer.feature_player.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.feature_player.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    var uiState by mutableStateOf(PlayerScreenState())
        private set

    // The MediaController should ideally be managed by the PlayerRepository itself,
    // not directly by the ViewModel. The ViewModel interacts with the Repository.
    // REMOVE: private var mediaController: MediaController? = null

    init {
        // Observer for playback state from the repository. This should be initialized early.
        // It's crucial to observe the repository's state as soon as the ViewModel is created.
        playerRepository.getPlaybackState().onEach { playbackState ->
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    playbackState = playbackState,
                    error = playbackState.error ?: uiState.error,
                    isLoading = playbackState.isLoading // Keep this tied to player's buffering state
                )
            }
        }.launchIn(viewModelScope)


        if (permissionHandlerUseCase.hasAudioPermission()) {
            savedStateHandle.get<String>(PlayerArgs.AUDIO_FILE_URI)?.let { uriString ->
                val initialAudioFileUri = Uri.decode(uriString).toUri()
                // Directly initiate playback through the repository
                // The repository is now responsible for getting the full list of files
                // from SharedAudioDataSource and setting up the MediaController.
                viewModelScope.launch {
                    playerRepository.initiatePlayback(initialAudioFileUri)
                }
            } ?: run {
                uiState = uiState.copy(
                    error = "No audio file URI provided to play.",
                    isLoading = false
                )
                Log.w("PlayerViewModel", "No audio file URI provided via navigation.")
            }
        } else {
            uiState = uiState.copy(
                error = "Storage permission required to play audio files.",
                isLoading = false
            )
            Log.w("PlayerViewModel", "Permissions not granted, skipping initialization")
        }
    }


    // REMOVED: initializePlayer function. Its logic is now primarily handled by PlayerRepository.initiatePlayback()
    // and the initial setup in the init block.
    // REMOVED: createMediaController function. This is now managed by PlayerRepositoryImpl internally.

    fun onEvent(event: PlayerEvent) {
        // Launch on Main, as all these repository calls are now suspend and internally
        // handle thread switching if needed (as per our previous discussion for PlayerRepositoryImpl).
        viewModelScope.launch(Dispatchers.Main) {
            try {
                when (event) {
                    is PlayerEvent.PlayAudioFile -> {
                        if (permissionHandlerUseCase.hasAudioPermission()) {
                            // If this event means "play a new individual song",
                            // you'd typically need to find its index in the full list
                            // and then setMediaItems.
                            // The playerRepository.initiatePlayback handles setting up the full playlist
                            // and starting playback from a specific URI.
                            playerRepository.initiatePlayback(event.audioFile.uri)
                            Log.d("PlayerViewModel", "PlayAudioFile event: Initiating playback for ${event.audioFile.title}")
                        } else {
                            uiState = uiState.copy(error = "Storage permission required to play audio.")
                        }
                    }
                    PlayerEvent.PlayPause -> {
                        // The playerRepository.playPause() handles the logic internally
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
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Event handling failed: ${e.message}", e)
                uiState = uiState.copy(error = "Action failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // The PlayerRepository is responsible for releasing the MediaController.
        // We call releasePlayer() in PlayerEvent.ReleasePlayer if explicitly needed by UI,
        // but here it's good to ensure it's released on ViewModel clear.
        viewModelScope.launch {
            playerRepository.releasePlayer()
            Log.d("PlayerViewModel", "PlayerRepository.releasePlayer() called from onCleared")
        }
    }
}