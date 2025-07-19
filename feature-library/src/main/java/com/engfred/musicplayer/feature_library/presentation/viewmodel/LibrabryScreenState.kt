package com.engfred.musicplayer.feature_library.presentation.viewmodel

import com.engfred.musicplayer.feature_library.domain.model.AudioFile

/**
 * Data class representing the complete UI state for the Library Screen.
 */
data class LibraryScreenState(
    val audioFiles: List<AudioFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasStoragePermission: Boolean = false
)