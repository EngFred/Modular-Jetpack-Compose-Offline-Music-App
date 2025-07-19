package com.engfred.musicplayer.feature_library.domain.repository

import com.engfred.musicplayer.feature_library.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to audio files.
 * This interface is part of the domain layer, making it independent of data source implementation.
 */
interface AudioFileRepository {
    /**
     * Fetches all audio files from the device.
     * @return A Flow emitting a list of AudioFile objects.
     */
    fun getAllAudioFiles(): Flow<List<AudioFile>>

    /**
     * Searches for audio files based on a query string.
     * @param query The search query (e.g., song title, artist, album).
     * @return A Flow emitting a list of matching AudioFile objects.
     */
    fun searchAudioFiles(query: String): Flow<List<AudioFile>>
}