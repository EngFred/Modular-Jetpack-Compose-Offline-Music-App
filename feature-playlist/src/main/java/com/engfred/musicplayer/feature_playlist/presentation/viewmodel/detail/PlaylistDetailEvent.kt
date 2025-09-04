package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.Playlist

sealed class PlaylistDetailEvent {
    data class RenamePlaylist(val newName: String) : PlaylistDetailEvent()
    data object ShowRenameDialog : PlaylistDetailEvent()
    data object HideRenameDialog : PlaylistDetailEvent()
    data class AddSong(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class PlaySong(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class LoadPlaylist(val playlistId: Long) : PlaylistDetailEvent()
    data object ShufflePlay : PlaylistDetailEvent()
    data object PlayPause: PlaylistDetailEvent()
    data object PlayNext: PlaylistDetailEvent()
    data object PlayPrev: PlaylistDetailEvent()
    data object DismissAddToPlaylistDialog : PlaylistDetailEvent()
    data class ShowPlaylistsDialog(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class AddedSongToPlaylist(val playlist: Playlist) : PlaylistDetailEvent()
    data class SetPlayNext(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class ShowRemoveSongConfirmation(val audioFile: AudioFile) : PlaylistDetailEvent()
    data object DismissRemoveSongConfirmation : PlaylistDetailEvent()
    data object ConfirmRemoveSong : PlaylistDetailEvent()
}