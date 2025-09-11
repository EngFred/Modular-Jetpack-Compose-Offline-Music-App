package com.engfred.musicplayer.feature_player.data.repository

import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.feature_player.data.repository.controller.ControllerCallback
import com.engfred.musicplayer.feature_player.data.repository.controller.MediaControllerBuilder
import com.engfred.musicplayer.feature_player.data.repository.controller.PlaybackProgressTracker
import com.engfred.musicplayer.feature_player.data.repository.controller.PlaybackStateUpdater
import com.engfred.musicplayer.feature_player.data.repository.controller.QueueManager
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
class PlaybackControllerImpl @Inject constructor(
    private val sharedAudioDataSource: SharedAudioDataSource,
    audioFileMapper: AudioFileMapper,
    permissionHandlerUseCase: PermissionHandlerUseCase,
    playlistRepository: PlaylistRepository,
    @ApplicationContext private val context: Context,
    sessionToken: SessionToken,
) : PlaybackController {
    private val mediaController = MutableStateFlow<MediaController?>(null)
    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var attachedController: MediaController? = null
    private var intendedRepeatMode: RepeatMode = RepeatMode.OFF
    private var intendedShuffleMode: ShuffleMode = ShuffleMode.OFF
    private val pendingPlayNextMediaId = MutableStateFlow<String?>(null)
    // Helpers
    private val stateUpdater = PlaybackStateUpdater(_playbackState, mediaController, sharedAudioDataSource, audioFileMapper)
    private val progressTracker = PlaybackProgressTracker(mediaController, stateUpdater)
    private val controllerCallback = ControllerCallback(repositoryScope, playlistRepository, progressTracker.currentAudioFilePlaybackProgress, stateUpdater, progressTracker, pendingPlayNextMediaId, sharedAudioDataSource, _playbackState)
    private val mediaControllerBuilder = MediaControllerBuilder(context, sessionToken, mediaController, _playbackState)
    private val queueManager = QueueManager(
        sharedAudioDataSource,
        audioFileMapper,
        permissionHandlerUseCase,
        context,
        mediaController,
        _playbackState,
        stateUpdater,
        progressTracker,
        setRepeatCallback = ::setRepeatMode,
        setShuffleCallback = ::setShuffleMode,
        pendingPlayNextMediaId = pendingPlayNextMediaId
    )
    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")
        repositoryScope.launch {
            mediaControllerBuilder.buildAndConnectController()
        }
        repositoryScope.launch {
            mediaController.collectLatest { newController ->
                withContext(Dispatchers.Main) {
                    attachedController?.removeListener(controllerCallback)
                    if (newController != null) {
                        newController.addListener(controllerCallback)
                        attachedController = newController
                        Log.d(TAG, "PlayerControllerImpl received and attached to shared MediaController.")
                        setRepeatMode(intendedRepeatMode)
                        setShuffleMode(intendedShuffleMode)
                        stateUpdater.updatePlaybackState()
                        progressTracker.updateCurrentAudioFilePlaybackProgress(newController)
                    } else {
                        attachedController = null
                        Log.w(TAG, "PlayerControllerImpl received null MediaController.")
                        _playbackState.update { PlaybackState() }
                        controllerCallback.resetTracking()
                        progressTracker.resetProgress()
                    }
                }
            }
        }
        repositoryScope.launch {
            progressTracker.playEventRecorder = controllerCallback
            progressTracker.startPlaybackPositionUpdates()
        }
    }
    override suspend fun initiatePlayback(initialAudioFileUri: android.net.Uri) {
        queueManager.initiatePlayback(initialAudioFileUri, intendedRepeatMode, intendedShuffleMode)
    }
    // In PlaybackControllerImpl.kt
    override suspend fun initiateShufflePlayback(playingQueue: List<AudioFile>) {
        if (playingQueue.isEmpty()) {
            Log.w(TAG, "Cannot initiate shuffle playback: empty queue.")
            return
        }

        // Create a deterministic shuffled queue (a full permutation).
        // This ensures every song appears exactly once in the queue.
        val shuffledQueue = playingQueue.shuffled()

        // Set the playing queue to the shuffled permutation so the QueueManager
        // and sharedAudioDataSource reflect the actual order we want to play.
        sharedAudioDataSource.setPlayingQueue(shuffledQueue)

        // IMPORTANT: disable ExoPlayer's internal shuffle to avoid double-shuffling
        // and unexpected skip behavior. We'll manage order via the shuffledQueue.
        setShuffleMode(ShuffleMode.OFF)

        // Start playback at the first item of the shuffled queue.
        // initiatePlayback will set the controller queue and begin playback.
        initiatePlayback(shuffledQueue.first().uri)
    }

    override suspend fun playPause() {
        withContext(Dispatchers.Main) {
            mediaController.value?.run {
                if (isPlaying) pause() else play()
            } ?: Log.w(TAG, "MediaController not set when trying to play/pause.")
        }
    }
    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            mediaController.value?.seekToNextMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip next.")
        }
    }
    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            mediaController.value?.seekToPreviousMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip previous.")
        }
    }
    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            mediaController.value?.let { controller ->
                controller.seekTo(positionMs)
                stateUpdater.updatePlaybackState()
                progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
            } ?: Log.w(TAG, "MediaController not set when trying to seek.")
        }
    }
    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            intendedRepeatMode = mode
            mediaController.value?.let { controller ->
                controller.repeatMode = when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
                Log.d(TAG, "Set repeat mode to $mode")
            } ?: Log.w(TAG, "MediaController not set when trying to set repeat mode. Stored $mode for later.")
        }
    }
    override suspend fun setShuffleMode(mode: ShuffleMode) {
        withContext(Dispatchers.Main) {
            intendedShuffleMode = mode
            mediaController.value?.let { controller ->
                controller.shuffleModeEnabled = (mode == ShuffleMode.ON)
                Log.d(TAG, "Set shuffle mode to $mode")
            } ?: Log.w(TAG, "MediaController not set when trying to set shuffle mode. Stored $mode for later.")
        }
    }
    override suspend fun addAudioToQueueNext(audioFile: AudioFile) {
        queueManager.addAudioToQueueNext(audioFile)
    }
    override suspend fun releasePlayer() {
        val controllerToRelease = mediaController.value
        repositoryScope.cancel()
        withContext(Dispatchers.Main) {
            attachedController?.removeListener(controllerCallback)
            attachedController = null
            Log.d(TAG, "PlayerControllerImpl resources released and listener removed.")
            controllerCallback.resetTracking()
            progressTracker.resetProgress()
            try {
                controllerToRelease?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaController: ${e.message}")
            } finally {
                mediaController.value = null
            }
        }
    }
    override fun clearPlaybackError() {
        _playbackState.update { it.copy(error = null) }
    }
    override suspend fun onAudioFileRemoved(deletedAudioFile: AudioFile) {
        queueManager.onAudioFileRemoved(deletedAudioFile)
    }
    override suspend fun removeFromQueue(audioFile: AudioFile) {
        queueManager.removeFromQueue(audioFile)
    }

    override suspend fun waitUntilReady(timeoutMs: Long): Boolean {
        val start = System.currentTimeMillis()
        while (mediaController.value == null && System.currentTimeMillis() - start < timeoutMs) {
            delay(100)
        }
        return mediaController.value != null
    }
}