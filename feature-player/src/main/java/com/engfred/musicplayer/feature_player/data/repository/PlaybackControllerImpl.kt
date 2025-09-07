package com.engfred.musicplayer.feature_player.data.repository

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
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
    private val audioFileMapper: AudioFileMapper,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val playlistRepository: PlaylistRepository,
    @ApplicationContext private val context: Context,
    private val sessionToken: SessionToken,
) : PlaybackController {

    private val mediaController = MutableStateFlow<MediaController?>(null)
    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var attachedController: MediaController? = null

    // ---------------------------------------------
    // NEW: Store intended repeat and shuffle modes to apply them when MediaController connects
    // or when playback starts, ensuring early calls from MainActivity aren't lost.
    // ---------------------------------------------
    private var intendedRepeatMode: RepeatMode = RepeatMode.OFF
    private var intendedShuffleMode: ShuffleMode = ShuffleMode.OFF

    // FIX: Added to handle restoring shuffle mode after inserting play next items.
    private val pendingPlayNextMediaId = MutableStateFlow<String?>(null)

    // Helpers
    private val stateUpdater = PlaybackStateUpdater(_playbackState, mediaController, sharedAudioDataSource, audioFileMapper)
    private val progressTracker = PlaybackProgressTracker(mediaController, stateUpdater)
    private val controllerCallback = ControllerCallback(repositoryScope, playlistRepository, progressTracker.currentAudioFilePlaybackProgress, stateUpdater, progressTracker, pendingPlayNextMediaId, sharedAudioDataSource, playbackState = _playbackState)
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
        pendingPlayNextMediaId = pendingPlayNextMediaId // FIX: Pass the pending flow to QueueManager for use in addAudioToQueueNext.
    )

    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")
        // ---------------------------------------------
        // CHANGE: Build/connect MediaController once this repo is created.
        // This keeps ownership explicit in this layer. If you truly want a single "shared" controller
        // app-wide, extract this into a dedicated Manager and inject its StateFlow here.
        // ---------------------------------------------
        repositoryScope.launch {
            mediaControllerBuilder.buildAndConnectController()
        }

        // Launch a coroutine to observe changes in the MediaController instance
        repositoryScope.launch {
            mediaController.collectLatest { newController ->
                withContext(Dispatchers.Main) { // Ensure UI-related operations are on Main thread
                    // Remove listener from the previously attached controller, if any
                    attachedController?.removeListener(controllerCallback)

                    if (newController != null) {
                        // Attach listener to the new controller
                        newController.addListener(controllerCallback)
                        attachedController = newController
                        Log.d(TAG, "PlayerControllerImpl received and attached to shared MediaController.")

                        // ---------------------------------------------
                        // NEW: Apply stored repeat and shuffle modes when MediaController connects
                        // to ensure modes set by MainActivity are applied as soon as possible.
                        // ---------------------------------------------
                        setRepeatMode(intendedRepeatMode)
                        setShuffleMode(intendedShuffleMode)
                        stateUpdater.updatePlaybackState() // Initial state update for UI

                        // Initial update of playback progress for the first song
                        progressTracker.updateCurrentAudioFilePlaybackProgress(newController)

                    } else {
                        // Controller became null, indicating service disconnection or release
                        attachedController = null
                        Log.w(TAG, "PlayerControllerImpl received null MediaController. Releasing player state.")
                        _playbackState.update { PlaybackState() }

                        // Reset tracking variables in the callback as the player state is now unknown/idle
                        controllerCallback.resetTracking()
                        progressTracker.resetProgress() // Reset progress tracker
                    }
                }
            }
        }

        // Launch a coroutine to periodically update playback position in the state
        repositoryScope.launch {
            progressTracker.startPlaybackPositionUpdates()
        }
    }

    override suspend fun initiatePlayback(initialAudioFileUri: android.net.Uri) {
        queueManager.initiatePlayback(initialAudioFileUri, intendedRepeatMode, intendedShuffleMode)
    }

    // NEW: Implementation for initiating playback in shuffle mode.
    override suspend fun initiateShufflePlayback(playingQueue: List<AudioFile>) {
        if (playingQueue.isEmpty()) {
            Log.w(TAG, "Cannot initiate shuffle playback: empty queue.")
            return
        }
        setShuffleMode(ShuffleMode.ON)
        val randomAudio = playingQueue.shuffled().first()
        sharedAudioDataSource.setPlayingQueue(playingQueue)
        initiatePlayback(randomAudio.uri)
    }

    /**
     * Toggles the playback state between playing and paused.
     */
    override suspend fun playPause() {
        withContext(Dispatchers.Main) {
            mediaController.value?.run {
                if (isPlaying) pause() else play()
            } ?: Log.w(TAG, "MediaController not set when trying to play/pause.")
        }
    }

    /**
     * Skips to the next media item in the playback queue.
     */
    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            mediaController.value?.seekToNextMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip next.")
        }
    }

    /**
     * Skips to the previous media item in the playback queue.
     */
    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            mediaController.value?.seekToPreviousMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip previous.")
        }
    }

    /**
     * Seeks to a specific position within the current media item.
     * @param positionMs The position in milliseconds to seek to.
     */
    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            mediaController.value?.let { controller ->
                controller.seekTo(positionMs)
                stateUpdater.updatePlaybackState() // Update state immediately after seeking
                progressTracker.updateCurrentAudioFilePlaybackProgress(controller) // Update progress tracker after seeking
            } ?: Log.w(TAG, "MediaController not set when trying to seek.")
        }
    }

    /**
     * Sets the repeat mode for the player.
     * @param mode The desired [RepeatMode] (OFF, ONE, ALL).
     */
    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            // ---------------------------------------------
            // NEW: Store the intended repeat mode to apply it later if MediaController isn't ready
            // and log when it's stored vs. applied for debugging.
            // ---------------------------------------------
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

    /**
     * Sets the shuffle mode for the player.
     * @param mode The desired [ShuffleMode] (ON, OFF).
     */
    override suspend fun setShuffleMode(mode: ShuffleMode) {
        withContext(Dispatchers.Main) {
            // ---------------------------------------------
            // NEW: Store the intended shuffle mode to apply it later if MediaController isn't ready
            // and log when it's stored vs. applied for debugging.
            // ---------------------------------------------
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

    /**
     * Releases all resources held by the player controller, including removing listeners
     * and cancelling coroutines. Should be called when the player is no longer needed.
     */
    override suspend fun releasePlayer() {
        // ---------------------------------------------
        // CHANGE: Ensure we also release the underlying MediaController we created here.
        // If you centralize the controller elsewhere, remove the release() here and delegate to that owner.
        // ---------------------------------------------
        val controllerToRelease = mediaController.value
        repositoryScope.cancel() // Cancel all coroutines launched in this scope
        withContext(Dispatchers.Main) {
            attachedController?.removeListener(controllerCallback)
            attachedController = null
            Log.d(TAG, "PlayerControllerImpl resources released and listener removed.")
            controllerCallback.resetTracking() // Reset event tracking
            progressTracker.resetProgress() // Reset progress tracker
            try {
                controllerToRelease?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaController: ${e.message}")
            } finally {
                mediaController.value = null
            }
        }
    }

    /**
     * Clears any current playback error message from the [_playbackState].
     */
    override fun clearPlaybackError() {
        _playbackState.update { it.copy(error = null) }
    }

    override suspend fun onAudioFileRemoved(deletedAudioFile: AudioFile) {
        queueManager.onAudioFileRemoved(deletedAudioFile)
    }

    override suspend fun removeFromQueue(audioFile: AudioFile) {
        queueManager.removeFromQueue(audioFile)
    }
}