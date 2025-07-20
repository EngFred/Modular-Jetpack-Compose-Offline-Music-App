package com.engfred.musicplayer.feature_playlist.domain.model


import com.engfred.musicplayer.core.domain.model.AudioFile

/**
 * Represents a user-created playlist in the domain layer.
 */
data class Playlist(
    val playlistId: Long = 0L, // 0L for new playlists, Room will auto-generate
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songs: List<AudioFile> = emptyList() // List of AudioFile objects in this playlist
)