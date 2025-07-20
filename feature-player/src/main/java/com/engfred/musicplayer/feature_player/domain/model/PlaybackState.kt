package com.engfred.musicplayer.feature_player.domain.model

import com.engfred.musicplayer.core.domain.model.AudioFile

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
    val playbackSpeed: Float = 1.0f,
    val isLoading: Boolean = false,
    val error: String? = null,
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

enum class ShuffleMode {
    OFF,
    ON
}