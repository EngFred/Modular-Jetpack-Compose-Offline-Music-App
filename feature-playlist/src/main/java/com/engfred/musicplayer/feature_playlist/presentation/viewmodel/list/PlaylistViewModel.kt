package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.domain.repository.PlayerController
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.domain.repository.PlaylistRepository
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
    playerController: PlayerController,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Change from 'var uiState by mutableStateOf' to MutableStateFlow
    private val _uiState = MutableStateFlow(PlaylistScreenState())
    val uiState: StateFlow<PlaylistScreenState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        loadPlaylists()
        playerController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                if (state.currentAudioFile != null) {
                    Log.d("PlaylistViewModel", "Is playing...")
                    currentState.copy(isPlaying = true)
                } else {
                    Log.d("PlaylistViewModel", "Is not playing!!")
                    currentState.copy(isPlaying = false)
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedLayout = settingsRepository.getAppSettings().first().playlistLayoutType
                _uiState.update { currentState ->
                    currentState.copy(currentLayout = savedLayout)
                }
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Failed to load playlist layout from settings: ${e.message}", e)
                // Fallback to default if loading fails
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
                        if (uiState.value.playlists.any { it.name.equals(event.name, ignoreCase = true) }) {
                            _uiState.update { it.copy(dialogInputError = "Playlist with this name already exists.") }
                            return@launch
                        }

                        val newPlaylist = Playlist(name = event.name)
                        try {
                            playlistRepository.createPlaylist(newPlaylist)
                            _uiState.update { it.copy(showCreatePlaylistDialog = false, dialogInputError = null) }
                            _uiEvent.emit("Playlist '${event.name}' created!")
                        } catch (e: Exception) {
                            _uiState.update { it.copy(dialogInputError = "Error creating playlist: ${e.message}") }
                            _uiEvent.emit("Error creating playlist: ${e.message}")
                        }
                    } else {
                        _uiState.update { it.copy(dialogInputError = "Playlist name cannot be empty.") }
                    }
                }
                is PlaylistEvent.DeletePlaylist -> {
                    try {
                        playlistRepository.deletePlaylist(event.playlistId)
                        _uiEvent.emit("Playlist deleted.")
                    } catch (e: Exception) {
                        _uiEvent.emit("Error deleting playlist: ${e.message}")
                    }
                }
                is PlaylistEvent.AddSongToPlaylist -> {
                    try {
                        playlistRepository.addSongToPlaylist(event.playlistId, event.audioFile)
                        _uiEvent.emit("Song added to playlist.")
                    } catch (e: Exception) {
                        _uiEvent.emit("Error adding song: ${e.message}")
                    }
                }
                is PlaylistEvent.RemoveSongFromPlaylist -> {
                    try {
                        playlistRepository.removeSongFromPlaylist(event.playlistId, event.audioFileId)
                        _uiEvent.emit("Error removing song!")
                    } catch (e: Exception) {
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
        playlistRepository.getPlaylists().onEach { playlists ->
            _uiState.update {
                it.copy(
                    playlists = playlists,
                    isLoading = false,
                    error = null
                )
            }
        }.launchIn(viewModelScope)
    }
}