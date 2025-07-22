package com.engfred.musicplayer.feature_playlist.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioFile

/**
 * Sealed class representing all possible events that can occur on the Playlists Screen.
 */
sealed class PlaylistEvent {
    data class CreatePlaylist(val name: String) : PlaylistEvent()
    data class DeletePlaylist(val playlistId: Long) : PlaylistEvent()
    data class AddSongToPlaylist(val playlistId: Long, val audioFile: AudioFile) : PlaylistEvent()
    data class RemoveSongFromPlaylist(val playlistId: Long, val audioFileId: Long) : PlaylistEvent()
    data object LoadPlaylists : PlaylistEvent()
    data object ShowCreatePlaylistDialog : PlaylistEvent()
    data object HideCreatePlaylistDialog : PlaylistEvent()
    data object ToggleLayout : PlaylistEvent()
}