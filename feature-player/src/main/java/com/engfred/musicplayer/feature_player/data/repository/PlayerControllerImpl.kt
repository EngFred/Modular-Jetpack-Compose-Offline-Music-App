package com.engfred.musicplayer.feature_player.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.engfred.musicplayer.core.data.session.MediaControllerProvider
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlayerControllerImpl"

@UnstableApi
@Singleton
class PlayerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val audioFileMapper: AudioFileMapper,
    private val mediaControllerProvider: MediaControllerProvider
) : PlayerController {

    private var currentMediaController: MediaController? = null
    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val controllerCallback = ControllerCallback()

    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")
        repositoryScope.launch {
            mediaControllerProvider.mediaController.collectLatest { controller ->
                currentMediaController?.removeListener(controllerCallback)

                currentMediaController = controller
                if (controller != null) {
                    controller.addListener(controllerCallback)
                    Log.d(TAG, "PlayerControllerImpl received and attached to shared MediaController.")
                    // Initial update when controller becomes available. This is already on Main due to updatePlaybackState()
                    withContext(Dispatchers.Main) {
                        updatePlaybackState()
                    }
                } else {
                    Log.w(TAG, "PlayerControllerImpl received null MediaController. Releasing player state.")
                    _playbackState.update { PlaybackState() }
                }
            }
        }
        repositoryScope.launch {
            startPlaybackPositionUpdates()
        }
    }

    private suspend fun startPlaybackPositionUpdates() {
        while (true) {
            // The entire block accessing MediaController properties/methods must be on Main
            currentMediaController?.let { controller ->
                withContext(Dispatchers.Main) {
                    if (controller.playbackState != Player.STATE_IDLE && controller.playbackState != Player.STATE_ENDED) {
                        updatePlaybackState()
                    }
                }
            }
            delay(500)
        }
    }

    override suspend fun initiatePlayback(initialAudioFileUri: Uri) {
        withContext(Dispatchers.Main) {
            val controller = currentMediaController
            if (controller == null) {
                Log.e(TAG, "MediaController not available for playback initiation.")
                _playbackState.update { it.copy(error = "Player not initialized.") }
                return@withContext
            }

            val allAudioFiles = sharedAudioDataSource.allAudioFiles.value
            if (allAudioFiles.isEmpty()) {
                Log.w(TAG, "Shared audio files are empty. Cannot initiate playback.")
                _playbackState.update { it.copy(error = "No audio files available to play.") }
                return@withContext
            }

            val startIndex = allAudioFiles.indexOfFirst { it.uri == initialAudioFileUri }.coerceAtLeast(0)
            try {
                val mediaItems = allAudioFiles.map { audioFileMapper.mapAudioFileToMediaItem(it) }
                controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                controller.prepare()
                controller.play()
                Log.d(TAG, "Initiated playback for: $initialAudioFileUri")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting media items or playing during initiation: ${e.message}", e)
                _playbackState.update { it.copy(error = "Playback error: ${e.message}") }
            }
        }
    }

    override suspend fun playPause() {
        withContext(Dispatchers.Main) { // This is already good
            currentMediaController?.run {
                if (isPlaying) pause() else play()
            } ?: Log.w(TAG, "MediaController not set when trying to play/pause.")
        }
    }

    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) { // This is already good
            currentMediaController?.seekToNextMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip next.")
        }
    }

    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            currentMediaController?.seekToPreviousMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip previous.")
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            currentMediaController?.let { controller ->
                controller.seekTo(positionMs)
                updatePlaybackState()
            } ?: Log.w(TAG, "MediaController not set when trying to seek.")
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            currentMediaController?.let { controller ->
                controller.repeatMode = when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            } ?: Log.w(TAG, "MediaController not set when trying to set repeat mode.")
        }
    }

    override suspend fun setShuffleMode(mode: ShuffleMode) {
        withContext(Dispatchers.Main) {
            currentMediaController?.let { controller ->
                controller.shuffleModeEnabled = (mode == ShuffleMode.ON)
            } ?: Log.w(TAG, "MediaController not set when trying to set shuffle mode.")
        }
    }

    override suspend fun releasePlayer() {
        repositoryScope.cancel()
        withContext(Dispatchers.Main) {
            currentMediaController?.removeListener(controllerCallback)
            currentMediaController = null
            Log.d(TAG, "PlayerControllerImpl resources released and listener removed.")
        }
    }

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
                updatePlaybackState()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Similar to onEvents, this is called on Main thread.
            _playbackState.update { it.copy(error = error.message ?: "Player error") }
            Log.e(TAG, "Playback error from ExoPlayer: ${error.message}", error)
        }
    }

    private fun updatePlaybackState() {
        // This function is correctly called from the Main thread already
        currentMediaController?.let { controller ->
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