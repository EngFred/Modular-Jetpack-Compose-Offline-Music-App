package com.engfred.musicplayer.feature_library.data.repository

import com.engfred.musicplayer.feature_library.data.source.local.ContentResolverDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_library.domain.repository.AudioFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Concrete implementation of AudioFileRepository that fetches data
 * from the ContentResolverDataSource and maps it to domain models.
 */
class AudioFileRepositoryImpl @Inject constructor(
    private val dataSource: ContentResolverDataSource
) : AudioFileRepository {

    override fun getAllAudioFiles(): Flow<List<AudioFile>> = flow {
        val audioFileDtos = dataSource.getAllAudioFiles()
        // Map DTOs to domain models
        val audioFiles = audioFileDtos.map { dto ->
            AudioFile(
                id = dto.id,
                title = dto.title ?: "Unknown Title", // Provide default for nulls
                artist = dto.artist ?: "Unknown Artist",
                album = dto.album ?: "Unknown Album",
                duration = dto.duration,
                uri = dto.uri,
                albumArtUri = dto.albumArtUri,
                dateAdded = dto.dateAdded * 1000L // Convert seconds to milliseconds
            )
        }
        emit(audioFiles)
    }

    override fun searchAudioFiles(query: String): Flow<List<AudioFile>> = flow {
        val audioFileDtos = dataSource.searchAudioFiles(query)
        val audioFiles = audioFileDtos.map { dto ->
            AudioFile(
                id = dto.id,
                title = dto.title ?: "Unknown Title",
                artist = dto.artist ?: "Unknown Artist",
                album = dto.album ?: "Unknown Album",
                duration = dto.duration,
                uri = dto.uri,
                albumArtUri = dto.albumArtUri,
                dateAdded = dto.dateAdded * 1000L
            )
        }
        emit(audioFiles)
    }
}
