package com.engfred.musicplayer.core.domain.repository

import android.content.Context
import android.net.Uri
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.common.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to audio files.
 */
interface LibraryRepository {
    /**
     * Fetches all audio files from the device.
     * @return A Flow emitting a list of AudioFile objects.
     */
    fun getAllAudioFiles(): Flow<List<AudioFile>>

    /**
     * Fetch a single AudioFile by its content Uri.
     * Returns Resource.Success(AudioFile) if found, or Resource.Error(...) on failure / not found.
     */
    suspend fun getAudioFileByUri(uri: Uri): Resource<AudioFile>

    /**
     * Edits the metadata of an audio file.
     */
    suspend fun editAudioMetadata(
        id: Long,
        newTitle: String?,
        newArtist: String?,
        newAlbumArt: ByteArray?,
        context: Context
    ): Resource<Unit>
}
