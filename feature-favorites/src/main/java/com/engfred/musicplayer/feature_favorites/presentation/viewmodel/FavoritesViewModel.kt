package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.FavoritesRepository
import com.engfred.musicplayer.core.domain.repository.PlayerController
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playerController: PlayerController,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesScreenState())
    val uiState: StateFlow<FavoritesScreenState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadFavoriteAudioFiles()
        startObservingPlaybackState()

        playlistRepository.getPlaylists().onEach { playlists ->
            _uiState.update { it.copy(playlists = playlists.filterNot { playlist -> playlist.isAutomatic }) }
        }.launchIn(viewModelScope)
    }

    private fun startObservingPlaybackState() {
        playerController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                if (state.currentAudioFile != null && state.isPlaying) {
                    currentState.copy(
                        currentPlayingId = state.currentAudioFile!!.id,
                        isPlaying = true
                    )
                } else if (!state.isPlaying) {
                    if (currentState.currentPlayingId == state.currentAudioFile?.id) {
                        currentState.copy(
                            currentPlayingId = null,
                            isPlaying = false
                        )
                    } else {
                        currentState
                    }
                } else {
                    currentState
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: FavoritesEvent) {
        viewModelScope.launch {
            when (event) {
                // Handle new confirmation events first
                is FavoritesEvent.ShowRemoveFavoriteConfirmation -> {
                    _uiState.update {
                        it.copy(
                            showRemoveFavoriteConfirmationDialog = true,
                            audioFileToRemove = event.audioFile
                        )
                    }
                }
                FavoritesEvent.DismissRemoveFavoriteConfirmation -> {
                    _uiState.update {
                        it.copy(
                            showRemoveFavoriteConfirmationDialog = false,
                            audioFileToRemove = null
                        )
                    }
                }
                FavoritesEvent.ConfirmRemoveFavorite -> {
                    _uiState.value.audioFileToRemove?.let { audioFile ->
                        try {
                            favoritesRepository.removeFavoriteAudioFile(audioFile.id)
                            playerController.removeFromQueue(audioFile)
                            _uiEvent.emit("Removed '${audioFile.title}' from favorites.")
                            Log.d("FavoritesViewModel", "Removed favorite audio file ID: ${audioFile.id}")
                        } catch (e: Exception) {
                            _uiState.update { it.copy(error = "Error removing favorite: ${e.message}") }
                            _uiEvent.emit("Failed to remove favorite: ${e.message}")
                            Log.e("FavoritesViewModel", "Error removing favorite: ${e.message}", e)
                        } finally {
                            // Always dismiss the dialog after attempting removal
                            _uiState.update {
                                it.copy(
                                    showRemoveFavoriteConfirmationDialog = false,
                                    audioFileToRemove = null
                                )
                            }
                        }
                    } ?: _uiEvent.emit("No song selected to remove from favorites.")
                }

                is FavoritesEvent.PlayAudio -> {
                    startAudioPlayback(event.audioFile)
                }

                is FavoritesEvent.SwipedLeft -> {
                    startAudioPlayback(event.audioFile)
                }
                is FavoritesEvent.SwipedRight -> {
                    if (_uiState.value.currentPlayingId == event.audioFile.id && _uiState.value.isPlaying) {
                        playerController.playPause()
                    }
                }

                is FavoritesEvent.ShowPlaylistsDialog -> {
                    _uiState.update {
                        it.copy(
                            audioToAddToPlaylist = event.audioFile,
                            showAddToPlaylistDialog = true
                        )
                    }
                }

                is FavoritesEvent.AddedSongToPlaylist -> {
                    val audioFile = _uiState.value.audioToAddToPlaylist
                    if (audioFile != null) {
                        val songAlreadyInPlaylist = event.playlist.songs.any { it.id == audioFile.id }

                        if (songAlreadyInPlaylist) {
                            _uiEvent.emit("Song already in playlist")
                        } else {
                            try {
                                playlistRepository.addSongToPlaylist(event.playlist.id, audioFile)
                                _uiEvent.emit("Added successfully!")
                            } catch (ex: Exception) {
                                _uiEvent.emit("Failed to add song to playlist")
                            }
                        }
                        _uiState.update { it.copy(audioToAddToPlaylist = null, showAddToPlaylistDialog = false) }
                    } else {
                        _uiEvent.emit("Failed to add song to playlist")
                    }
                }

                FavoritesEvent.DismissAddToPlaylistDialog -> {
                    _uiState.update { it.copy(audioToAddToPlaylist = null, showAddToPlaylistDialog = false) }
                }

                is FavoritesEvent.PlayNext -> {
                    playerController.addAudioToQueueNext(event.audioFile)
                }
            }
        }
    }

    private suspend fun startAudioPlayback(audioFile: AudioFile) {
        val audioFiles =  uiState.value.favoriteAudioFiles
        sharedAudioDataSource.setPlayingQueue(audioFiles)
        playerController.initiatePlayback(audioFile.uri)
    }

    private fun loadFavoriteAudioFiles() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        favoritesRepository.getFavoriteAudioFiles().onEach { favoriteAudioFiles ->
            _uiState.update {
                it.copy(
                    favoriteAudioFiles = favoriteAudioFiles,
                    isLoading = false,
                    error = null
                )
            }
            Log.d("FavoritesViewModel", "Loaded ${favoriteAudioFiles.size} favorite audio files.")
        }.launchIn(viewModelScope)
    }
}