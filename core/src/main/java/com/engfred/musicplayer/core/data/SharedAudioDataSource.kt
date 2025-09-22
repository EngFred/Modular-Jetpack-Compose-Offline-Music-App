package com.engfred.musicplayer.core.data

import android.util.Log
import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton data source that holds the globally available list of all audio files.
 * This acts as a central hub for sharing the loaded audio files across different modules
 * and components without re-fetching.
 */
@Singleton
class SharedAudioDataSource @Inject constructor() {

    private val _playingQueueAudioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val playingQueueAudioFiles: StateFlow<List<AudioFile>> = _playingQueueAudioFiles.asStateFlow()

    private val _deviceAudioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val deviceAudioFiles: StateFlow<List<AudioFile>> = _deviceAudioFiles.asStateFlow()


    /**
     * Updates the globally available list of audio files.
     */
    fun setDeviceAudioFiles(audioFiles: List<AudioFile>) {
        _deviceAudioFiles.value = audioFiles
    }

    /**
     * Updates the globally available list of audio files to be used as the playing queue.
     * This method is called by the component that is responsible for loading the files,
     * e.g., LibraryViewModel after a successful scan.
     */
    fun setPlayingQueue(audioFiles: List<AudioFile>) {
        if (_playingQueueAudioFiles.value != audioFiles) {
            _playingQueueAudioFiles.value = audioFiles
            Log.d("SharedAudioDataSource", "Updated shared audio file list with ${audioFiles.size} items.")
        }
    }

    /**
     * Clears the current playing queue.
     */
    fun clearPlayingQueue() {
        _playingQueueAudioFiles.value = emptyList()
        Log.d("SharedAudioDataSource", "Cleared shared audio file list.")
    }

    /**
     * Removes the specified audio file from the in-memory lists held by this data source.
     * This method does NOT delete the actual file from the device's storage (MediaStore).
     * The actual file deletion from disk is handled by a separate mechanism.
     */
    fun deleteAudioFile(audioFile: AudioFile) {
        val updatedDeviceFiles = _deviceAudioFiles.value.filterNot { it.id == audioFile.id }
        _deviceAudioFiles.value = updatedDeviceFiles

        val updatedPlayingQueue = _playingQueueAudioFiles.value.filterNot { it.id == audioFile.id }
        _playingQueueAudioFiles.value = updatedPlayingQueue

        Log.d("SharedAudioDataSource", "Removed audio file with id: ${audioFile.id}")
    }

    fun removeAudioFileFromPlayingQueue(audioFile: AudioFile) {
        val updatedPlayingQueue = _playingQueueAudioFiles.value.filterNot { it.id == audioFile.id }
        _playingQueueAudioFiles.value = updatedPlayingQueue
    }

    fun updateAudioFile(updatedAudio: AudioFile) {
        _deviceAudioFiles.update { list ->
            list.map { if (it.id == updatedAudio.id) updatedAudio else it }
        }
        _playingQueueAudioFiles.update { queue ->
            queue.map { if (it.id == updatedAudio.id) updatedAudio else it }
        }
        Log.d("SharedAudioDataSource", "Updated audio file with id: ${updatedAudio.id}")
    }
}