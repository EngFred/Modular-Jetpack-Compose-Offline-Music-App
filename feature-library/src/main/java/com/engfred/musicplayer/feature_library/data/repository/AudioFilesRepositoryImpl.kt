package com.engfred.musicplayer.feature_library.data.repository

import com.engfred.musicplayer.feature_library.data.source.local.ContentResolverDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_library.domain.repository.AudioFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Concrete implementation of [AudioFileRepository] that fetches audio data from the device's
 * media storage using a [ContentResolverDataSource] and maps it to [AudioFile] domain models.
 */
class AudioFileRepositoryImpl @Inject constructor(
    private val dataSource: ContentResolverDataSource
) : AudioFileRepository {

    /**
     * Returns a Flow of [AudioFile] list from the local data source.
     * Converts nullable fields to default values for safety.
     */
    override fun getAllAudioFiles(): Flow<List<AudioFile>> {
        return dataSource.getAllAudioFilesFlow().map { dtoList ->
            dtoList.map { dto ->
                AudioFile(
                    id = dto.id,
                    title = dto.title ?: "Unknown Title",
                    artist = dto.artist ?: "Unknown Artist",
                    album = dto.album ?: "Unknown Album",
                    duration = dto.duration,
                    uri = dto.uri,
                    albumArtUri = dto.albumArtUri,
                    dateAdded = dto.dateAdded * 1000L // Convert from seconds to milliseconds
                )
            }
        }
    }
}
