package com.engfred.musicplayer.feature_player.data.repository.controller
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import androidx.media3.common.MediaItem

private const val TAG = "PlayerControllerImpl"

@UnstableApi
class ControllerCallback(
    private val repositoryScope: CoroutineScope,
    private val playlistRepository: PlaylistRepository,
    private val progressFlow: StateFlow<CurrentAudioFilePlaybackProgress>,
    private val stateUpdater: PlaybackStateUpdater,
    private val progressTracker: PlaybackProgressTracker,
    //Added to restore shuffle mode upon transitioning to the play next item.
    private val pendingPlayNextMediaId: MutableStateFlow<String?>,
    //Added sharedAudioDataSource to remove inaccessible files from in-memory lists in onPlayerError.
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playbackState: MutableStateFlow<PlaybackState>
) : Player.Listener {
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
            // the play event for the previous song to avoid double-counting.
            if (now - lastEventProcessedTimestamp < 100) { // 100ms threshold, adjust if needed
                Log.d(TAG, "Skipping play event processing for previous song due to rapid succession of events.")
                stateUpdater.updatePlaybackState()
                // Still update progress for the new current item to avoid stale data
                progressTracker.updateCurrentAudioFilePlaybackProgress(player as MediaController)
                lastPlaybackState = player.playbackState
                return
            }
            lastEventProcessedTimestamp = now
            // --- CRITICAL: Use the last known progress for the song that just ended/transitioned ---
            // This value comes from the _currentAudioFilePlaybackProgress flow, which is updated frequently
            // by startPlaybackPositionUpdates, giving us the most accurate "final" state of the previous song.
            val songToEvaluateProgress = progressFlow.value
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
                evaluateAndRecordPlayEvent(songToEvaluateProgress)
            } else {
                Log.d(TAG, "Skipped play event evaluation. No valid previous song data or not a significant transition. " +
                        "MediaId: ${songToEvaluateProgress.mediaId}, Duration: ${songToEvaluateProgress.totalDurationMs}, " +
                        "Played: ${songToEvaluateProgress.playbackPositionMs}, IsTransition: $isSongTransition, IsEnded: $isPlaybackEnded")
            }
            // FIX: If this transition is to the pending play next item, re-enable shuffle if it was temporarily disabled.
            if (isSongTransition && currentMediaItemAfterEvent != null) {
                if (pendingPlayNextMediaId.value == currentMediaItemAfterEvent.mediaId) {
                    (player as MediaController).shuffleModeEnabled = true
                    pendingPlayNextMediaId.value = null
                    Log.d(TAG, "Restored shuffle mode after transitioning to play next item.")
                }
            }
        }
        // Always update the overall playback state exposed to the UI
        stateUpdater.updatePlaybackState()
        // Always update progress tracker for the new current song, for the next event cycle
        progressTracker.updateCurrentAudioFilePlaybackProgress(player as MediaController)
        // Always update lastPlaybackState for detecting future transitions
        lastPlaybackState = player.playbackState
    }
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        // Grok: Added to handle song loops in REPEAT_MODE_ONE, where no MEDIA_ITEM_TRANSITION occurs,
        // but a position discontinuity is triggered when the song restarts after completing.
        // This fixes the issue where songs on repeat-one were not being counted in top played tracks.
        if (reason == Player.DISCONTINUITY_REASON_INTERNAL &&
            (oldPosition.mediaItemIndex == newPosition.mediaItemIndex) // Same media item
        ) {
            val now = System.currentTimeMillis()
            if (now - lastEventProcessedTimestamp < 100) {
                Log.d(TAG, "Skipping play event processing for discontinuity due to rapid events.")
                return
            }
            val mediaController = progressTracker.mediaController.value ?: return // To get player
            if (mediaController.repeatMode != Player.REPEAT_MODE_ONE) return // Only for repeat one
            val playedDurationMs = oldPosition.positionMs
            val totalDurationMs = mediaController.duration.coerceAtLeast(0L)
            val mediaId = mediaController.currentMediaItem?.mediaId
            if (mediaId != null && totalDurationMs > 0 && playedDurationMs != C.TIME_UNSET) {
                val progress = CurrentAudioFilePlaybackProgress(mediaId, playedDurationMs, totalDurationMs)
                evaluateAndRecordPlayEvent(progress)
                lastEventProcessedTimestamp = now
            }
        }
    }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    }
    // FIX: Added to handle playback errors (e.g., inaccessible files), remove the failing item,
    // and continue playback, ensuring all songs in shuffle mode are attempted.
    override fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "Playback error: ${error.message}", error)
        val controller = progressTracker.mediaController.value ?: return
        val currentIndex = controller.currentMediaItemIndex
        if (currentIndex != C.INDEX_UNSET) {
            val failedMediaItem = controller.getMediaItemAt(currentIndex)
            val failedMediaId = failedMediaItem.mediaId
            playbackState.update { it.copy(error = "Playback error on song: ${error.message}. Removed from queue.") }
            // Remove the failing media item from the player queue to prevent repeated errors and clean the queue.
            // This ensures the player continues with the next valid item and plays all accessible songs.
            controller.removeMediaItem(currentIndex)
            Log.d(TAG, "Removed failing media item (ID: $failedMediaId) from queue due to playback error.")
            // Also remove the inaccessible file from the shared data source to clean up the in-memory lists.
            val failedId = failedMediaId.toLongOrNull()
            if (failedId != null) {
                repositoryScope.launch {
                    // Create a dummy AudioFile with the ID for removal (other fields not needed for ID-based filter).
                    val dummyAudioFile = AudioFile(
                        id = failedId,
                        title = "",
                        artist = "",
                        album = "",
                        duration = 0L,
                        uri = Uri.EMPTY,
                        albumArtUri = null,
                        dateAdded = System.currentTimeMillis()
                    )
                    sharedAudioDataSource.deleteAudioFile(dummyAudioFile)
                    Log.d(TAG, "Removed inaccessible audio file (ID: $failedId) from shared data source.")
                    // Optional: Remove from all playlists if the file is inaccessible (uncomment if desired).
                    // playlistRepository.removeSongFromAllPlaylists(failedId)
                }
            }
        } else {
            controller.stop()
        }
        stateUpdater.updatePlaybackState()
    }
    //Extracted the play evaluation and recording logic into a separate function to avoid code duplication
    // between transition/end events and discontinuity events (for repeat-one loops). This makes the code easier to maintain
    // and remember the core algorithm for what constitutes a "significant play".
    private fun evaluateAndRecordPlayEvent(progress: CurrentAudioFilePlaybackProgress) {
        val playedDurationMs = progress.playbackPositionMs
        val totalDurationMs = progress.totalDurationMs
        if (playedDurationMs != C.TIME_UNSET) {
            val playedPercentage = playedDurationMs.toFloat() / totalDurationMs
            val minPlayDurationMs = 30 * 1000L // Minimum 30 seconds to count as a play
            // Robust condition for a "significant play":
            // Either 50% of the song was played OR at least 30 seconds were played.
            if (playedPercentage >= 0.5f || playedDurationMs >= minPlayDurationMs) {
                repositoryScope.launch {
                    val audioFileId = progress.mediaId?.toLongOrNull()
                    if (audioFileId != null) {
                        playlistRepository.recordSongPlayEvent(audioFileId)
                        Log.d(TAG, "Recorded play event for song ID: $audioFileId " +
                                "(Played: ${playedDurationMs / 1000}s / ${totalDurationMs / 1000}s, " +
                                "Percentage: ${"%.2f".format(playedPercentage * 100)}%)")
                    } else {
                        Log.e(TAG, "Could not convert mediaId to AudioFile ID: ${progress.mediaId}")
                    }
                }
            } else {
                Log.d(TAG, "Skipped recording play event for song ID: ${progress.mediaId} " +
                        "(Insignificant play: ${playedDurationMs / 1000}s / ${totalDurationMs / 1000}s, " +
                        "Percentage: ${"%.2f".format(playedPercentage * 100)}%)")
            }
        }
    }
}