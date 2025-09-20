package com.engfred.musicplayer.helpers

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.engfred.musicplayer.core.data.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.media3.common.C

private const val TAG = "PlaybackQueueHelper"

object PlaybackQueueHelper {

    fun sortAudioFiles(audioFiles: List<AudioFile>, filter: FilterOption): List<AudioFile> {
        return when (filter) {
            FilterOption.DATE_ADDED_ASC -> audioFiles.sortedBy { it.dateAdded }
            FilterOption.DATE_ADDED_DESC -> audioFiles.sortedByDescending { it.dateAdded }
            FilterOption.LENGTH_ASC -> audioFiles.sortedBy { it.duration }
            FilterOption.LENGTH_DESC -> audioFiles.sortedByDescending { it.duration }
            FilterOption.ALPHABETICAL_ASC -> audioFiles.sortedBy { it.title.lowercase() }
            FilterOption.ALPHABETICAL_DESC -> audioFiles.sortedByDescending { it.title.lowercase() }
        }
    }

    fun playAll(
        context: Context,
        sharedAudioDataSource: SharedAudioDataSource,
        playbackController: PlaybackController,
        settingsRepository: SettingsRepository
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val audioFiles = sharedAudioDataSource.deviceAudioFiles.value
            if (audioFiles.isEmpty()) {
                Toast.makeText(context, "No audio files found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val lastState = settingsRepository.getLastPlaybackState().first()
            val appSettings = settingsRepository.getAppSettings().first()
            val repeat = appSettings.repeatMode
            val filter = settingsRepository.getFilterOption().first()
            val sorted = sortAudioFiles(audioFiles, filter)
            val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                val idToAudio = audioFiles.associateBy { it.id }
                ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sorted
            } ?: sorted
            sharedAudioDataSource.setPlayingQueue(playingQueue)
            playbackController.setRepeatMode(repeat)
            playbackController.setShuffleMode(ShuffleMode.OFF)
            val startAudio = lastState.audioId?.let { id ->
                playingQueue.find { it.id == id }
            }
            val startUri = startAudio?.uri ?: playingQueue.firstOrNull()?.uri
            if (startUri != null) {
                val resumePosition = if (startAudio != null && lastState.positionMs > 0) lastState.positionMs else C.TIME_UNSET
                Log.d(TAG, "Starting playback with URI: $startUri (resumePos=$resumePosition)")
                playbackController.initiatePlayback(startUri, resumePosition)
                if (startAudio != null) {
                    Toast.makeText(context, "Resumed playback", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No audio files found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shuffleAll(
        context: Context,
        sharedAudioDataSource: SharedAudioDataSource,
        playbackController: PlaybackController,
        settingsRepository: SettingsRepository
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val audioFiles = sharedAudioDataSource.deviceAudioFiles.value
            if (audioFiles.isEmpty()) {
                Toast.makeText(context, "No audio files found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val lastState = settingsRepository.getLastPlaybackState().first()
            val appSettings = settingsRepository.getAppSettings().first()
            val repeat = appSettings.repeatMode
            val filter = settingsRepository.getFilterOption().first()
            val sorted = sortAudioFiles(audioFiles, filter)
            val playingQueue = lastState.queueIds?.takeIf { it.isNotEmpty() }?.let { ids ->
                val idToAudio = audioFiles.associateBy { it.id }
                ids.mapNotNull { idToAudio[it] }.takeIf { it.isNotEmpty() } ?: sorted
            } ?: sorted
            sharedAudioDataSource.setPlayingQueue(playingQueue)
            playbackController.setRepeatMode(repeat)
            // pass resume position into shuffle initiation
//            val resumePosition = if (lastState.positionMs > 0) lastState.positionMs else C.TIME_UNSET
            playbackController.initiateShufflePlayback(playingQueue)
        }
    }
}
