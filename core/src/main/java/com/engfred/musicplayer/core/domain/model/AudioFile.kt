package com.engfred.musicplayer.core.domain.model

import android.net.Uri

/**
 * Represents a single audio file in the domain layer.
 * This is a pure Kotlin data class, independent of Android-specific details
 * like ContentResolver or MediaStore.
 */
data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String?,
    val artistId: Long?,
    val album: String?,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long
)
