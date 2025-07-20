package com.engfred.musicplayer.feature_search.domain.usecases

import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_library.domain.repository.AudioFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for searching audio files.
 * It interacts with the AudioFileRepository from the feature-library module.
 */
class SearchAudioFilesUseCase @Inject constructor(
    private val audioFileRepository: AudioFileRepository // Inject repository from feature-library
) {
    operator fun invoke(query: String): Flow<Resource<List<AudioFile>>> = flow {
        if (query.isBlank()) {
            emit(Resource.Success(emptyList())) // Emit empty list if query is blank
            return@flow
        }

        emit(Resource.Loading())
        try {
            // Directly use the searchAudioFiles method from the AudioFileRepository
            // This pushes the filtering logic down to the data layer, which is more efficient.
            audioFileRepository.searchAudioFiles(query).collect { searchResults ->
                emit(Resource.Success(searchResults))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred during search."))
        }
    }
}
