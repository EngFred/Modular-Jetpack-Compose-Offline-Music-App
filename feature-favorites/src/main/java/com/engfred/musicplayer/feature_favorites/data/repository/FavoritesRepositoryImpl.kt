package com.engfred.musicplayer.feature_favorites.data.repository

import android.util.Log
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.FavoritesRepository
import com.engfred.musicplayer.feature_favorites.data.local.dao.FavoriteAudioFileDao
import com.engfred.musicplayer.feature_favorites.data.local.entity.FavoriteAudioFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteAudioFileDao: FavoriteAudioFileDao
) : FavoritesRepository {
    // --- Mapper functions to convert between domain models and Room entities ---
    private fun FavoriteAudioFileEntity.toDomain(): AudioFile {
        return AudioFile(
            id = this.audioFileId,
            title = this.title,
            artist = this.artist,
            album = this.album,
            duration = this.duration,
            uri = this.uri,
            albumArtUri = this.albumArtUri,
            dateAdded = this.dateAdded
        )
    }
    private fun AudioFile.toEntity(): FavoriteAudioFileEntity {
        return FavoriteAudioFileEntity(
            audioFileId = this.id,
            title = this.title,
            artist = this.artist,
            album = this.album,
            duration = this.duration,
            uri = this.uri,
            albumArtUri = this.albumArtUri,
            dateAdded = this.dateAdded
        )
    }
    // --- Repository interface implementations ---
    override fun getFavoriteAudioFiles(): Flow<List<AudioFile>> {
        return favoriteAudioFileDao.getFavoriteAudioFiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    override suspend fun addFavoriteAudioFile(audioFile: AudioFile) {
        val favoriteEntity = audioFile.toEntity()
        favoriteAudioFileDao.insertFavoriteAudioFile(favoriteEntity)
    }
    override suspend fun removeFavoriteAudioFile(audioFileId: Long) {
        favoriteAudioFileDao.deleteFavoriteAudioFile(audioFileId)
    }
    override suspend fun isFavorite(audioFileId: Long): Boolean {
        return favoriteAudioFileDao.isFavorite(audioFileId)
    }
    // NEW: Updates the favorite's metadata if it exists.
    override suspend fun updateFavoriteAudioFile(updatedAudioFile: AudioFile) {
        try {
            favoriteAudioFileDao.updateFavoriteAudioFileMetadata(
                audioFileId = updatedAudioFile.id,
                title = updatedAudioFile.title,
                artist = updatedAudioFile.artist ?: "Unknown Artist",
                albumArtUri = updatedAudioFile.albumArtUri?.toString()
            )
            Log.d("LocalUpdate", "Updated favorite audio file with ID: ${updatedAudioFile.id}")
        }catch (e: Exception) {
            Log.e("LocalUpdate", "Error updating favorite audio file: ${e.message}")
        }
    }
}