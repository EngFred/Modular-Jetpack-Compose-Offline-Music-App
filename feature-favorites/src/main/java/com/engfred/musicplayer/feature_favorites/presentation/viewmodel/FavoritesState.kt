package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.Playlist

data class FavoritesScreenState(
    val favoriteAudioFiles: List<AudioFile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentPlayingId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPlaybackAudioFile: AudioFile? = null,
    val audioToAddToPlaylist: AudioFile? = null,
    val playlists: List<Playlist> = emptyList(),
    val showAddToPlaylistDialog: Boolean = false,
    val showRemoveFavoriteConfirmationDialog: Boolean = false,
    val audioFileToRemove: AudioFile? = null
)