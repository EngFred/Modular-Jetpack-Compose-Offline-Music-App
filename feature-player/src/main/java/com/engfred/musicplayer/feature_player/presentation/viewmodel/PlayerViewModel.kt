package com.engfred.musicplayer.feature_player.presentation.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.feature_library.domain.usecases.GetAllAudioFilesUseCase
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.feature_player.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.feature_player.data.service.MusicService
import java.util.concurrent.TimeUnit


object PlayerArgs {
    const val AUDIO_FILE_URI = "audioFileUri"
}


@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val getAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var uiState by mutableStateOf(PlayerScreenState())
        private set

    private var mediaController: MediaController? = null

    init {
        if (permissionHandlerUseCase.hasAudioPermission()) {
            savedStateHandle.get<String>(PlayerArgs.AUDIO_FILE_URI)?.let { uriString ->
                val initialAudioFileUri = Uri.decode(uriString).toUri()
                initializePlayer(initialAudioFileUri)
            }
        } else {
            uiState = uiState.copy(
                error = "Storage permission required to play audio files.",
                isLoading = false
            )
            Log.w("PlayerViewModel", "Permissions not granted, skipping initialization")
        }
    }

    private fun initializePlayer(initialAudioFileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            Log.d("PlayerViewModel", "initializePlayer started")

            try {
                // Start MusicService
                val serviceIntent = Intent(context, MusicService::class.java)
                context.startService(serviceIntent)

                // Create MediaController with retry
                val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
                mediaController = createMediaController(sessionToken)
                mediaController?.let { playerRepository.setMediaController(it) }

                if (mediaController == null) {
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = "Failed to initialize media controller."
                        )
                    }
                    return@launch
                }

                uiState = uiState.copy(isLoading = true)
                val allAudioFilesResource = getAudioFilesUseCase().firstOrNull()
                when (allAudioFilesResource) {
                    is Resource.Success -> {
                        val allAudioFiles = allAudioFilesResource.data ?: emptyList()
                        if (allAudioFiles.isNotEmpty()) {
                            val startIndex = allAudioFiles.indexOfFirst { it.uri == initialAudioFileUri }
                                .coerceAtLeast(0)
                            withContext(Dispatchers.Main) {
                                playerRepository.setMediaItems(listOf(allAudioFiles[startIndex].uri), 0)
                                playerRepository.play()
                                Log.d("PlayerViewModel", "Initial file set: ${System.currentTimeMillis() - startTime}ms")
                            }
                            if (allAudioFiles.size > 1) {
                                val remainingUris = allAudioFiles.map { it.uri }
                                    .filterIndexed { index, _ -> index != startIndex }
                                playerRepository.addMediaItems(remainingUris)
                                Log.d("PlayerViewModel", "Remaining files added: ${System.currentTimeMillis() - startTime}ms")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                uiState = uiState.copy(
                                    isLoading = false,
                                    error = "No audio files found to play."
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(
                                isLoading = false,
                                error = allAudioFilesResource.message ?: "Failed to load audio files."
                            )
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            uiState = uiState.copy(
                                isLoading = false,
                                error = "Failed to load audio files: Unknown error."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Initialization failed: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Failed to initialize player: ${e.message}"
                    )
                }
            }
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(isLoading = false)
                Log.d("PlayerViewModel", "initializePlayer completed: ${System.currentTimeMillis() - startTime}ms")
            }
        }

        playerRepository.getPlaybackState().onEach { playbackState ->
            uiState = uiState.copy(
                playbackState = playbackState,
                error = playbackState.error ?: uiState.error,
                isLoading = playbackState.isLoading
            )
        }.launchIn(viewModelScope)
    }

    private suspend fun createMediaController(sessionToken: SessionToken): MediaController? {
        return withContext(Dispatchers.IO) {
            try {
                val future = MediaController.Builder(context, sessionToken).buildAsync()
                future.get(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "MediaController creation failed: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
                // Retry once after a delay
                delay(1000)
                try {
                    val future = MediaController.Builder(context, sessionToken).buildAsync()
                    future.get(5, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "MediaController retry failed: ${e.message}")
                    // FirebaseCrashlytics.getInstance().recordException(e)
                    null
                }
            }
        }
    }

    fun onEvent(event: PlayerEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is PlayerEvent.PlayAudioFile -> {
                        if (permissionHandlerUseCase.hasAudioPermission()) {
                            // Ensure MusicService is running
                            context.startService(Intent(context, MusicService::class.java))
                            playerRepository.setMediaItems(listOf(event.audioFile.uri), 0)
                            playerRepository.play()
                        } else {
                            uiState = uiState.copy(error = "Storage permission required to play audio.")
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
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Event handling failed: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
                uiState = uiState.copy(error = "Action failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerRepository.releasePlayer() // Release MediaController, but keep MusicService running
    }
}