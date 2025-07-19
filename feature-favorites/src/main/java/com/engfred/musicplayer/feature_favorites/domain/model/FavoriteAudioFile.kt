package com.engfred.musicplayer.feature_favorites.domain.model
import com.engfred.musicplayer.feature_library.domain.model.AudioFile

/**
 * Represents an audio file that has been marked as a favorite.
 * This domain model wraps an AudioFile and adds a timestamp for when it was favorited.
 */
data class FavoriteAudioFile(
    val audioFile: AudioFile,
    val favoritedAt: Long = System.currentTimeMillis()
)