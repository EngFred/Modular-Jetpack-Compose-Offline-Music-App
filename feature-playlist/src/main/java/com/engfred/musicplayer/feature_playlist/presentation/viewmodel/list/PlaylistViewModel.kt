package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.model.Playlist
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    playbackController: PlaybackController,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val TAG = "PlaylistViewModel"

    private val _uiState = MutableStateFlow(PlaylistScreenState())
    val uiState: StateFlow<PlaylistScreenState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        loadPlaylists()
        playbackController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                currentState.copy(isPlaying = state.currentAudioFile != null && state.isPlaying)
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedLayout = settingsRepository.getAppSettings().first().playlistLayoutType
                _uiState.update { currentState ->
                    currentState.copy(currentLayout = savedLayout)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load playlist layout from settings: ${e.message}", e)
                _uiState.update { currentState ->
                    currentState.copy(currentLayout = PlaylistLayoutType.LIST)
                }
            }
        }
    }

    fun onEvent(event: PlaylistEvent) {
        viewModelScope.launch {
            when (event) {
                is PlaylistEvent.CreatePlaylist -> {
                    if (event.name.isNotBlank()) {
                        if (uiState.value.userPlaylists.any { it.name.equals(event.name, ignoreCase = true) }) { // Check only user playlists
                            _uiState.update { it.copy(dialogInputError = "Playlist with this name already exists.") }
                            return@launch
                        }

                        val newPlaylist = Playlist(name = event.name, isAutomatic = false, type = null)
                        try {
                            playlistRepository.createPlaylist(newPlaylist)
                            _uiState.update { it.copy(showCreatePlaylistDialog = false, dialogInputError = null) }
                            _uiEvent.emit("Playlist '${event.name}' created!")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating playlist: ${e.message}", e)
                            _uiState.update { it.copy(dialogInputError = "Error creating playlist: ${e.message}") }
                            _uiEvent.emit("Error creating playlist: ${e.message}")
                        }
                    } else {
                        _uiState.update { it.copy(dialogInputError = "Playlist name cannot be empty.") }
                    }
                }
                is PlaylistEvent.DeletePlaylist -> {
                    if (event.playlistId < 0) {
                        _uiEvent.emit("Automatic playlists cannot be deleted.")
                        return@launch
                    }
                    try {
                        playlistRepository.deletePlaylist(event.playlistId)
                        _uiEvent.emit("Playlist deleted.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting playlist: ${e.message}", e)
                        _uiEvent.emit("Error deleting playlist: ${e.message}")
                    }
                }
                is PlaylistEvent.AddSongToPlaylist -> {
                    if (event.playlistId < 0) {
                        _uiEvent.emit("Cannot manually add songs to automatic playlists.")
                        return@launch
                    }
                    try {
                        playlistRepository.addSongToPlaylist(event.playlistId, event.audioFile)
                        _uiEvent.emit("Song added to playlist.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding song to playlist: ${e.message}", e)
                        _uiEvent.emit("Error adding song: ${e.message}")
                    }
                }
                is PlaylistEvent.RemoveSongFromPlaylist -> {
                    if (event.playlistId < 0) {
                        _uiEvent.emit("Cannot manually remove songs from automatic playlists.")
                        return@launch
                    }
                    try {
                        playlistRepository.removeSongFromPlaylist(event.playlistId, event.audioFileId)
                        _uiEvent.emit("Song removed from playlist.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing song from playlist: ${e.message}", e)
                        _uiEvent.emit("Error removing song: ${e.message}")
                    }
                }
                PlaylistEvent.LoadPlaylists -> {
                    loadPlaylists()
                }
                PlaylistEvent.ShowCreatePlaylistDialog -> {
                    _uiState.update { it.copy(showCreatePlaylistDialog = true, dialogInputError = null) }
                }
                PlaylistEvent.HideCreatePlaylistDialog -> {
                    _uiState.update { it.copy(showCreatePlaylistDialog = false, dialogInputError = null) }
                }
                PlaylistEvent.ToggleLayout -> {
                    _uiState.update {
                        it.copy(
                            currentLayout = if (it.currentLayout == PlaylistLayoutType.LIST) PlaylistLayoutType.GRID else PlaylistLayoutType.LIST
                        )
                    }
                    settingsRepository.updatePlaylistLayout(_uiState.value.currentLayout)
                }
            }
        }
    }

    private fun loadPlaylists() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        playlistRepository.getPlaylists().onEach { allPlaylists ->
            val automatic = allPlaylists.filter { it.isAutomatic }
            val user = allPlaylists.filter { !it.isAutomatic }
            _uiState.update {
                it.copy(
                    automaticPlaylists = automatic,
                    userPlaylists = user,
                    isLoading = false,
                    error = null
                )
            }
        }.launchIn(viewModelScope)
    }
}
