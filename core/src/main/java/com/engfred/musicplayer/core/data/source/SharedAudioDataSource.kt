package com.engfred.musicplayer.core.data.source

import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton data source that holds the globally available list of all audio files.
 * This acts as a central hub for sharing the loaded audio files across different modules
 * and components without re-fetching.
 */
@Singleton
class SharedAudioDataSource @Inject constructor() {

    private val _allAudioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val allAudioFiles: StateFlow<List<AudioFile>> = _allAudioFiles.asStateFlow()

    /**
     * Updates the globally available list of audio files.
     * This method should be called by the component that is responsible for loading the files,
     * e.g., LibraryViewModel after a successful scan.
     */
    fun setAudioFiles(audioFiles: List<AudioFile>) {
        if (_allAudioFiles.value != audioFiles) { // Avoid unnecessary updates
            _allAudioFiles.value = audioFiles
            android.util.Log.d("SharedAudioDataSource", "Updated shared audio file list with ${audioFiles.size} items.")
        }
    }

    /**
     * Clears the current list of audio files.
     */
    fun clearAudioFiles() {
        _allAudioFiles.value = emptyList()
        android.util.Log.d("SharedAudioDataSource", "Cleared shared audio file list.")
    }
}