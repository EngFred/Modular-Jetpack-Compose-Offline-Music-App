package com.engfred.musicplayer.feature_library.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_library.presentation.components.FilterOption

/**
 * Data class representing the complete UI state for the Library Screen.
 */
data class LibraryScreenState(
    val audioFiles: List<AudioFile> = emptyList(),
    val filteredAudioFiles: List<AudioFile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasStoragePermission: Boolean = false,
    val currentPlayingId: Long? = null,
    val currentFilterOption: FilterOption = FilterOption.ALPHABETICAL_DESC
)