package com.engfred.musicplayer.feature_library.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioFile

/**
 * Sealed class representing all possible events that can occur on the Library Screen.
 */
sealed class LibraryEvent {
    data object LoadAudioFiles : LibraryEvent()
    data object PermissionGranted : LibraryEvent()
    data object CheckPermission : LibraryEvent()
    data class OnAudioFileClick(val audioFile: AudioFile) : LibraryEvent()
    data class OnSearchQueryChanged(val query: String) : LibraryEvent()
}