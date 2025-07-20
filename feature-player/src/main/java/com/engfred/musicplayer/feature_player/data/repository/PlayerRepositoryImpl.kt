package com.engfred.musicplayer.feature_player.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_player.data.service.AudioFileMapper
import com.engfred.musicplayer.feature_player.data.service.MusicService
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import com.engfred.musicplayer.feature_player.domain.repository.PlayerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of PlayerRepository that interacts with the MusicService
 * via a MediaController to control playback.
 */
@OptIn(UnstableApi::class)
@UnstableApi
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val audioFileMapper: AudioFileMapper
) : PlayerRepository {

    private var mediaController: MediaController? = null
    private val _currentPlaybackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _currentPlaybackState.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // allCachedAudioFiles is now observed from SharedAudioDataSource
    // private var allCachedAudioFiles: List<AudioFile> = emptyList() // REMOVE THIS

    init {
        repositoryScope.launch {
            connectToMediaService()
            // Observe the shared data source for all audio files
            // The initial value might be empty, and it will update when LibraryViewModel loads them.
            sharedAudioDataSource.allAudioFiles
                .onEach { files ->
                    // This will update the internal state if needed, but we mostly just rely on it
                    // being available when initiatePlayback is called.
                    Log.d("PlayerRepositoryImpl", "Received ${files.size} files from SharedAudioDataSource.")
                }.launchIn(repositoryScope)
        }
    }

    private suspend fun connectToMediaService() {
        val startTime = System.currentTimeMillis()
        Log.d("PlayerRepositoryImpl", "Connecting to MediaSessionService...")

        try {
            // 1. Start MusicService
            val serviceIntent = Intent(context, MusicService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d("PlayerRepositoryImpl", "MusicService start requested")

            // 2. Build SessionToken
            val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
            val timeoutMs = 15000L
            val pollIntervalMs = 500L
            val maxAttempts = (timeoutMs / pollIntervalMs).toInt()

            // 3. Retry building MediaController until the service is ready
            repeat(maxAttempts) { attempt ->
                try {
                    val controller = MediaController.Builder(context, sessionToken)
                        .buildAsync()
                        .get(3, TimeUnit.SECONDS) // Short timeout per attempt

                    if (controller != null) {
                        mediaController = controller
                        controller.addListener(ControllerCallback())
                        Log.d("PlayerRepositoryImpl", "MediaController connected on attempt ${attempt + 1} in ${System.currentTimeMillis() - startTime}ms")

                        withContext(Dispatchers.Main) {
                            updatePlaybackState()
                        }
                        return // ✅ Success — exit function
                    }
                } catch (e: Exception) {
                    Log.w("PlayerRepositoryImpl", "MediaController not available yet (attempt ${attempt + 1}): ${e.message}")
                    delay(pollIntervalMs)
                }
            }

            // 4. All attempts failed
            Log.e("PlayerRepositoryImpl", "Failed to connect MediaController after ${timeoutMs}ms.")
            withContext(Dispatchers.Main) {
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = "Failed to connect to player service."
                )
            }
        } catch (e: Exception) {
            Log.e("PlayerRepositoryImpl", "MediaService connection failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = "Player connection error: ${e.message}"
                )
            }
        }
    }

    // UPDATED: initiatePlayback now uses sharedAudioDataSource
    override suspend fun initiatePlayback(initialAudioFileUri: Uri) {
        withContext(Dispatchers.Main) {
            if (mediaController == null) {
                Log.e("PlayerRepositoryImpl", "MediaController not available for playback initiation.")
                _currentPlaybackState.value = _currentPlaybackState.value.copy(error = "Player not initialized.")
                return@withContext
            }

            val allAudioFiles = sharedAudioDataSource.allAudioFiles.value // GET LATEST LIST FROM SHARED SOURCE
            if (allAudioFiles.isEmpty()) {
                Log.w("PlayerRepositoryImpl", "Shared audio files are empty. Cannot initiate playback.")
                _currentPlaybackState.value = _currentPlaybackState.value.copy(error = "No audio files available to play.")
                return@withContext
            }

            val allAudioFileUris = allAudioFiles.map { it.uri }
            val startIndex = allAudioFileUris.indexOf(initialAudioFileUri).coerceAtLeast(0)

            try {
                mediaController?.setMediaItems(allAudioFileUris.map { MediaItem.Builder().setUri(it).build() }, startIndex, C.TIME_UNSET)
                mediaController?.prepare()
                mediaController?.play()
                Log.d("PlayerRepositoryImpl", "Initiated playback for: $initialAudioFileUri")
            } catch (e: Exception) {
                Log.e("PlayerRepositoryImpl", "Error setting media items or playing during initiation: ${e.message}", e)
                _currentPlaybackState.value = _currentPlaybackState.value.copy(error = "Playback error: ${e.message}")
            }
        }
    }

    override suspend fun setMediaItems(audioFileUris: List<Uri>, startIndex: Int) {
        withContext(Dispatchers.Main) {
            mediaController?.let { controller ->
                try {
                    val mediaItems = audioFileUris.map { uri -> MediaItem.Builder().setUri(uri).build() }
                    controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                    controller.prepare()
                    Log.d("PlayerRepositoryImpl", "Set ${mediaItems.size} media items, startIndex: $startIndex")
                } catch (e: Exception) {
                    Log.e("PlayerRepositoryImpl", "Failed to set media items: ${e.message}", e)
                    _currentPlaybackState.value = _currentPlaybackState.value.copy(
                        error = "Failed to set media items: ${e.message}"
                    )
                }
            } ?: run {
                Log.w("PlayerRepositoryImpl", "MediaController not set when trying to set media items.")
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = "Player not initialized. Cannot set media items."
                )
            }
        }
    }

    override suspend fun addMediaItems(audioFileUris: List<Uri>) {
        withContext(Dispatchers.Main) {
            mediaController?.let { controller ->
                try {
                    val mediaItems = audioFileUris.map { uri -> MediaItem.Builder().setUri(uri).build() }
                    controller.addMediaItems(mediaItems)
                    Log.d("PlayerRepositoryImpl", "Added ${mediaItems.size} media items to playlist")
                } catch (e: Exception) {
                    Log.e("PlayerRepositoryImpl", "Failed to add media items: ${e.message}", e)
                    _currentPlaybackState.value = _currentPlaybackState.value.copy(
                        error = "Failed to add media items: ${e.message}"
                    )
                }
            } ?: run {
                Log.w("PlayerRepositoryImpl", "MediaController not set when trying to add media items.")
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = "Player not initialized. Cannot add media items."
                )
            }
        }
    }

    override suspend fun play() {
        withContext(Dispatchers.Main) {
            mediaController?.play() ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to play.")
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            mediaController?.pause() ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to pause.")
        }
    }

    override suspend fun playPause() {
        withContext(Dispatchers.Main) {
            mediaController?.run {
                if (isPlaying) pause() else play()
            } ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to play/pause.")
        }
    }

    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            mediaController?.seekToNextMediaItem() ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to skip next.")
        }
    }

    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            mediaController?.seekToPreviousMediaItem() ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to skip previous.")
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            mediaController?.seekTo(positionMs) ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to seek.")
        }
    }

    override suspend fun seekToItem(index: Int) {
        withContext(Dispatchers.Main) {
            mediaController?.seekTo(index, C.TIME_UNSET) ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to seek to item.")
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            mediaController?.let { controller ->
                controller.repeatMode = when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            } ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to set repeat mode.")
        }
    }

    override suspend fun setShuffleMode(mode: ShuffleMode) {
        withContext(Dispatchers.Main) {
            mediaController?.let { controller ->
                controller.shuffleModeEnabled = (mode == ShuffleMode.ON)
            } ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to set shuffle mode.")
        }
    }

    override suspend fun releasePlayer() {
        repositoryScope.launch {
            withContext(Dispatchers.Main) {
                mediaController?.removeListener(ControllerCallback())
                mediaController?.release()
                mediaController = null
                Log.d("PlayerRepositoryImpl", "MediaController released by repository.")
            }
            repositoryScope.cancel()
        }
    }

    @OptIn(UnstableApi::class)
    private inner class ControllerCallback : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_PLAYER_ERROR
                )
            ) {
                repositoryScope.launch(Dispatchers.Main) {
                    updatePlaybackState()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            repositoryScope.launch(Dispatchers.Main) {
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = error.message ?: "Player error"
                )
                Log.e("PlayerRepositoryImpl", "Playback error from ExoPlayer: ${error.message}", error)
            }
        }
    }

    private fun updatePlaybackState() {
        mediaController?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val currentAudioFile = currentMediaItem?.let { audioFileMapper.mapMediaItemToAudioFile(it) }

            _currentPlaybackState.value = PlaybackState(
                currentAudioFile = currentAudioFile,
                isPlaying = controller.isPlaying,
                playbackPositionMs = controller.currentPosition,
                totalDurationMs = controller.duration.coerceAtLeast(0L),
                bufferedPositionMs = controller.bufferedPosition,
                repeatMode = when (controller.repeatMode) {
                    Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                },
                shuffleMode = if (controller.shuffleModeEnabled) ShuffleMode.ON else ShuffleMode.OFF,
                playbackSpeed = controller.playbackParameters.speed,
                error = _currentPlaybackState.value.error,
                isLoading = controller.playbackState == Player.STATE_BUFFERING
            )
        }
    }
}