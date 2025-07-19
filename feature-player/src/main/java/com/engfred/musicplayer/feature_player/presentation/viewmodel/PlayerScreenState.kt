package com.engfred.musicplayer.feature_player.presentation.viewmodel

import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState // Import PlaybackState
import com.engfred.musicplayer.feature_library.domain.model.AudioFile // Keep AudioFile import for PlaybackState
import kotlin.time.Duration
/**
 * Data class representing the complete UI state for the Player Screen.
 * Now primarily holds a PlaybackState object, along with screen-specific loading/error.
 */
data class PlayerScreenState(
    val playbackState: PlaybackState = PlaybackState(), // Holds the core player state
    val error: String? = null, // General screen error, not necessarily player-specific
    val isLoading: Boolean = false // General screen loading, e.g., initial data fetch
) {
    // Convenience getters to easily access properties from the nested PlaybackState
    val currentAudioFile: AudioFile?
        get() = playbackState.currentAudioFile
    val isPlaying: Boolean
        get() = playbackState.isPlaying
    val playbackPositionMs: Long
        get() = playbackState.playbackPositionMs
    val totalDurationMs: Long
        get() = playbackState.totalDurationMs
    val bufferedPositionMs: Long
        get() = playbackState.bufferedPositionMs
    val repeatMode: RepeatMode
        get() = playbackState.repeatMode
    val shuffleMode: ShuffleMode
        get() = playbackState.shuffleMode
    val playbackSpeed: Float
        get() = playbackState.playbackSpeed
    val playbackProgress: Float
        get() = playbackState.playbackProgress
    val currentPlaybackTime: Duration
        get() = playbackState.currentPlaybackTime
    val totalPlaybackDuration: Duration
        get() = playbackState.totalPlaybackDuration
}