package com.engfred.musicplayer.core.domain.repository

import android.content.Context
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.common.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to audio files.
 * This interface is part of the domain layer, making it independent of data source implementation.
 */
interface LibraryRepository {
    /**
     * Fetches all audio files from the device.
     * @return A Flow emitting a list of AudioFile objects.
     */
    fun getAllAudioFiles(): Flow<List<AudioFile>>

    /**
     * Edits the metadata of an audio file.
     * @param id The ID of the audio file.
     * @param newTitle The new title, or null to skip.
     * @param newArtist The new artist, or null to skip.
     * @param newAlbumArt The new album art bytes, or null to skip.
     * @return Resource indicating success or error.
     */
    suspend fun editAudioMetadata(
        id: Long,
        newTitle: String?,
        newArtist: String?,
        newAlbumArt: ByteArray?,
        context: Context
    ): Resource<Unit>
}