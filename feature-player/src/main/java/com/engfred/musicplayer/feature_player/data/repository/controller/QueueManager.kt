package com.engfred.musicplayer.feature_player.data.repository.controller

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.util.MediaUtils.isAudioFileAccessible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val TAG = "PlayerControllerImpl"

class QueueManager(
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val audioFileMapper: AudioFileMapper,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val context: Context,
    private val mediaController: MutableStateFlow<MediaController?>,
    private val playbackState: MutableStateFlow<PlaybackState>,
    private val stateUpdater: PlaybackStateUpdater,
    private val progressTracker: PlaybackProgressTracker,
    private val setRepeatCallback: suspend (RepeatMode) -> Unit,
    private val setShuffleCallback: suspend (ShuffleMode) -> Unit,
    // FIX: Added to manage restoring shuffle mode after play next insertion.
    private val pendingPlayNextMediaId: MutableStateFlow<String?>,
) {
    /**
     * Initiates playback of a given audio file within the current playing queue.
     * If the queue doesn't match the controller's queue, it will be set.
     * @param initialAudioFileUri The [android.net.Uri] of the audio file to start playback from.
     */
    suspend fun initiatePlayback(initialAudioFileUri: android.net.Uri, intendedRepeat: RepeatMode, intendedShuffle: ShuffleMode) {
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available for playback initiation.")
                playbackState.update { it.copy(error = "Player not initialized.") }
                return@withContext
            }

            val playingQueue = sharedAudioDataSource.playingQueueAudioFiles.value
            if (playingQueue.isEmpty()) {
                Log.w(TAG, "Shared audio files are empty. Cannot initiate playback.")
                playbackState.update { it.copy(error = "No audio files available to play.") }
                return@withContext
            }

            // Find the index of the initial audio file in the current playing queue
            val audioFileToPlay = playingQueue.find { it.uri == initialAudioFileUri } ?: run {
                Log.w(TAG, "Initial audio file not found in current playing queue for URI: $initialAudioFileUri")
                playbackState.update { it.copy(error = "Selected song not found in library.") }
                return@withContext
            }

            val desiredMediaId = audioFileToPlay.id.toString()
            val startIndex = playingQueue.indexOf(audioFileToPlay)

            // Pre-playback accessibility check: ensure the file exists and is readable
            val isAccessible = isAudioFileAccessible(
                context = context,
                audioFileUri = audioFileToPlay.uri,
                permissionHandlerUseCase = permissionHandlerUseCase
            )
            if (!isAccessible) {
                Log.e(TAG, "Audio file is not accessible: ${audioFileToPlay.uri}. Aborting playback.")
                playbackState.update {
                    it.copy(
                        currentAudioFile = null,
                        isPlaying = false,
                        error = "Cannot play '${audioFileToPlay.title}'. File not found or storage permission denied."
                    )
                }
                return@withContext
            }

            // FIX: Changed matching logic to use sets of media IDs instead of ordered comparison.
            // This ensures correct matching even when shuffle mode is enabled and the timeline order differs.
            val currentMediaIds = HashSet<String>().apply {
                for (i in 0 until controller.mediaItemCount) {
                    add(controller.getMediaItemAt(i).mediaId)
                }
            }
            val sharedMediaIds = HashSet<String>().apply {
                playingQueue.forEach { add(it.id.toString()) }
            }
            val currentMediaItemsMatchSharedSource = controller.mediaItemCount == playingQueue.size &&
                    currentMediaIds == sharedMediaIds

            if (currentMediaItemsMatchSharedSource) {
                // FIX: If queues match, handle repositioning appropriately, with reshuffle if needed.
                val currentMediaId = controller.currentMediaItem?.mediaId
                if (currentMediaId == desiredMediaId && controller.isPlaying) {
                    Log.d(TAG, "Already playing the desired song. No action needed.")
                    return@withContext
                }

                // Temporarily disable shuffle to seek to the original index, then re-enable to reshuffle from the new position.
                val wasShuffle = controller.shuffleModeEnabled
                controller.shuffleModeEnabled = false
                controller.seekToDefaultPosition(startIndex)
                controller.shuffleModeEnabled = wasShuffle

                if (!controller.isPlaying) {
                    controller.play()
                }
                stateUpdater.updatePlaybackState()
                progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
                Log.d(TAG, "Repositioned playback within existing queue to index $startIndex.")
            } else {
                // FIX: When setting a new queue, filter to only accessible files to prevent playback errors.
                val accessibleQueue = playingQueue.filter {
                    isAudioFileAccessible(context, it.uri, permissionHandlerUseCase)
                }

                if (accessibleQueue.isEmpty()) {
                    playbackState.update { it.copy(error = "No accessible audio files in the playlist.") }
                    return@withContext
                }

                var adjustedStartIndex = accessibleQueue.indexOf(audioFileToPlay)
                if (adjustedStartIndex == -1) {
                    // If the original starting file is inaccessible (though checked earlier), fall back to first.
                    adjustedStartIndex = 0
                }

                // Set the filtered queue.
                try {
                    val mediaItems = accessibleQueue.map { audioFileMapper.mapAudioFileToMediaItem(it) }
                    controller.setMediaItems(mediaItems, adjustedStartIndex, C.TIME_UNSET)

                    // ---------------------------------------------
                    // NEW: Re-apply stored repeat and shuffle modes to ensure they're set for new playback
                    // sessions, especially when the playlist changes.
                    // ---------------------------------------------
                    setRepeatCallback(intendedRepeat)
                    setShuffleCallback(intendedShuffle)

                    controller.prepare()
                    controller.play()
                    Log.d(TAG, "Initiated playback for: $initialAudioFileUri by setting new media items from shared source.")
                    progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting media items or playing during initiation: ${e.message}", e)
                    playbackState.update { it.copy(error = "Playback error: ${e.message}") }
                }
            }
        }
    }

    /**
     * Adds an [AudioFile] to the player's queue right after the currently playing song.
     * @param audioFile The [AudioFile] to add to the queue.
     */
    suspend fun addAudioToQueueNext(audioFile: AudioFile) {
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available. Cannot add to queue.")
                playbackState.update { it.copy(error = "Player not initialized. Cannot add to queue.") }
                return@withContext
            }

            // Pre-add to queue accessibility check
            val isAccessible = isAudioFileAccessible(context, audioFile.uri, permissionHandlerUseCase)
            if (!isAccessible) {
                Log.e(TAG, "Audio file is not accessible for 'Play Next': ${audioFile.uri}. Aborting add.")
                playbackState.update { it.copy(error = "Cannot add '${audioFile.title}'. File not found or storage permission denied.") }
                return@withContext
            }

            val mediaItemToAdd = audioFileMapper.mapAudioFileToMediaItem(audioFile)
            val newItemMediaId = mediaItemToAdd.mediaId
            Log.d(TAG, "Attempting to 'Play Next': Title='${audioFile.title}', AudioFile.ID='${audioFile.id}', NewItemMediaId='$newItemMediaId'")

            // FIX: Temporarily disable shuffle to insert exactly next, restore later via callback if was enabled.
            val wasShuffle = controller.shuffleModeEnabled
            controller.shuffleModeEnabled = false

            val currentMediaItemIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentMediaItemIndex == C.INDEX_UNSET || controller.mediaItemCount == 0) {
                0
            } else {
                currentMediaItemIndex + 1
            }

            try {
                controller.addMediaItem(insertIndex, mediaItemToAdd)
                if (wasShuffle) {
                    pendingPlayNextMediaId.value = newItemMediaId
                }

                Log.d(TAG, "Added ${audioFile.title} (ID: ${audioFile.id}) to queue at index $insertIndex (Play Next).")

                // If this was the very first item added and the player was idle, start playback
                if (controller.mediaItemCount == 1 && !controller.isPlaying && controller.playbackState == Player.STATE_IDLE) {
                    controller.prepare()
                    controller.play()
                    Log.d(TAG, "Started playback as it was the first item in the queue.")
                    progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error adding media item to queue: ${e.message}", e)
                playbackState.update { it.copy(error = "Error adding song to queue: ${e.message}") }
            } finally {
                // Note: Shuffle is left disabled if was enabled; it will be re-enabled in the callback on transition.
            }
        }
    }

    /**
     * Handles the event when an audio file is removed from the device's storage.
     * It attempts to remove the corresponding media item from the player's queue.
     * @param deletedAudioFile The [AudioFile] that was removed.
     */
    suspend fun onAudioFileRemoved(deletedAudioFile: AudioFile) {
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
                        playbackState.update { it.copy(currentAudioFile = null, isPlaying = false) }
                        progressTracker.resetProgress() // Reset progress tracker
                        Log.d(TAG, "ExoPlayer queue is empty after deletion. Stopping playback.")
                    } else if (wasPlayingDeletedSong) {
                        // If the currently playing song was deleted, the player automatically transitions.
                        // Update state to reflect the new current song.
                        stateUpdater.updatePlaybackState()
                        progressTracker.updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                        Log.d(TAG, "Currently playing song deleted. Player automatically transitioned.")
                    } else {
                        // A song was deleted from the queue but not the currently playing one.
                        // Just update the state to reflect the new queue size/indices.
                        stateUpdater.updatePlaybackState()
                        progressTracker.updateCurrentAudioFilePlaybackProgress(controller) // Ensure progress tracker is updated
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing media item from ExoPlayer: ${e.message}", e)
                    playbackState.update { it.copy(error = "Error removing deleted song from player: ${e.message}") }
                }
            } else {
                Log.d(TAG, "Deleted audio file '${deletedAudioFile.title}' (ID: ${deletedAudioFile.id}) not found in active ExoPlayer queue. No action needed by PlayerController.")
            }
        }
    }

    suspend fun removeFromQueue(audioFile: AudioFile) {
        withContext(Dispatchers.Main) {
            val controller = mediaController.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available. Cannot remove from queue.")
                playbackState.update { it.copy(error = "Player not initialized. Cannot remove from queue.") }
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
                        playbackState.update { it.copy(currentAudioFile = null, isPlaying = false) }
                        progressTracker.resetProgress()
                        Log.d(TAG, "ExoPlayer queue is empty after removal. Stopping playback.")
                    } else if (wasPlayingRemovedSong) {
                        stateUpdater.updatePlaybackState()
                        progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
                        Log.d(TAG, "Currently playing song removed. Player automatically transitioned.")
                    } else {
                        stateUpdater.updatePlaybackState()
                        progressTracker.updateCurrentAudioFilePlaybackProgress(controller)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing media item from ExoPlayer queue: ${e.message}", e)
                    playbackState.update { it.copy(error = "Error removing song from queue: ${e.message}") }
                }
            } else {
                Log.d(TAG, "Audio file '${audioFile.title}' (ID: ${audioFile.id}) not found in active ExoPlayer queue. No action needed.")
                playbackState.update { it.copy(error = "'${audioFile.title}' not found in queue to remove.") }
            }
        }
    }
}