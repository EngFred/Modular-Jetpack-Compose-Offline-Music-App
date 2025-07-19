package com.engfred.musicplayer.feature_playlist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for a Playlist.
 * Represents the 'playlists' table in the local database.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)