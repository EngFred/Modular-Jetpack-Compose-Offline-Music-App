package com.engfred.musicplayer.feature_player.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode

/**
 * Sealed class representing all possible events that can occur on the Player Screen.
 */
sealed class PlayerEvent {
    data class PlayAudioFile(val audioFile: AudioFile) : PlayerEvent()
    data object PlayPause : PlayerEvent()
    data object SkipToNext : PlayerEvent()
    data object SkipToPrevious : PlayerEvent()
    data class SeekTo(val positionMs: Long) : PlayerEvent()
    data class SetRepeatMode(val mode: RepeatMode) : PlayerEvent()
    data class SetShuffleMode(val mode: ShuffleMode) : PlayerEvent()
    data object ReleasePlayer : PlayerEvent() // Event to explicitly release player resources
}