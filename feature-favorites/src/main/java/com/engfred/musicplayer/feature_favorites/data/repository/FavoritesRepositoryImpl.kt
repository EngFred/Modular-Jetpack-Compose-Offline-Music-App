package com.engfred.musicplayer.feature_favorites.data.repository

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_favorites.data.local.dao.FavoriteAudioFileDao
import com.engfred.musicplayer.feature_favorites.data.local.entity.FavoriteAudioFileEntity
import com.engfred.musicplayer.feature_favorites.domain.model.FavoriteAudioFile
import com.engfred.musicplayer.feature_favorites.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of FavoritesRepository that interacts with the local Room database.
 * It maps between Room entities and domain models.
 */
@Singleton // This repository will be a singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteAudioFileDao: FavoriteAudioFileDao
) : FavoritesRepository {

    // --- Mapper functions to convert between domain models and Room entities ---

    private fun FavoriteAudioFileEntity.toDomain(): FavoriteAudioFile {
        return FavoriteAudioFile(
            audioFile = AudioFile(
                id = this.audioFileId,
                title = this.title,
                artist = this.artist,
                album = this.album,
                duration = this.duration,
                uri = this.uri,
                albumArtUri = this.albumArtUri,
                dateAdded = this.dateAdded
            ),
            favoritedAt = this.favoritedAt
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

    override fun getFavoriteAudioFiles(): Flow<List<FavoriteAudioFile>> {
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
}
