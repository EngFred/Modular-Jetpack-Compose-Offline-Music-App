package com.engfred.musicplayer.feature_player.domain.model

import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the current playback state of the music player.
 * This is a pure Kotlin data class, independent of Media3 specifics.
 */
data class PlaybackState(
    val currentAudioFile: AudioFile? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val playbackSpeed: Float = 1.0f, // From your provided code
    val error: String? = null,
    val isLoading: Boolean = false // Re-ADDED: This is necessary for MusicService.kt
) {
    val playbackProgress: Float
        get() = if (totalDurationMs > 0) playbackPositionMs.toFloat() / totalDurationMs else 0f

    val currentPlaybackTime: Duration
        get() = playbackPositionMs.milliseconds

    val totalPlaybackDuration: Duration
        get() = totalDurationMs.milliseconds
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

enum class ShuffleMode {
    OFF,
    ON
}