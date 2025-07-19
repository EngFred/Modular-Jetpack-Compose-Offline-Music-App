package com.engfred.musicplayer.feature_favorites.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.engfred.musicplayer.feature_favorites.data.local.entity.FavoriteAudioFileEntity

/**
 * Data Access Object (DAO) for managing Favorite Audio Files in the local database.
 */
@Dao
interface FavoriteAudioFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAudioFile(favoriteAudioFile: FavoriteAudioFileEntity)

    @Query("DELETE FROM favorite_audio_files WHERE audioFileId = :audioFileId")
    suspend fun deleteFavoriteAudioFile(audioFileId: Long)

    @Query("SELECT * FROM favorite_audio_files ORDER BY favoritedAt DESC")
    fun getFavoriteAudioFiles(): Flow<List<FavoriteAudioFileEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_audio_files WHERE audioFileId = :audioFileId LIMIT 1)")
    suspend fun isFavorite(audioFileId: Long): Boolean
}