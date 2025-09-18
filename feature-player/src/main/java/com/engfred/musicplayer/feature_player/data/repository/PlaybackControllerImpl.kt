package com.engfred.musicplayer.feature_player.data.repository

import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.SharedAudioDataSource
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
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val settingsRepository: SettingsRepository
) : PlaybackController {

    private val mediaController = MutableStateFlow<MediaController?>(null)
    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): kotlinx.coroutines.flow.Flow<PlaybackState> = _playbackState.asStateFlow()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var attachedController: MediaController? = null
    private val pendingPlayNextMediaId = MutableStateFlow<String?>(null)

    // Keep the last "intended" repeat mode (comes from preferences or user action)
    // This is applied to the controller as soon as it is available.
    @Volatile
    private var intendedRepeatMode: RepeatMode = RepeatMode.OFF

    // Helpers
    private val stateUpdater = PlaybackStateUpdater(_playbackState, mediaController, sharedAudioDataSource, audioFileMapper)
    private val progressTracker = PlaybackProgressTracker(mediaController, stateUpdater)
    private val controllerCallback = ControllerCallback(
        repositoryScope,
        playlistRepository,
        stateUpdater,
        progressTracker,
        pendingPlayNextMediaId,
        sharedAudioDataSource,
        _playbackState
    )
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
        pendingPlayNextMediaId = pendingPlayNextMediaId
    )

    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")

        // 1) Start collecting settings first so intendedRepeatMode is known early.
        repositoryScope.launch {
            settingsRepository.getAppSettings()
                .map { it.repeatMode } // map to RepeatMode
                .distinctUntilChanged()
                .collectLatest { mode ->
                    Log.d(TAG, "Settings repository emitted repeat mode: $mode")
                    intendedRepeatMode = mode
                    // Apply to controller if available; if not available, setRepeatMode will store for later application.
                    setRepeatMode(mode)
                }
        }

        // 2) Build and connect the media controller after we've started listening to settings.
        repositoryScope.launch {
            mediaControllerBuilder.buildAndConnectController()
        }

        // 3) React to mediaController changes (attach/detach) on Main thread
        repositoryScope.launch {
            mediaController.collectLatest { newController ->
                withContext(Dispatchers.Main) {
                    attachedController?.removeListener(controllerCallback)
                    if (newController != null) {
                        newController.addListener(controllerCallback)
                        attachedController = newController
                        Log.d(TAG, "PlayerControllerImpl received and attached to shared MediaController.")

                        // Apply the intended repeat mode (whatever was read from prefs or previously set)
                        // We call the suspend setRepeatMode which will apply to the attached controller.
                        setRepeatMode(intendedRepeatMode)

                        // Update other state and start tracking progress
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

        // 4) Start progress tracker loop
        repositoryScope.launch {
            progressTracker.playEventRecorder = controllerCallback
            progressTracker.startPlaybackPositionUpdates()
        }
    }

    override suspend fun initiatePlayback(initialAudioFileUri: android.net.Uri) {
        queueManager.initiatePlayback(initialAudioFileUri, intendedRepeatMode)
    }

    override suspend fun initiateShufflePlayback(playingQueue: List<AudioFile>) {
        if (playingQueue.isEmpty()) {
            Log.w(TAG, "Cannot initiate shuffle playback: empty queue.")
            return
        }

        val shuffledQueue = playingQueue.shuffled()
        sharedAudioDataSource.setPlayingQueue(shuffledQueue)
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

    /**
     * Sets repeat mode on the MediaController if available, otherwise stores it to apply later.
     *
     * NOTE: This function applies the *intended* repeat mode but does NOT persist it to preferences.
     * Persisting should be done by the settings UI (call settingsRepository.updateRepeatMode(mode)).
     */
    override suspend fun setRepeatMode(mode: RepeatMode) {
        // remember intention immediately (helps avoid races)
        intendedRepeatMode = mode

        withContext(Dispatchers.Main) {
            mediaController.value?.let { controller ->
                controller.repeatMode = when (mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
                Log.d(TAG, "Set repeat mode to $mode on MediaController")
                // Optionally update playback state object so UI reads the current mode immediately
                _playbackState.update { it.copy(repeatMode = mode) }
            } ?: Log.w(TAG, "MediaController not set when trying to set repeat mode. Stored $mode for later.")
        }
    }

    override suspend fun setShuffleMode(mode: ShuffleMode) {
        withContext(Dispatchers.Main) {
            mediaController.value?.let {
                it.shuffleModeEnabled = mode == ShuffleMode.ON
                Log.d(TAG, "Set shuffle mode to $mode")
                _playbackState.update { it.copy(shuffleMode = mode) }
            } ?: Log.w(TAG, "MediaController not set when trying to set shuffle mode.")
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
