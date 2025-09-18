package com.engfred.musicplayer.feature_player.data.service

import android.util.Log
import com.engfred.musicplayer.core.data.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import com.engfred.musicplayer.core.domain.model.LastPlaybackState
import kotlinx.coroutines.flow.first

private const val TAG = "PlaybackActions"

/**
 * Extracted widget-play/pause and repeat toggle logic.
 * These are pure helpers that match the original behavior exactly.
 */
object PlaybackActions {

    fun handleRepeatToggle(exoPlayer: ExoPlayer, settingsRepository: SettingsRepository, scope: CoroutineScope) {
        val current = exoPlayer.repeatMode
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> {
                scope.launch { settingsRepository.updateRepeatMode(RepeatMode.ALL) }
                Player.REPEAT_MODE_ALL
            }
            Player.REPEAT_MODE_ALL -> {
                scope.launch { settingsRepository.updateRepeatMode(RepeatMode.ONE) }
                Player.REPEAT_MODE_ONE
            }
            Player.REPEAT_MODE_ONE -> {
                scope.launch { settingsRepository.updateRepeatMode(RepeatMode.OFF) }
                Player.REPEAT_MODE_OFF
            }
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = next
    }

    fun handlePlayPauseFromWidget(
        exoPlayer: ExoPlayer,
        libRepo: LibraryRepository,
        settingsRepository: SettingsRepository,
        sharedAudioDataSource: SharedAudioDataSource,
        playbackController: PlaybackController,
        serviceScope: CoroutineScope
    ) {
        try {
            serviceScope.launch {
                if (exoPlayer.mediaItemCount == 0) {
                    val lastState = settingsRepository.getLastPlaybackState().first()
                    val deviceAudios = libRepo.getAllAudioFiles().first()

                    if (deviceAudios.isNotEmpty()) {
                        val filter = settingsRepository.getFilterOption().first()
                        val appSettings = settingsRepository.getAppSettings().first()
                        val repeatMode = appSettings.repeatMode
                        val sortedAudios = when (filter) {
                            FilterOption.DATE_ADDED_ASC -> deviceAudios.sortedBy { it.dateAdded }
                            FilterOption.DATE_ADDED_DESC -> deviceAudios.sortedByDescending { it.dateAdded }
                            FilterOption.LENGTH_ASC -> deviceAudios.sortedBy { it.duration }
                            FilterOption.LENGTH_DESC -> deviceAudios.sortedByDescending { it.duration }
                            FilterOption.ALPHABETICAL_ASC -> deviceAudios.sortedBy { it.title.lowercase() }
                            FilterOption.ALPHABETICAL_DESC -> deviceAudios.sortedByDescending { it.title.lowercase() }
                        }
                        Log.d(TAG, "Applied sort order: $filter to create queue of ${sortedAudios.size} items")

                        val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                            val idToAudio = deviceAudios.associateBy { it.id }
                            ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sortedAudios
                        } ?: sortedAudios

                        val isResuming = lastState.audioId != null
                        var audioToPlay: AudioFile? = lastState.audioId?.let { id ->
                            playingQueue.find { it.id == id }
                        }
                        val resumePositionMs = if (audioToPlay != null) lastState.positionMs else 0L

                        if (audioToPlay == null) {
                            audioToPlay = playingQueue.firstOrNull()
                            if (isResuming) {
                                settingsRepository.saveLastPlaybackState(LastPlaybackState(null))
                                Log.w(TAG, "Last audio ID ${lastState.audioId} not found; cleared state and falling back to first song")
                            }
                        }

                        if (audioToPlay != null) {
                            sharedAudioDataSource.setPlayingQueue(playingQueue)
                            playbackController.setRepeatMode(repeatMode)
                            playbackController.setShuffleMode(ShuffleMode.OFF)
                            Log.d(TAG, "Set repeat: $repeatMode, shuffle: OFF for playback")

                            playbackController.initiatePlayback(audioToPlay.uri)

                            if (resumePositionMs > 0) {
                                val startTime = System.currentTimeMillis()
                                while (exoPlayer.playbackState != Player.STATE_READY && System.currentTimeMillis() - startTime < 10000L) {
                                    delay(100)
                                }
                                val isReady = exoPlayer.playbackState == Player.STATE_READY
                                if (isReady) {
                                    exoPlayer.seekTo(resumePositionMs)
                                    Log.d(TAG, "Seeked to resume position ${resumePositionMs}ms")
                                } else {
                                    Log.w(TAG, "Player not ready in time for resume seek; continuing from start")
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "No device audios available; cannot start playback")
                    }
                } else {
                    playbackController.playPause()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handlePlayPauseFromWidget: ${e.message}", e)
        }
    }
}
