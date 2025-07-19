package com.engfred.musicplayer.feature_library.data.model

import android.net.Uri

/**
 * Data Transfer Object (DTO) for an audio file as retrieved directly from the Android MediaStore.
 * This class is specific to the data layer and may contain Android-specific types (like Uri).
 */
data class AudioFileDto(
    val id: Long,
    val title: String?, // Can be null from MediaStore
    val artist: String?, // Can be null
    val album: String?, // Can be null
    val duration: Long, // in milliseconds
    val data: String?, // Path to the file, often deprecated for Uri
    val uri: Uri, // Content URI for the media file
    val albumId: Long?, // Album ID for fetching album art
    val albumArtUri: Uri?, //URI for album art, can be null, passed from ContentResolverDataSource
    val dateAdded: Long // Timestamp when the song was added to the device, in seconds
)