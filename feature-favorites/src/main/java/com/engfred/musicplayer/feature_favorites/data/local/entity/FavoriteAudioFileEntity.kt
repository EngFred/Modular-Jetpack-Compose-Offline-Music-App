package com.engfred.musicplayer.feature_favorites.data.local.entity


import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for a Favorite Audio File.
 * Represents the 'favorite_audio_files' table in the local database.
 * The primary key is the original audio file ID.
 */
@Entity(tableName = "favorite_audio_files")
data class FavoriteAudioFileEntity(
    @PrimaryKey
    val audioFileId: Long, // Corresponds to AudioFile.id
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long, // Original dateAdded from MediaStore
    val favoritedAt: Long = System.currentTimeMillis() // When this song was added to favorites
)
