package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list

import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.model.Playlist

/**
 * Represents the UI state for the Playlists screen.
 */
data class PlaylistScreenState(
    val automaticPlaylists: List<Playlist> = emptyList(),
    val userPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreatePlaylistDialog: Boolean = false,
    val dialogInputError: String? = null,
    val isPlaying: Boolean = false,
    val currentLayout: PlaylistLayoutType = PlaylistLayoutType.LIST
)