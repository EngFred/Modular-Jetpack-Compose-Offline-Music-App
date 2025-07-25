package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail

import com.engfred.musicplayer.core.domain.model.AudioFile

sealed class PlaylistDetailEvent {
    data class RemoveSong(val audioFileId: Long) : PlaylistDetailEvent()
    data class RenamePlaylist(val newName: String) : PlaylistDetailEvent()
    data object ShowRenameDialog : PlaylistDetailEvent()
    data object HideRenameDialog : PlaylistDetailEvent()
    data class AddSong(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class PlaySong(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class LoadPlaylist(val playlistId: Long) : PlaylistDetailEvent()
    data object ShufflePlay : PlaylistDetailEvent()
    data class SwipedLeft(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class SwipedRight(val audioFile: AudioFile) : PlaylistDetailEvent()
    data object PlayPause: PlaylistDetailEvent()
    data object PlayNext: PlaylistDetailEvent()
    data object PlayPrev: PlaylistDetailEvent()
}