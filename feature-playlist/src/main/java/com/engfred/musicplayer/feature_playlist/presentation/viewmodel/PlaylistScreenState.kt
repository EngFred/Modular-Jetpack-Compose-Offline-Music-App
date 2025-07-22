package com.engfred.musicplayer.feature_playlist.presentation.viewmodel

import com.engfred.musicplayer.feature_playlist.domain.model.LayoutType
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist

/**
 * Data class representing the complete UI state for the Playlists Screen.
 */
data class PlaylistScreenState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true, // Start as loading
    val error: String? = null,
    val showCreatePlaylistDialog: Boolean = false,
    val dialogInputError: String? = null,
    val currentLayout: LayoutType = LayoutType.LIST
)