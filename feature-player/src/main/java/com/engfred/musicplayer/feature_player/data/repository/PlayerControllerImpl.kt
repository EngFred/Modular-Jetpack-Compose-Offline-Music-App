package com.engfred.musicplayer.feature_player.data.repository

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.engfred.musicplayer.core.data.session.MediaControllerProvider
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
import com.engfred.musicplayer.core.mapper.AudioFileMapper
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
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val audioFileMapper: AudioFileMapper,
    mediaControllerProvider: MediaControllerProvider
) : PlayerController {

    private val mediaControllerFlow = mediaControllerProvider.mediaController

    private val _playbackState = MutableStateFlow(PlaybackState())
    override fun getPlaybackState(): Flow<PlaybackState> = _playbackState.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val controllerCallback = ControllerCallback()

    private var attachedController: MediaController? = null

    init {
        Log.d(TAG, "Initializing PlayerControllerImpl")
        repositoryScope.launch {
            mediaControllerFlow.collectLatest { newController ->
                withContext(Dispatchers.Main) {
                    attachedController?.removeListener(controllerCallback)

                    if (newController != null) {
                        newController.addListener(controllerCallback)
                        attachedController = newController
                        Log.d(TAG, "PlayerControllerImpl received and attached to shared MediaController.")
                        updatePlaybackState()
                    } else {
                        attachedController = null
                        Log.w(TAG, "PlayerControllerImpl received null MediaController. Releasing player state.")
                        _playbackState.update { PlaybackState() }
                    }
                }
            }
        }
        repositoryScope.launch {
            startPlaybackPositionUpdates()
        }
    }

    private suspend fun startPlaybackPositionUpdates() {
        while (true) {
            withContext(Dispatchers.Main) {
                mediaControllerFlow.value?.let { actualController ->
                    if (actualController.playbackState != Player.STATE_IDLE && actualController.playbackState != Player.STATE_ENDED) {
                        updatePlaybackState()
                    }
                }
            }
            delay(500)
        }
    }

    override suspend fun initiatePlayback(initialAudioFileUri: Uri) {
        withContext(Dispatchers.Main) {
            val controller = mediaControllerFlow.value
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

            // OPTIONAL: Check if the player's current queue is already the same as allAudioFiles
            // This prevents re-setting the media items unnecessarily if the user clicks a song
            // that is already part of the currently loaded "library" queue.
            val currentMediaItemsMatchSharedSource = controller.mediaItemCount == allAudioFiles.size &&
                    (0 until allAudioFiles.size).all { i ->
                        controller.getMediaItemAt(i).mediaId == audioFileMapper.mapAudioFileToMediaItem(allAudioFiles[i]).mediaId
                    }

            if (currentMediaItemsMatchSharedSource && controller.currentMediaItemIndex == startIndex) {
                Log.d(TAG, "Player queue already matches shared data source and is at the correct song. Just playing.")
                if (!controller.isPlaying) {
                    controller.play()
                }
                updatePlaybackState()
                return@withContext
            } else if (currentMediaItemsMatchSharedSource) {
                Log.d(TAG, "Player queue matches shared data source, but starting index is different. Seeking and playing.")
                controller.seekToDefaultPosition(startIndex)
                if (!controller.isPlaying) {
                    controller.play()
                }
                updatePlaybackState()
                return@withContext
            }

            try {
                val mediaItems = allAudioFiles.map { audioFileMapper.mapAudioFileToMediaItem(it) }
                controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                controller.prepare()
                controller.play()
                Log.d(TAG, "Initiated playback for: $initialAudioFileUri by setting new media items from shared source.")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting media items or playing during initiation: ${e.message}", e)
                _playbackState.update { it.copy(error = "Playback error: ${e.message}") }
            }
        }
    }

    override suspend fun playPause() {
        withContext(Dispatchers.Main) {
            mediaControllerFlow.value?.run {
                if (isPlaying) pause() else play()
            } ?: Log.w(TAG, "MediaController not set when trying to play/pause.")
        }
    }

    override suspend fun skipToNext() {
        withContext(Dispatchers.Main) {
            mediaControllerFlow.value?.seekToNextMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip next.")
        }
    }

    override suspend fun skipToPrevious() {
        withContext(Dispatchers.Main) {
            mediaControllerFlow.value?.seekToPreviousMediaItem() ?: Log.w(TAG, "MediaController not set when trying to skip previous.")
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(Dispatchers.Main) {
            mediaControllerFlow.value?.let { controller ->
                controller.seekTo(positionMs)
                updatePlaybackState()
            } ?: Log.w(TAG, "MediaController not set when trying to seek.")
        }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        withContext(Dispatchers.Main) {
            mediaControllerFlow.value?.let { controller ->
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
            mediaControllerFlow.value?.let { controller ->
                controller.shuffleModeEnabled = (mode == ShuffleMode.ON)
            } ?: Log.w(TAG, "MediaController not set when trying to set shuffle mode.")
        }
    }

    override suspend fun addAudioToQueueNext(audioFile: AudioFile) {
        withContext(Dispatchers.Main) {
            val controller = mediaControllerFlow.value
            if (controller == null) {
                Log.e(TAG, "MediaController not available. Cannot add to queue.")
                _playbackState.update { it.copy(error = "Player not initialized. Cannot add to queue.") }
                return@withContext
            }

            val mediaItemToAdd = audioFileMapper.mapAudioFileToMediaItem(audioFile)
            val newItemMediaId = mediaItemToAdd.mediaId

            Log.d(TAG, "Attempting to 'Play Next': Title='${audioFile.title}', AudioFile.ID='${audioFile.id}', NewItemMediaId='$newItemMediaId'")

            // IMPORTANT: Removed the duplicate check here for "Play Next" functionality.
            // The intent is to always insert this song immediately after the current one,
            // even if it exists elsewhere in a potentially long queue (like the full library).
            // This allows the user to explicitly force a song to play next.

            val currentMediaItemIndex = controller.currentMediaItemIndex
            val insertIndex = if (currentMediaItemIndex == C.INDEX_UNSET || controller.mediaItemCount == 0) {
                0 // If nothing playing or empty queue, add to start
            } else {
                currentMediaItemIndex + 1 // Add after the current item
            }

            try {
                controller.addMediaItem(insertIndex, mediaItemToAdd)
                Log.d(TAG, "Added ${audioFile.title} (ID: ${audioFile.id}) to queue at index $insertIndex (Play Next).")

                // If this was the very first item added to an empty queue, start playback
                if (controller.mediaItemCount == 1 && !controller.isPlaying && controller.playbackState == Player.STATE_IDLE) {
                    controller.prepare()
                    controller.play()
                    Log.d(TAG, "Started playback as it was the first item in the queue.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding media item to queue: ${e.message}", e)
                _playbackState.update { it.copy(error = "Error adding song to queue: ${e.message}") }
            }
        }
    }

    override suspend fun releasePlayer() {
        repositoryScope.cancel()
        withContext(Dispatchers.Main) {
            attachedController?.removeListener(controllerCallback)
            attachedController = null
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
                    Player.EVENT_PLAYER_ERROR,
                    Player.EVENT_TIMELINE_CHANGED
                )
            ) {
                updatePlaybackState()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update { it.copy(error = error.message ?: "Player error") }
            Log.e(TAG, "Playback error from ExoPlayer: ${error.message}", error)
        }
    }

    private fun updatePlaybackState() {
        mediaControllerFlow.value?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val allSharedAudioFiles = sharedAudioDataSource.allAudioFiles.value

            val currentAudioFile = currentMediaItem?.let { mediaItem ->
                val mediaUri = mediaItem.localConfiguration?.uri
                val mediaId = mediaItem.mediaId

                allSharedAudioFiles.find { it.id.toString() == mediaId || it.uri == mediaUri }
                    ?: audioFileMapper.mapMediaItemToAudioFile(mediaItem)
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