package com.engfred.musicplayer.feature_player.data.repository

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.util.MediaUtils.isAudioFileAccessible
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
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlayerControllerImpl"


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

    /**
     * Provides a [Flow] of the current playback state.
     */
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()

    /**
     * Data class to hold the ID, current position, and total duration of the currently playing song.
     * Used internally for robust play event tracking.
     */
    private data class CurrentAudioFilePlaybackProgress(
        val mediaId: String? = null,
        val playbackPositionMs: Long = 0L,
        val totalDurationMs: Long = 0L
    )

    /**
     * A [MutableStateFlow] that tracks the playback progress of the currently playing audio file.
     * This is crucial for accurately capturing the played duration of a song even if it's skipped.
     */
    private val _currentAudioFilePlaybackProgress = MutableStateFlow(CurrentAudioFilePlaybackProgress())

    /**
     * A [CoroutineScope] tied to the lifecycle of this repository,
     * using [SupervisorJob] for resilience and [Dispatchers.IO] for background operations.
     */
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * The internal callback listener for [Player] events, used to update playback state
     * and record song play events.
     */
    private val controllerCallback = ControllerCallback()

    /**
     * The currently attached [MediaController] instance. Can be null if not yet connected.
     */
    private var attachedController: MediaController? = null

    // ---------------------------------------------
    // NEW: Store intended repeat and shuffle modes to apply them when MediaController connects
    // or when playback starts, ensuring early calls from MainActivity aren't lost.
    // ---------------------------------------------
    private var intendedRepeatMode: RepeatMode = RepeatMode.OFF
    private var intendedShuffleMode: ShuffleMode = ShuffleMode.OFF

    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")

        // ---------------------------------------------
        // CHANGE: Build/connect MediaController once this repo is created.
        // This keeps ownership explicit in this layer. If you truly want a single "shared" controller
        // app-wide, extract this into a dedicated Manager and inject its StateFlow here.
        // ---------------------------------------------
        repositoryScope.launch {
            buildAndConnectController()
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
                        updatePlaybackState() // Initial state update for UI
                        // Initial update of playback progress for the first song
                        updateCurrentAudioFilePlaybackProgress(newController)
                    } else {
                        // Controller became null, indicating service disconnection or release
                        attachedController = null
                        Log.w(TAG, "PlayerControllerImpl received null MediaController. Releasing player state.")
                        _playbackState.update { PlaybackState() }
                        // Reset tracking variables in the callback as the player state is now unknown/idle
                        controllerCallback.resetTracking()
                        _currentAudioFilePlaybackProgress.value = CurrentAudioFilePlaybackProgress() // Reset progress tracker
                    }
                }
            }
        }

        // Launch a coroutine to periodically update playback position in the state
        repositoryScope.launch {
            startPlaybackPositionUpdates()
        }
    }

    // ---------------------------------------------
    // CHANGE: Build the MediaController (Main thread) and push it into the StateFlow.
    // Uses coroutines-guava 'await' to suspend until the controller is ready.
    // Includes a small retry loop for resilience if the service isn't up yet.
    // ---------------------------------------------
    private suspend fun buildAndConnectController() {
        withContext(Dispatchers.Main) {
            var attempts = 0
            val maxAttempts = 3
            var lastError: Throwable? = null

            while (attempts < maxAttempts) {
                try {
                    val controller = MediaController.Builder(context, sessionToken)
                        .buildAsync()
                        .await()
                    Log.d(TAG, "MediaController connected (attempt ${attempts + 1}).")
                    mediaController.value = controller
                    return@withContext
                } catch (e: Exception) {
                    lastError = e
                    attempts++
                    Log.w(TAG, "Failed to connect MediaController (attempt $attempts/$maxAttempts): ${e.message}")
                    delay(300L * attempts) // backoff
                }
            }

            Log.e(TAG, "Unable to connect MediaController after $maxAttempts attempts.", lastError)
            mediaController.value = null
            _playbackState.update { it.copy(error = "Player not initialized.") }
        }
    }

    /**
     * Starts a continuous loop to update the current playback position in the [_playbackState]
     * and [_currentAudioFilePlaybackProgress] at regular intervals (every 500ms).
     */
    private suspend fun startPlaybackPositionUpdates() {
        while (true) {
            withContext(Dispatchers.Main) {
                mediaController.value?.let { actualController ->
                    // Only update if player is not idle or ended, to avoid unnecessary updates
                    if (actualController.playbackState != Player.STATE_IDLE && actualController.playbackState != Player.STATE_ENDED) {
                        updatePlaybackState()
                        updateCurrentAudioFilePlaybackProgress(actualController) // Keep this updated frequently
                    }
                }
            }
            delay(500) // Update every half second
        }
    }

    /**
     * Updates the internal [_currentAudioFilePlaybackProgress] with the latest playback
     * information of the actively playing song.
     *
     * @param controller The [MediaController] instance.
     */
    private fun updateCurrentAudioFilePlaybackProgress(controller: MediaController) {
        controller.currentMediaItem?.let { currentMediaItem ->
            _currentAudioFilePlaybackProgress.update {
                it.copy(
                    mediaId = currentMediaItem.mediaId,
                    playbackPositionMs = controller.currentPosition,
                    totalDurationMs = controller.duration.coerceAtLeast(0L)
                )
            }
        } ?: _currentAudioFilePlaybackProgress.update { CurrentAudioFilePlaybackProgress() }
    }

    /**
     * Initiates playback of a given audio file within the current playing queue.
     * If the queue doesn't match the controller's queue, it will be set.
     *
     * @param initialAudioFileUri The [android.net.Uri] of the audio file to start playback from.
     */
    override suspend fun initiatePlayback(initialAudioFileUri: android.net.Uri) {
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available for playback initiation.")
                _playbackState.update { it.copy(error = "Player not initialized.") }
                return@withContext
            }

            val playingQueue = sharedAudioDataSource.playingQueueAudioFiles.value
            if (playingQueue.isEmpty()) {
                Log.w(TAG, "Shared audio files are empty. Cannot initiate playback.")
                _playbackState.update { it.copy(error = "No audio files available to play.") }
                return@withContext
            }

            // Find the index of the initial audio file in the current playing queue
            val startIndex = playingQueue.indexOfFirst { it.uri == initialAudioFileUri }.coerceAtLeast(0)
            val audioFileToPlay = playingQueue.getOrNull(startIndex)

            if (audioFileToPlay == null) {
                Log.w(TAG, "Initial audio file not found in current playing queue for URI: $initialAudioFileUri")
                _playbackState.update { it.copy(error = "Selected song not found in library.") }
                return@withContext
            }

            // Pre-playback accessibility check: ensure the file exists and is readable
            val isAccessible = isAudioFileAccessible(
                context = context,
                audioFileUri = audioFileToPlay.uri,
                permissionHandlerUseCase = permissionHandlerUseCase
            )
            if (!isAccessible) {
                Log.e(TAG, "Audio file is not accessible: ${audioFileToPlay.uri}. Aborting playback.")
                _playbackState.update {
                    it.copy(
                        currentAudioFile = null,
                        isPlaying = false,
                        error = "Cannot play '${audioFileToPlay.title}'. File not found or storage permission denied."
                    )
                }
                return@withContext
            }

            // Check if the player's current media items match the shared queue to optimize
            val currentMediaItemsMatchSharedSource = controller.mediaItemCount == playingQueue.size &&
                    playingQueue.indices.all { i ->
                        controller.getMediaItemAt(i).mediaId == audioFileMapper.mapAudioFileToMediaItem(playingQueue[i]).mediaId
                    }

            if (currentMediaItemsMatchSharedSource && controller.currentMediaItemIndex == startIndex) {
                // Player queue is identical and already at the correct song, just play if paused
                Log.d(TAG, "Player queue already matches shared data source and is at the correct song. Just playing.")
                if (!controller.isPlaying) {
                    controller.play()
                }
                updatePlaybackState()
                updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                return@withContext
            } else if (currentMediaItemsMatchSharedSource) {
                // Player queue is identical but at a different song, seek to the correct one
                Log.d(TAG, "Player queue matches shared data source, but starting index is different. Seeking and playing.")
                controller.seekToDefaultPosition(startIndex)
                if (!controller.isPlaying) {
                    controller.play()
                }
                updatePlaybackState()
                updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                return@withContext
            }

            // If the queues don't match, set new media items for the player
            try {
                val mediaItems = playingQueue.map { audioFileMapper.mapAudioFileToMediaItem(it) }
                controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                // ---------------------------------------------
                // NEW: Re-apply stored repeat and shuffle modes to ensure they're set for new playback
                // sessions, especially when the playlist changes.
                // ---------------------------------------------
                setRepeatMode(intendedRepeatMode)
                setShuffleMode(intendedShuffleMode)
                controller.prepare()
                controller.play()
                Log.d(TAG, "Initiated playback for: $initialAudioFileUri by setting new media items from shared source.")
                updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
            } catch (e: Exception) {
                Log.e(TAG, "Error setting media items or playing during initiation: ${e.message}", e)
                _playbackState.update { it.copy(error = "Playback error: ${e.message}") }
            }
        }
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
     *
     * @param positionMs The position in milliseconds to seek to.
     */
    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            mediaController.value?.let { controller ->
                controller.seekTo(positionMs)
                updatePlaybackState() // Update state immediately after seeking
                updateCurrentAudioFilePlaybackProgress(controller) // Update progress tracker after seeking
            } ?: Log.w(TAG, "MediaController not set when trying to seek.")
        }
    }

    /**
     * Sets the repeat mode for the player.
     *
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
     *
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

    /**
     * Adds an [AudioFile] to the player's queue right after the currently playing song.
     *
     * @param audioFile The [AudioFile] to add to the queue.
     */
    override suspend fun addAudioToQueueNext(audioFile: AudioFile) {
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available. Cannot add to queue.")
                _playbackState.update { it.copy(error = "Player not initialized. Cannot add to queue.") }
                return@withContext
            }

            // Pre-add to queue accessibility check
            val isAccessible = isAudioFileAccessible(context, audioFile.uri, permissionHandlerUseCase)
            if (!isAccessible) {
                Log.e(TAG, "Audio file is not accessible for 'Play Next': ${audioFile.uri}. Aborting add.")
                _playbackState.update { it.copy(error = "Cannot add '${audioFile.title}'. File not found or storage permission denied.") }
                return@withContext
            }

            val mediaItemToAdd = audioFileMapper.mapAudioFileToMediaItem(audioFile)
            val newItemMediaId = mediaItemToAdd.mediaId

            Log.d(TAG, "Attempting to 'Play Next': Title='${audioFile.title}', AudioFile.ID='${audioFile.id}', NewItemMediaId='$newItemMediaId'")

            // Determine the insert index: after the current song, or at the beginning if no song is playing
            val currentMediaItemIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentMediaItemIndex == C.INDEX_UNSET || controller.mediaItemCount == 0) {
                0
            } else {
                currentMediaItemIndex + 1
            }

            try {
                controller.addMediaItem(insertIndex, mediaItemToAdd)
                Log.d(TAG, "Added ${audioFile.title} (ID: ${audioFile.id}) to queue at index $insertIndex (Play Next).")

                // If this was the very first item added and the player was idle, start playback
                if (controller.mediaItemCount == 1 && !controller.isPlaying && controller.playbackState == Player.STATE_IDLE) {
                    controller.prepare()
                    controller.play()
                    Log.d(TAG, "Started playback as it was the first item in the queue.")
                    updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding media item to queue: ${e.message}", e)
                _playbackState.update { it.copy(error = "Error adding song to queue: ${e.message}") }
            }
        }
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
            _currentAudioFilePlaybackProgress.value = CurrentAudioFilePlaybackProgress() // Reset progress tracker

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
     * An inner class that implements [Player.Listener] to react to playback events
     * and manage song play event recording.
     */
    private inner class ControllerCallback : Player.Listener {
        // Tracks the previous playback state to detect transitions (e.g., from playing to ended)
        private var lastPlaybackState = Player.STATE_IDLE

        // A timestamp to debounce rapid consecutive events (e.g., STATE_ENDED immediately followed by MEDIA_ITEM_TRANSITION)
        private var lastEventProcessedTimestamp: Long = 0L

        /**
         * Resets internal tracking variables. Called when player controller is released or disconnected.
         */
        fun resetTracking() {
            lastPlaybackState = Player.STATE_IDLE
            lastEventProcessedTimestamp = 0L
            Log.d(TAG, "ControllerCallback event tracking variables reset.")
        }

        /**
         * Called when player events occur. This is where the "top tracks" logic resides.
         *
         * @param player The [Player] instance.
         * @param events The [Player.Events] that occurred.
         */
        override fun onEvents(player: Player, events: Player.Events) {
            val isSongTransition = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
            val isPlaybackEnded = events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
                    player.playbackState == Player.STATE_ENDED &&
                    lastPlaybackState != Player.STATE_ENDED // Transition from non-ended to ended

            if (isSongTransition || isPlaybackEnded) {
                val now = System.currentTimeMillis()
                // Simple debounce: if an event just occurred very recently, skip processing
                // the play event for the *previous* song to avoid double-counting.
                if (now - lastEventProcessedTimestamp < 100) { // 100ms threshold, adjust if needed
                    Log.d(TAG, "Skipping play event processing for previous song due to rapid succession of events.")
                    updatePlaybackState()
                    // Still update progress for the *new* current item to avoid stale data
                    updateCurrentAudioFilePlaybackProgress(player as MediaController)
                    lastPlaybackState = player.playbackState
                    return
                }
                lastEventProcessedTimestamp = now

                // --- CRITICAL: Use the *last known progress* for the song that just ended/transitioned ---
                // This value comes from the _currentAudioFilePlaybackProgress flow, which is updated frequently
                // by startPlaybackPositionUpdates, giving us the most accurate "final" state of the *previous* song.
                val songToEvaluateProgress = _currentAudioFilePlaybackProgress.value

                // If it's a transition and the mediaId in _currentAudioFilePlaybackProgress
                // matches the old mediaId before the transition (this is generally true due to timing),
                // then we proceed.
                // We also check if player.currentMediaItem is different from songToEvaluateProgress.mediaId
                // to confirm a transition actually occurred from THAT song.
                val currentMediaItemAfterEvent = player.currentMediaItem
                val isActuallyDifferentSong = currentMediaItemAfterEvent?.mediaId != songToEvaluateProgress.mediaId

                // Ensure we have valid data for the song we're about to evaluate
                if (songToEvaluateProgress.mediaId != null &&
                    songToEvaluateProgress.totalDurationMs > 0 &&
                    (isSongTransition && isActuallyDifferentSong || isPlaybackEnded)) { // Only evaluate if a real transition or true end

                    val playedDurationMs = songToEvaluateProgress.playbackPositionMs
                    val totalDurationMs = songToEvaluateProgress.totalDurationMs

                    if (playedDurationMs != C.TIME_UNSET) {
                        val playedPercentage = playedDurationMs.toFloat() / totalDurationMs
                        val minPlayDurationMs = 30 * 1000L // Minimum 30 seconds to count as a play

                        // Robust condition for a "significant play":
                        // Either 50% of the song was played OR at least 30 seconds were played.
                        if (playedPercentage >= 0.5f || playedDurationMs >= minPlayDurationMs) {
                            repositoryScope.launch {
                                val audioFileId = songToEvaluateProgress.mediaId.toLongOrNull()
                                if (audioFileId != null) {
                                    playlistRepository.recordSongPlayEvent(audioFileId)
                                    Log.d(TAG, "Recorded play event for song ID: $audioFileId " +
                                            "(Played: ${playedDurationMs / 1000}s / ${totalDurationMs / 1000}s, " +
                                            "Percentage: ${"%.2f".format(playedPercentage * 100)}%)")
                                } else {
                                    Log.e(TAG, "Could not convert mediaId to AudioFile ID: ${songToEvaluateProgress.mediaId}")
                                }
                            }
                        } else {
                            Log.d(TAG, "Skipped recording play event for song ID: ${songToEvaluateProgress.mediaId} " +
                                    "(Insignificant play: ${playedDurationMs / 1000}s / ${totalDurationMs / 1000}s, " +
                                    "Percentage: ${"%.2f".format(playedPercentage * 100)}%)")
                        }
                    }
                } else {
                    Log.d(TAG, "Skipped play event evaluation. No valid previous song data or not a significant transition. " +
                            "MediaId: ${songToEvaluateProgress.mediaId}, Duration: ${songToEvaluateProgress.totalDurationMs}, " +
                            "Played: ${songToEvaluateProgress.playbackPositionMs}, IsTransition: $isSongTransition, IsEnded: $isPlaybackEnded")
                }
            }

            // Always update the overall playback state exposed to the UI
            updatePlaybackState()
            // Always update progress tracker for the *new* current song, for the next event cycle
            updateCurrentAudioFilePlaybackProgress(player as MediaController)
            // Always update lastPlaybackState for detecting future transitions
            lastPlaybackState = player.playbackState
        }
    }

    /**
     * Clears any current playback error message from the [_playbackState].
     */
    override fun clearPlaybackError() {
        _playbackState.update { it.copy(error = null) }
    }

    /**
     * Handles the event when an audio file is removed from the device's storage.
     * It attempts to remove the corresponding media item from the player's queue.
     *
     * @param deletedAudioFile The [AudioFile] that was removed.
     */
    override suspend fun onAudioFileRemoved(deletedAudioFile: AudioFile) {
        Log.d(TAG, "onAudioFileRemoved: Attempting to remove '${deletedAudioFile.title}' (ID: ${deletedAudioFile.id}) from player queue.")
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available. Cannot remove audio file from player queue.")
                return@withContext
            }

            Log.d(TAG, "onAudioFileRemoved: Attempting to remove '${deletedAudioFile.title}' (ID: ${deletedAudioFile.id}) from player queue.")

            val deletedMediaId = deletedAudioFile.id.toString()
            var removedIndex: Int? = null

            // Find the index of the deleted song in the player's current queue
            for (i in 0 until controller.mediaItemCount) {
                val mediaItem = controller.getMediaItemAt(i)
                if (mediaItem.mediaId == deletedMediaId) {
                    removedIndex = i
                    break
                }
            }

            if (removedIndex != null) {
                try {
                    // Check if the deleted song was currently playing
                    val wasPlayingDeletedSong = (controller.currentMediaItemIndex == removedIndex) && controller.isPlaying
                    controller.removeMediaItem(removedIndex)
                    Log.d(TAG, "Successfully removed '${deletedAudioFile.title}' from ExoPlayer queue at index $removedIndex.")

                    if (controller.mediaItemCount == 0) {
                        // If queue becomes empty, stop and clear player completely
                        controller.stop()
                        controller.clearMediaItems()
                        _playbackState.update { it.copy(currentAudioFile = null, isPlaying = false) }
                        _currentAudioFilePlaybackProgress.value = CurrentAudioFilePlaybackProgress() // Reset progress tracker
                        Log.d(TAG, "ExoPlayer queue is empty after deletion. Stopping playback.")
                    } else if (wasPlayingDeletedSong) {
                        // If the currently playing song was deleted, the player automatically transitions.
                        // Update state to reflect the new current song.
                        updatePlaybackState()
                        updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                        Log.d(TAG, "Currently playing song deleted. Player automatically transitioned.")
                    } else {
                        // A song was deleted from the queue but not the currently playing one.
                        // Just update the state to reflect the new queue size/indices.
                        updatePlaybackState()
                        updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error removing media item from ExoPlayer: ${e.message}", e)
                    _playbackState.update { it.copy(error = "Error removing deleted song from player: ${e.message}") }
                }
            } else {
                Log.d(TAG, "Deleted audio file '${deletedAudioFile.title}' (ID: ${deletedAudioFile.id}) not found in active ExoPlayer queue. No action needed by PlayerController.")
            }
        }
    }

    override suspend fun removeFromQueue(audioFile: AudioFile) {
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available. Cannot remove from queue.")
                _playbackState.update { it.copy(error = "Player not initialized. Cannot remove from queue.") }
                return@withContext
            }

            val mediaIdToRemove = audioFile.id.toString()
            var removedIndex: Int? = null

            // Find the index of the AudioFile in the player's queue using its Media ID
            for (i in 0 until controller.mediaItemCount) {
                val mediaItem = controller.getMediaItemAt(i)
                if (mediaItem.mediaId == mediaIdToRemove) {
                    removedIndex = i
                    break
                }
            }

            if (removedIndex != null) {
                try {
                    val wasPlayingRemovedSong = (controller.currentMediaItemIndex == removedIndex) && controller.isPlaying

                    controller.removeMediaItem(removedIndex)
                    sharedAudioDataSource.removeAudioFileFromPlayingQueue(audioFile)
                    Log.d(TAG, "Removed '${audioFile.title}' from ExoPlayer queue at index $removedIndex.")

                    if (controller.mediaItemCount == 0) {
                        controller.stop()
                        controller.clearMediaItems()
                        _playbackState.update { it.copy(currentAudioFile = null, isPlaying = false) }
                        _currentAudioFilePlaybackProgress.value = CurrentAudioFilePlaybackProgress()
                        Log.d(TAG, "ExoPlayer queue is empty after removal. Stopping playback.")
                    } else if (wasPlayingRemovedSong) {
                        updatePlaybackState()
                        updateCurrentAudioFilePlaybackProgress(controller)
                        Log.d(TAG, "Currently playing song removed. Player automatically transitioned.")
                    } else {
                        updatePlaybackState()
                        updateCurrentAudioFilePlaybackProgress(controller)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing media item from ExoPlayer queue: ${e.message}", e)
                    _playbackState.update { it.copy(error = "Error removing song from queue: ${e.message}") }
                }
            } else {
                Log.d(TAG, "Audio file '${audioFile.title}' (ID: ${audioFile.id}) not found in active ExoPlayer queue. No action needed.")
                _playbackState.update { it.copy(error = "'${audioFile.title}' not found in queue to remove.") }
            }
        }
    }

    /**
     * Updates the internal [_playbackState] based on the current state of the [MediaController].
     * This method is called periodically and upon significant player events.
     */
    private fun updatePlaybackState() {
        mediaController.value?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val playingQueue = sharedAudioDataSource.playingQueueAudioFiles.value

            // Attempt to find the AudioFile in the shared queue using its ID or URI.
            // If not found (e.g., temporary item), map directly from MediaItem.
            val currentAudioFile = currentMediaItem?.let { mediaItem ->
                val mediaUri = mediaItem.localConfiguration?.uri
                val mediaId = mediaItem.mediaId

                playingQueue.find { it.id.toString() == mediaId || it.uri == mediaUri }
                    ?: audioFileMapper.mapMediaItemToAudioFile(mediaItem)
            }

            _playbackState.update {
                it.copy(
                    currentAudioFile = currentAudioFile,
                    isPlaying = controller.isPlaying,
                    playbackPositionMs = controller.currentPosition,
                    totalDurationMs = controller.duration.coerceAtLeast(0L), // Ensure non-negative duration
                    bufferedPositionMs = controller.bufferedPosition,
                    repeatMode = when (controller.repeatMode) {
                        Player.REPEAT_MODE_OFF -> RepeatMode.OFF
                        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                        else -> RepeatMode.OFF // Default to OFF if unknown
                    },
                    shuffleMode = if (controller.shuffleModeEnabled) ShuffleMode.ON else ShuffleMode.OFF,
                    playbackSpeed = controller.playbackParameters.speed,
                    isLoading = controller.playbackState == Player.STATE_BUFFERING,
                    playingQueue = playingQueue,
                    playingSongIndex = controller.currentMediaItemIndex
                )
            }
        }
    }
}
