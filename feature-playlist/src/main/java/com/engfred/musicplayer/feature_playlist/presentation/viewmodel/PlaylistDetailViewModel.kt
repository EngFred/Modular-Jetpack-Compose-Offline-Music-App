package com.engfred.musicplayer.feature_playlist.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.domain.model.PlaylistDetailScreenMode // Ensure this import points to your enum definition
import com.engfred.musicplayer.feature_playlist.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailScreenState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRenameDialog: Boolean = false,
    // val showAddSongsDialog: Boolean = false, // REMOVED: Replaced by screenMode
    val allAudioFiles: List<AudioFile> = emptyList(), // All audio files on the device
    val currentPlayingId: Long? = null,
    val screenMode: PlaylistDetailScreenMode = PlaylistDetailScreenMode.VIEW_PLAYLIST // NEW: To control the active UI
)

sealed class PlaylistDetailEvent {
    data class RemoveSong(val audioFileId: Long) : PlaylistDetailEvent()
    data class RenamePlaylist(val newName: String) : PlaylistDetailEvent()
    data object ShowRenameDialog : PlaylistDetailEvent()
    data object HideRenameDialog : PlaylistDetailEvent()
    // REMOVED: ShowAddSongsDialog and HideAddSongsDialog are replaced by ChangeScreenMode
    data class AddSong(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class PlaySong(val audioFile: AudioFile, val onPlay: (String) -> Unit) : PlaylistDetailEvent()
    data class LoadPlaylist(val playlistId: Long) : PlaylistDetailEvent()
    data class ChangeScreenMode(val mode: PlaylistDetailScreenMode) : PlaylistDetailEvent() // NEW event
    data object ShufflePlay : PlaylistDetailEvent() // Added this back if it was missing from the last update
}

// --- ViewModel ---

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
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailScreenState())
    val uiState: StateFlow<PlaylistDetailScreenState> = _uiState.asStateFlow()

    private var currentPlaylistId: Long? = null
    // No longer need a separate `allDeviceAudioFiles` var, as it's now in UiState
    // private var allDeviceAudioFiles: List<AudioFile> = emptyList()

    init {
        // Collect all audio files from SharedAudioDataSource for UI state
        // This will now update `uiState.allAudioFiles` directly.
        sharedAudioDataSource.allAudioFiles.onEach { audioFiles ->
            _uiState.update { it.copy(allAudioFiles = audioFiles) }
            Log.d("PlaylistDetailViewModel", "Updated uiState.allAudioFiles with ${audioFiles.size} songs.")
        }.launchIn(viewModelScope)

        // Observe player's current audio for updating currentPlayingId
        playerController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                if (state.currentAudioFile != null && state.isPlaying) {
                    currentState.copy(currentPlayingId = state.currentAudioFile!!.id)
                } else if (!state.isPlaying) {
                    // If paused or stopped, clear current playing ID if it's the same song
                    // Or keep it if it's just paused on the current song
                    if (currentState.currentPlayingId == state.currentAudioFile?.id) {
                        currentState.copy(currentPlayingId = null) // Clear if paused/stopped
                    } else {
                        currentState // No change needed if a different song was playing or current ID is already null
                    }
                } else {
                    currentState // No change needed
                }
            }
        }.launchIn(viewModelScope)

        // Load playlist details
        savedStateHandle.get<Long>(PlaylistDetailArgs.PLAYLIST_ID)?.let { playlistId ->
            currentPlaylistId = playlistId
            loadPlaylistDetails(playlistId)
        } ?: run {
            _uiState.update { it.copy(error = "Playlist ID not provided.") }
        }
    }

    fun onEvent(event: PlaylistDetailEvent) {
        viewModelScope.launch {
            when (event) {
                is PlaylistDetailEvent.RemoveSong -> {
                    currentPlaylistId?.let { playlistId ->
                        try {
                            playlistRepository.removeSongFromPlaylist(playlistId, event.audioFileId)
                            // Reload playlist after removal to update UI
                            loadPlaylistDetails(playlistId)
                        } catch (e: Exception) {
                            _uiState.update { it.copy(error = "Failed to remove song: ${e.message}") }
                        }
                    }
                }
                is PlaylistDetailEvent.RenamePlaylist -> {
                    currentPlaylistId?.let { playlistId ->
                        val currentPlaylist = uiState.value.playlist
                        if (currentPlaylist != null && event.newName.isNotBlank()) {
                            try {
                                val updatedPlaylist = currentPlaylist.copy(name = event.newName)
                                playlistRepository.updatePlaylist(updatedPlaylist)
                                _uiState.update { it.copy(showRenameDialog = false) }
                                // Reload playlist after rename to update UI
                                loadPlaylistDetails(playlistId)
                            } catch (e: Exception) {
                                _uiState.update { it.copy(error = "Failed to rename playlist: ${e.message}") }
                            }
                        } else {
                            _uiState.update { it.copy(error = "Playlist name cannot be empty.") }
                        }
                    }
                }
                PlaylistDetailEvent.ShowRenameDialog -> {
                    _uiState.update { it.copy(showRenameDialog = true, error = null) }
                }
                PlaylistDetailEvent.HideRenameDialog -> {
                    _uiState.update { it.copy(showRenameDialog = false, error = null) }
                }
                // REMOVED: These events are no longer used for mode switching
                // PlaylistDetailEvent.ShowAddSongsDialog -> {
                //     _uiState.update { it.copy(showAddSongsDialog = true, error = null) }
                // }
                // PlaylistDetailEvent.HideAddSongsDialog -> {
                //     _uiState.update { it.copy(showAddSongsDialog = false, error = null) }
                // }
                is PlaylistDetailEvent.AddSong -> {
                    currentPlaylistId?.let { playlistId ->
                        val currentSongs = uiState.value.playlist?.songs?.map { it.id } ?: emptyList()
                        if (!currentSongs.contains(event.audioFile.id)) {
                            try {
                                playlistRepository.addSongToPlaylist(playlistId, event.audioFile)
                                // Reload playlist after adding song to update UI
                                loadPlaylistDetails(playlistId)
                            } catch (e: Exception) {
                                _uiState.update { it.copy(error = "Failed to add song: ${e.message}") }
                            }
                        } else {
                            // Optionally, show a message if song is already in playlist
                            Log.d("PlaylistDetailViewModel", "Song ${event.audioFile.title} is already in playlist.")
                        }
                    }
                }
                is PlaylistDetailEvent.PlaySong -> {
                    val playlistSongs = uiState.value.playlist?.songs ?: emptyList()
                    if (playlistSongs.isNotEmpty()) {
                        sharedAudioDataSource.setAudioFiles(playlistSongs)
                        //playerController.setAudioFiles(playlistSongs) // Use playerController directly
                        Log.d("PlaylistDetailViewModel", "Set playback queue to ${playlistSongs.size} playlist songs.")
                        // The `onPlay` callback from the UI will trigger navigation to player screen
                        event.onPlay(event.audioFile.id.toString()) // Pass ID as String if your onPlay expects it
                    }
                }
                PlaylistDetailEvent.ShufflePlay -> {
                    val playlistSongs = uiState.value.playlist?.songs?.shuffled() ?: emptyList()
                    if (playlistSongs.isNotEmpty()) {
                        sharedAudioDataSource.setAudioFiles(playlistSongs)
                        //playerController.setAudioFiles(playlistSongs)
                        Log.d("PlaylistDetailViewModel", "Shuffled playback queue with ${playlistSongs.size} songs.")
                        // Optionally, if you want to immediately start playing the first shuffled song:
                        // playerController.play(playlistSongs.first().id.toString())
                        // You might also want to notify the UI to navigate to the player or update its playing state.
                        // For now, let's assume the player controller handles the start of playback.
                    } else {
                        Log.d("PlaylistDetailViewModel", "Cannot shuffle empty playlist.")
                    }
                }
                is PlaylistDetailEvent.LoadPlaylist -> {
                    loadPlaylistDetails(event.playlistId)
                }
                // NEW: Handle screen mode changes
                is PlaylistDetailEvent.ChangeScreenMode -> {
                    _uiState.update { it.copy(screenMode = event.mode, error = null) }
                }
            }
        }
    }

    // Removed this function as allAudioFiles is now part of uiState
    // fun getAllDeviceAudioFiles(): List<AudioFile> {
    //     return allDeviceAudioFiles
    // }

    private fun loadPlaylistDetails(playlistId: Long) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        playlistRepository.getPlaylistById(playlistId).onEach { playlist ->
            _uiState.update { currentState ->
                if (playlist != null) {
                    currentState.copy(
                        playlist = playlist,
                        isLoading = false,
                        error = null
                    )
                } else {
                    currentState.copy(
                        isLoading = false,
                        error = "Playlist not found.",
                        playlist = null
                    )
                }
            }
        }.launchIn(viewModelScope)
    }
}