package com.engfred.musicplayer.feature_playlist.presentation.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Playlists screen.
 * Manages playlist state, handles user interactions, and interacts with PlaylistRepository.
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // UI state for the playlists screen
    var uiState by mutableStateOf(PlaylistScreenState())
        private set

    init {
        // Load playlists when the ViewModel is initialized
        loadPlaylists()
    }

    /**
     * Processes events from the UI and updates the ViewModel's state or triggers actions.
     */
    fun onEvent(event: PlaylistEvent) {
        viewModelScope.launch {
            when (event) {
                is PlaylistEvent.CreatePlaylist -> {
                    if (event.name.isNotBlank()) {
                        val newPlaylist = Playlist(name = event.name)
                        playlistRepository.createPlaylist(newPlaylist)
                        uiState = uiState.copy(showCreatePlaylistDialog = false) // Close dialog
                    } else {
                        uiState = uiState.copy(error = "Playlist name cannot be empty.")
                    }
                }
                is PlaylistEvent.DeletePlaylist -> {
                    playlistRepository.deletePlaylist(event.playlistId)
                }
                is PlaylistEvent.AddSongToPlaylist -> {
                    // This will be handled when integrating with the Library/Song selection
                    // For now, it's a placeholder
                    playlistRepository.addSongToPlaylist(event.playlistId, event.audioFile)
                }
                is PlaylistEvent.RemoveSongFromPlaylist -> {
                    playlistRepository.removeSongFromPlaylist(event.playlistId, event.audioFileId)
                }
                PlaylistEvent.LoadPlaylists -> {
                    loadPlaylists()
                }
                PlaylistEvent.ShowCreatePlaylistDialog -> {
                    uiState = uiState.copy(showCreatePlaylistDialog = true)
                }
                PlaylistEvent.HideCreatePlaylistDialog -> {
                    uiState = uiState.copy(showCreatePlaylistDialog = false, error = null) // Clear error on hide
                }
            }
        }
    }

    private fun loadPlaylists() {
        playlistRepository.getPlaylists().onEach { playlists ->
            uiState = uiState.copy(
                playlists = playlists,
                isLoading = false, // Assuming initial load is successful here
                error = null
            )
        }.launchIn(viewModelScope) // Launch collection in ViewModel's scope
    }
}
