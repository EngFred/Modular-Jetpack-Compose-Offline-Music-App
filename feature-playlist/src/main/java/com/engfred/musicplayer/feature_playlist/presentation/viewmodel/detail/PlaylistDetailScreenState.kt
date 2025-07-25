package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist

data class PlaylistDetailScreenState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRenameDialog: Boolean = false,
    val allAudioFiles: List<AudioFile> = emptyList(), //for adding new songs to playlist
    val currentPlayingAudioFile: AudioFile? = null,
    val isPlaying: Boolean = false,
    //val screenMode: PlaylistDetailScreenMode = PlaylistDetailScreenMode.VIEW_PLAYLIST
)