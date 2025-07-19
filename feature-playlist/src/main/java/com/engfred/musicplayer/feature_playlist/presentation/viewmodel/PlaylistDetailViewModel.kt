package com.engfred.musicplayer.feature_playlist.presentation.viewmodel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define the argument key locally within the playlist feature
object PlaylistDetailArgs {
    const val PLAYLIST_ID = "playlistId"
}

/**
 * ViewModel for the Playlist Detail screen.
 * Manages the state of a specific playlist and its songs.
 */
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    savedStateHandle: SavedStateHandle // To get navigation arguments
) : ViewModel() {

    // UI state for the playlist detail screen
    var uiState by mutableStateOf(PlaylistDetailScreenState())
        private set

    private var currentPlaylistId: Long? = null

    init {
        // Get playlistId from navigation arguments using the local constant
        savedStateHandle.get<Long>(PlaylistDetailArgs.PLAYLIST_ID)?.let { playlistId ->
            currentPlaylistId = playlistId
            loadPlaylistDetails(playlistId)
        } ?: run {
            uiState = uiState.copy(error = "Playlist ID not provided.")
        }
    }

    /**
     * Processes events from the UI and updates the ViewModel's state or triggers actions.
     */
    fun onEvent(event: PlaylistDetailEvent) {
        viewModelScope.launch {
            when (event) {
                is PlaylistDetailEvent.RemoveSong -> {
                    currentPlaylistId?.let { playlistId ->
                        playlistRepository.removeSongFromPlaylist(playlistId, event.audioFileId)
                    }
                }
                is PlaylistDetailEvent.RenamePlaylist -> {
                    currentPlaylistId?.let { playlistId ->
                        // Fetch current playlist, update name, then update in repo
                        val currentPlaylist = uiState.playlist
                        if (currentPlaylist != null && event.newName.isNotBlank()) {
                            val updatedPlaylist = currentPlaylist.copy(name = event.newName)
                            playlistRepository.updatePlaylist(updatedPlaylist)
                            uiState = uiState.copy(showRenameDialog = false) // Hide dialog
                        } else {
                            uiState = uiState.copy(error = "Playlist name cannot be empty.")
                        }
                    }
                }
                PlaylistDetailEvent.ShowRenameDialog -> {
                    uiState = uiState.copy(showRenameDialog = true)
                }
                PlaylistDetailEvent.HideRenameDialog -> {
                    uiState = uiState.copy(showRenameDialog = false, error = null)
                }
            }
        }
    }

    private fun loadPlaylistDetails(playlistId: Long) {
        uiState = uiState.copy(isLoading = true)
        playlistRepository.getPlaylistById(playlistId).onEach { playlist ->
            uiState = if (playlist != null) {
                uiState.copy(
                    playlist = playlist,
                    isLoading = false,
                    error = null
                )
            } else {
                uiState.copy(
                    isLoading = false,
                    error = "Playlist not found.",
                    playlist = null // Clear playlist if not found
                )
            }
        }.launchIn(viewModelScope)
    }
}

/**
 * Sealed class representing all possible events that can occur on the Playlist Detail Screen.
 */
sealed class PlaylistDetailEvent {
    data class RemoveSong(val audioFileId: Long) : PlaylistDetailEvent()
    data class RenamePlaylist(val newName: String) : PlaylistDetailEvent()
    data object ShowRenameDialog : PlaylistDetailEvent()
    data object HideRenameDialog : PlaylistDetailEvent()
}

/**
 * Data class representing the complete UI state for the Playlist Detail Screen.
 */
data class PlaylistDetailScreenState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRenameDialog: Boolean = false
)