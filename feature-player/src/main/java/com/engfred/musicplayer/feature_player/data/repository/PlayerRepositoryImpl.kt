package com.engfred.musicplayer.feature_player.data.repository

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.engfred.musicplayer.feature_library.domain.model.AudioFile
import com.engfred.musicplayer.feature_player.data.service.AudioFileMapper
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import com.engfred.musicplayer.feature_player.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Concrete implementation of PlayerRepository that interacts with the MusicService
 * via a MediaController to control playback.
 */
@OptIn(UnstableApi::class)
// Removed @RequiresApi(Build.VERSION_CODES.P) as MediaController handles API level compatibility
// Changed scope to Singleton as MediaController is a singleton provided by PlayerModule
@UnstableApi
class PlayerRepositoryImpl @Inject constructor(
    private val audioFileMapper: AudioFileMapper
) : PlayerRepository {

    private var mediaController: MediaController? = null
    private val _currentPlaybackState = MutableStateFlow(PlaybackState())

    override fun setMediaController(mediaController: MediaController) {
        this.mediaController = mediaController
        mediaController.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updatePlaybackState()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                updatePlaybackState()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                updatePlaybackState()
            }

            override fun onPlayerError(error: PlaybackException) {
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = error.message ?: "Player error"
                )
                Log.e("PlayerRepositoryImpl", "Playback error: ${error.message}")
                // FirebaseCrashlytics.getInstance().recordException(error)
            }
        })
        updatePlaybackState()
    }

    override fun getPlaybackState(): Flow<PlaybackState> {
        return _currentPlaybackState.asStateFlow()
    }

    override suspend fun setMediaItems(audioFileUris: List<Uri>, startIndex: Int) {
        mediaController?.let { controller ->
            try {
                val mediaItems = audioFileUris.map { uri ->
                    MediaItem.Builder().setUri(uri).build()
                }
                controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                controller.prepare()
                Log.d("PlayerRepositoryImpl", "Set ${mediaItems.size} media items, startIndex: $startIndex")
            } catch (e: Exception) {
                Log.e("PlayerRepositoryImpl", "Failed to set media items: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = "Failed to set media items: ${e.message}"
                )
            }
        } ?: run {
            Log.w("PlayerRepositoryImpl", "MediaController not set")
            _currentPlaybackState.value = _currentPlaybackState.value.copy(
                error = "MediaController not initialized"
            )
        }
    }

    override suspend fun addMediaItems(audioFileUris: List<Uri>) {
        mediaController?.let { controller ->
            try {
                val mediaItems = audioFileUris.map { uri ->
                    MediaItem.Builder().setUri(uri).build()
                }
                controller.addMediaItems(mediaItems)
                Log.d("PlayerRepositoryImpl", "Added ${mediaItems.size} media items to playlist")
            } catch (e: Exception) {
                Log.e("PlayerRepositoryImpl", "Failed to add media items: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
                _currentPlaybackState.value = _currentPlaybackState.value.copy(
                    error = "Failed to add media items: ${e.message}"
                )
            }
        } ?: run {
            Log.w("PlayerRepositoryImpl", "MediaController not set")
            _currentPlaybackState.value = _currentPlaybackState.value.copy(
                error = "MediaController not initialized"
            )
        }
    }

    override fun play() {
        mediaController?.play() ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun pause() {
        mediaController?.pause() ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun playPause() {
        mediaController?.run {
            if (isPlaying) pause() else play()
        } ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun skipToNext() {
        mediaController?.seekToNextMediaItem() ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem() ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs) ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun seekToItem(index: Int) {
        mediaController?.seekTo(index, C.TIME_UNSET) ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun setRepeatMode(mode: RepeatMode) {
        mediaController?.let { controller ->
            controller.repeatMode = when (mode) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            }
        } ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun setShuffleMode(mode: ShuffleMode) {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = (mode == ShuffleMode.ON)
        } ?: Log.w("PlayerRepositoryImpl", "MediaController not set")
    }

    override fun releasePlayer() {
        mediaController?.let { controller ->
            controller.release()
            mediaController = null
            Log.d("PlayerRepositoryImpl", "MediaController released")
        }
    }

    private fun updatePlaybackState() {
        mediaController?.let { controller ->
            try {
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
            } catch (e: Exception) {
                Log.e("PlayerRepositoryImpl", "Update playback state failed: ${e.message}")
                // FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}