package com.engfred.musicplayer.feature_player.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.feature_player.data.service.MusicService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val audioFileMapper: AudioFileMapper
) : PlayerController {

    private var mediaController: MediaController? = null
    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repositoryScope.launch {
            connectToMediaService()
            startPlaybackPositionUpdates()
        }
    }

    private suspend fun connectToMediaService() {
        val startTime = System.currentTimeMillis()
        Log.d("PlayerRepositoryImpl", "Connecting to MediaSessionService...")

        try {
            val serviceIntent = Intent(context, MusicService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d("PlayerRepositoryImpl", "MusicService start requested")

            val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
            val timeoutMs = 15000L
            val pollIntervalMs = 500L
            val maxAttempts = (timeoutMs / pollIntervalMs).toInt()

            repeat(maxAttempts) { attempt ->
                try {
                    val controller = MediaController.Builder(context, sessionToken)
                        .buildAsync()
                        .get(3, TimeUnit.SECONDS)

                    if (controller != null) {
                        mediaController = controller
                        controller.addListener(ControllerCallback())
                        Log.d("PlayerRepositoryImpl", "MediaController connected on attempt ${attempt + 1} in ${System.currentTimeMillis() - startTime}ms")
                        withContext(Dispatchers.Main) {
                            updatePlaybackState()
                        }
                        return
                    }
                } catch (e: Exception) {
                    Log.w("PlayerRepositoryImpl", "MediaController not available yet (attempt ${attempt + 1}): ${e.message}")
                    delay(pollIntervalMs)
                }
            }

            Log.e("PlayerRepositoryImpl", "Failed to connect MediaController after ${timeoutMs}ms.")
            withContext(Dispatchers.Main) {
                _playbackState.update { it.copy(error = "Failed to connect to player service.") }
            }
        } catch (e: Exception) {
            Log.e("PlayerRepositoryImpl", "MediaService connection failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                _playbackState.update { it.copy(error = "Player connection error: ${e.message}") }
            }
        }
    }

    private suspend fun startPlaybackPositionUpdates() {
        withContext(Dispatchers.Main) {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.playbackState != Player.STATE_IDLE && controller.playbackState != Player.STATE_ENDED) {
                        updatePlaybackState()
                    }
                }
                delay(500)
            }
        }
    }

    override suspend fun initiatePlayback(initialAudioFileUri: Uri) {
        withContext(Dispatchers.Main) {
            if (mediaController == null) {
                Log.e("PlayerRepositoryImpl", "MediaController not available for playback initiation.")
                _playbackState.update { it.copy(error = "Player not initialized.") }
                return@withContext
            }

            val allAudioFiles = sharedAudioDataSource.allAudioFiles.value
            if (allAudioFiles.isEmpty()) {
                Log.w("PlayerRepositoryImpl", "Shared audio files are empty. Cannot initiate playback.")
                _playbackState.update { it.copy(error = "No audio files available to play.") }
                return@withContext
            }

            val startIndex = allAudioFiles.indexOfFirst { it.uri == initialAudioFileUri }.coerceAtLeast(0)
            try {
                val mediaItems = allAudioFiles.map { audioFileMapper.mapAudioFileToMediaItem(it) }
                mediaController?.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                mediaController?.prepare()
                mediaController?.play()
                Log.d("PlayerRepositoryImpl", "Initiated playback for: $initialAudioFileUri")
            } catch (e: Exception) {
                Log.e("PlayerRepositoryImpl", "Error setting media items or playing during initiation: ${e.message}", e)
                _playbackState.update { it.copy(error = "Playback error: ${e.message}") }
            }
        }
    }

//    override suspend fun setMediaItems(audioFileUris: List<Uri>, startIndex: Int) {
//        withContext(Dispatchers.Main) {
//            mediaController?.let { controller ->
//                try {
//                    val allAudioFiles = sharedAudioDataSource.allAudioFiles.value
//                    val mediaItems = audioFileUris.mapNotNull { uri ->
//                        allAudioFiles.find { it.uri == uri }?.let { audioFileMapper.mapAudioFileToMediaItem(it) }
//                    }
//                    controller.setMediaItems(mediaItems, startIndex.coerceAtLeast(0), C.TIME_UNSET)
//                    controller.prepare()
//                    Log.d("PlayerRepositoryImpl", "Set ${mediaItems.size} media items, startIndex: $startIndex")
//                } catch (e: Exception) {
//                    Log.e("PlayerRepositoryImpl", "Failed to set media items: ${e.message}", e)
//                    _playbackState.update { it.copy(error = "Failed to set media items: ${e.message}") }
//                }
//            } ?: run {
//                Log.w("PlayerRepositoryImpl", "MediaController not set when trying to set media items.")
//                _playbackState.update { it.copy(error = "Player not initialized. Cannot set media items.") }
//            }
//        }
//    }

//    override suspend fun addMediaItems(audioFileUris: List<Uri>) {
//        withContext(Dispatchers.Main) {
//            mediaController?.let { controller ->
//                try {
//                    val allAudioFiles = sharedAudioDataSource.allAudioFiles.value
//                    val mediaItems = audioFileUris.mapNotNull { uri ->
//                        allAudioFiles.find { it.uri == uri }?.let { audioFileMapper.mapAudioFileToMediaItem(it) }
//                    }
//                    controller.addMediaItems(mediaItems)
//                    Log.d("PlayerRepositoryImpl", "Added ${mediaItems.size} media items to playlist")
//                } catch (e: Exception) {
//                    Log.e("PlayerRepositoryImpl", "Failed to add media items: ${e.message}", e)
//                    _playbackState.update { it.copy(error = "Failed to add media items: ${e.message}") }
//                }
//            } ?: run {
//                Log.w("PlayerRepositoryImpl", "MediaController not set when trying to add media items.")
//                _playbackState.update { it.copy(error = "Player not initialized. Cannot add media items.") }
//            }
//        }
//    }

//    override suspend fun play() {
//        withContext(Dispatchers.Main) {
//            mediaController?.play() ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to play.")
//        }
//    }
//
//    override suspend fun pause() {
//        withContext(Dispatchers.Main) {
//            mediaController?.pause() ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to pause.")
//        }
//    }

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
            mediaController?.let { controller ->
                controller.seekTo(positionMs)
                updatePlaybackState()
            } ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to seek.")
        }
    }

//    override suspend fun seekToItem(index: Int) {
//        withContext(Dispatchers.Main) {
//            mediaController?.seekTo(index, C.TIME_UNSET) ?: Log.w("PlayerRepositoryImpl", "MediaController not set when trying to seek to item.")
//        }
//    }

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
                _playbackState.update { it.copy(error = error.message ?: "Player error") }
                Log.e("PlayerRepositoryImpl", "Playback error from ExoPlayer: ${error.message}", error)
            }
        }
    }

    private fun updatePlaybackState() {
        mediaController?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val audioFiles = sharedAudioDataSource.allAudioFiles.value
            val currentAudioFile = currentMediaItem?.let { mediaItem ->
                val mediaUri = mediaItem.localConfiguration?.uri
                audioFiles.find { it.uri == mediaUri } ?: audioFileMapper.mapMediaItemToAudioFile(mediaItem)
            }

            _playbackState.update {
                it.copy(
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
                    isLoading = controller.playbackState == Player.STATE_BUFFERING
                )
            }
        }
    }
}