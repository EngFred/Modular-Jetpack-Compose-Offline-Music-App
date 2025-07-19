package com.engfred.musicplayer.feature_favorites.data.local.db

import android.net.Uri
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.engfred.musicplayer.feature_favorites.data.local.dao.FavoriteAudioFileDao
import com.engfred.musicplayer.feature_favorites.data.local.entity.FavoriteAudioFileEntity

/**
 * Room Database for managing Favorite Audio Files.
 */
@Database(
    entities = [FavoriteAudioFileEntity::class],
    version = 1, // Start with version 1
    exportSchema = false // For production, set to true and provide schema exports
)
@TypeConverters(FavoritesConverters::class) // Apply TypeConverters for custom types
abstract class FavoritesDatabase : RoomDatabase() {
    abstract fun favoriteAudioFileDao(): FavoriteAudioFileDao
}

/**
 * Type converters for Room to handle custom data types like Uri within the Favorites module.
 */
class FavoritesConverters {
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }
}