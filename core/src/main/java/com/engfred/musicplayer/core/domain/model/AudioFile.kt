package com.engfred.musicplayer.core.domain.model

import android.net.Uri

/**
 * Represents a single audio file in the domain layer.
 * This is a pure Kotlin data class, independent of Android-specific details
 * like ContentResolver or MediaStore.
 */
data class AudioFile(
    val id: Long, // Unique ID of the audio file
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long, // in milliseconds
    val uri: Uri, // URI to play the song
    val albumArtUri: Uri?, // URI for album art, can be null
    val dateAdded: Long // Timestamp when the song was added to the device, in milliseconds
)
