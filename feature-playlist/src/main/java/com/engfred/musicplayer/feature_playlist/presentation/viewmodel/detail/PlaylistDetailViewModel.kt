package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlayerController
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailScreenState())
    val uiState: StateFlow<PlaylistDetailScreenState> = _uiState.asStateFlow()

    private var currentPlaylistId: Long? = null

    init {
        sharedAudioDataSource.deviceAudioFiles.onEach {
            _uiState.update { currentState ->
                currentState.copy(allAudioFiles = it)
            }
        }.launchIn(viewModelScope)
        loadPlaylistDetails(savedStateHandle)
        startObservingPlaybackState(playerController)
    }

    private fun startObservingPlaybackState(playerController: PlayerController) {
        playerController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                currentState.copy(currentPlayingAudioFile = state.currentAudioFile, isPlaying = state.isPlaying)
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: PlaylistDetailEvent) {
        viewModelScope.launch {
            when (event) {
                is PlaylistDetailEvent.RemoveSong -> {
                    currentPlaylistId?.let { playlistId ->
                        try {
                            playlistRepository.removeSongFromPlaylist(playlistId, event.audioFileId)
                        } catch (e: Exception) {
                            _uiState.update { it.copy(error = "Failed to remove song: ${e.message}") }
                        }
                    }
                }
                is PlaylistDetailEvent.RenamePlaylist -> {
                    currentPlaylistId?.let {
                        val currentPlaylist = uiState.value.playlist
                        if (currentPlaylist != null && event.newName.isNotBlank()) {
                            try {
                                val updatedPlaylist = currentPlaylist.copy(name = event.newName)
                                playlistRepository.updatePlaylist(updatedPlaylist)
                                _uiState.update { it.copy(showRenameDialog = false) }
                                // Reload playlist after rename to update UI
                                //loadPlaylistDetails(playlistId)
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
                is PlaylistDetailEvent.AddSong -> {
                    currentPlaylistId?.let { playlistId ->
                        val playlistSongs = uiState.value.playlist?.songs?.map { it.id } ?: emptyList()
                        if (!playlistSongs.contains(event.audioFile.id)) {
                            try {
                                playlistRepository.addSongToPlaylist(playlistId, event.audioFile)
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
                    startAudioPlayback(event.audioFile)
                }
                PlaylistDetailEvent.ShufflePlay -> {
                    uiState.value.playlist?.songs?.let {
                        playerController.setShuffleMode(ShuffleMode.ON)
                        startAudioPlayback(it.shuffled()[0])
                    }
                }
                is PlaylistDetailEvent.LoadPlaylist -> {
                    loadPlaylistDetails(savedStateHandle)
                }

                is PlaylistDetailEvent.SwipedLeft -> {
                    startAudioPlayback(event.audioFile)
                }
                is PlaylistDetailEvent.SwipedRight -> {
                    if (_uiState.value.currentPlayingAudioFile?.id == event.audioFile.id && _uiState.value.isPlaying) {
                        playerController.playPause()
                    }
                }

                PlaylistDetailEvent.PlayNext -> {
                    viewModelScope.launch {
                        playerController.skipToNext()
                    }
                }
                PlaylistDetailEvent.PlayPause -> {
                    viewModelScope.launch {
                        playerController.playPause()
                    }
                }

                PlaylistDetailEvent.PlayPrev -> {
                    viewModelScope.launch {
                        playerController.skipToPrevious()
                    }
                }
            }
        }
    }

    private fun loadPlaylistDetails(savedStateHandle: SavedStateHandle) {
        savedStateHandle.get<Long>(PlaylistDetailArgs.PLAYLIST_ID)?.let { playlistId ->
            currentPlaylistId = playlistId
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
        } ?: run {
            _uiState.update { it.copy(error = "Playlist ID not provided.") }
        }
    }

    private suspend fun startAudioPlayback(audioFile: AudioFile) {
        uiState.value.playlist?.songs?.let {
            sharedAudioDataSource.setPlayingQueue(it)
            playerController.initiatePlayback(audioFile.uri)
        }
    }
}