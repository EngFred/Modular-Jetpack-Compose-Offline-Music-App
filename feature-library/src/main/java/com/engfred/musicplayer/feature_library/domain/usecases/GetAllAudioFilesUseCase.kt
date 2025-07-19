package com.engfred.musicplayer.feature_library.domain.usecases

import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.feature_library.domain.model.AudioFile
import com.engfred.musicplayer.feature_library.domain.repository.AudioFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case to get all audio files from the device.
 * It interacts with the AudioFileRepository and wraps the result in a Resource for UI state management.
 */
class GetAllAudioFilesUseCase @Inject constructor(
    private val repository: AudioFileRepository
) {
    operator fun invoke(): Flow<Resource<List<AudioFile>>> = flow {
        emit(Resource.Loading())

        try {
            repository.getAllAudioFiles().collect { audioFiles ->
                if (audioFiles.isEmpty()) {
                    emit(Resource.Error("No audio files found on device."))
                } else {
                    emit(Resource.Success(audioFiles))
                }
            }
        } catch (e: Exception) {
            // Emit Error state if any exception occurs during data fetching or processing
            emit(Resource.Error("Could not load audio files: ${e.localizedMessage}"))
        }
    }
}