package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list

import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist

/**
 * Data class representing the complete UI state for the Playlists Screen.
 */
data class PlaylistScreenState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreatePlaylistDialog: Boolean = false,
    val dialogInputError: String? = null,
    val currentLayout: PlaylistLayoutType = PlaylistLayoutType.LIST,
    val isPlaying: Boolean = false,
)