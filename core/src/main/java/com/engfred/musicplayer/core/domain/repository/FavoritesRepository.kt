package com.engfred.musicplayer.core.domain.repository

import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to favorite audio files.
 * This interface is part of the domain layer.
 */
interface FavoritesRepository {
    /**
     * Gets a flow of all favorite audio files.
     */
    fun getFavoriteAudioFiles(): Flow<List<AudioFile>>

    /**
     * Adds an audio file to favorites.
     * @param audioFile The AudioFile to add.
     */
    suspend fun addFavoriteAudioFile(audioFile: AudioFile)

    /**
     * Removes an audio file from favorites.
     * @param audioFileId The ID of the AudioFile to remove.
     */
    suspend fun removeFavoriteAudioFile(audioFileId: Long)

    /**
     * Checks if an audio file is currently a favorite.
     * @param audioFileId The ID of the AudioFile to check.
     * @return True if it's a favorite, false otherwise.
     */
    suspend fun isFavorite(audioFileId: Long): Boolean

    suspend fun updateFavoriteAudioFile(updatedAudioFile: AudioFile)
}