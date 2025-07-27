package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.Playlist

sealed class FavoritesEvent {
    data class PlayAudio(val audioFile: AudioFile) : FavoritesEvent()
    data class SwipedLeft(val audioFile: AudioFile) : FavoritesEvent()
    data class SwipedRight(val audioFile: AudioFile) : FavoritesEvent()
    data class ShowPlaylistsDialog(val audioFile: AudioFile) : FavoritesEvent()
    data class AddedSongToPlaylist(val playlist: Playlist) : FavoritesEvent()
    data object DismissAddToPlaylistDialog: FavoritesEvent()
    data class PlayNext(val audioFile: AudioFile) : FavoritesEvent()
    data class ShowRemoveFavoriteConfirmation(val audioFile: AudioFile) : FavoritesEvent()
    data object DismissRemoveFavoriteConfirmation : FavoritesEvent()
    data object ConfirmRemoveFavorite : FavoritesEvent()
}