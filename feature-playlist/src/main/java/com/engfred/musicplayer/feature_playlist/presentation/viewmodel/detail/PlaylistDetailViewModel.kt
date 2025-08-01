package com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlayerController
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.core.domain.repository.ShuffleMode
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

    private val TAG = "PlaylistDetailViewModel"

    private val _uiState = MutableStateFlow(PlaylistDetailScreenState())
    val uiState: StateFlow<PlaylistDetailScreenState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var currentPlaylistId: Long? = null

    init {
        sharedAudioDataSource.deviceAudioFiles.onEach {
            _uiState.update { currentState ->
                currentState.copy(allAudioFiles = it)
            }
        }.launchIn(viewModelScope)
        loadPlaylistDetails(savedStateHandle)
        startObservingPlaybackState(playerController)

        // Filter out the current playlist from the list of playlists available for "Add to another playlist"
        playlistRepository.getPlaylists().onEach { allPlaylists ->
            _uiState.update { currentState ->
                currentState.copy(
                    playlists = allPlaylists.filter { playlist -> playlist.id != currentPlaylistId && !playlist.isAutomatic }
                )
            }
        }.launchIn(viewModelScope)
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
            val currentPlaylist = _uiState.value.playlist

            when (event) {
                is PlaylistDetailEvent.ShowRemoveSongConfirmation -> {
                    if (currentPlaylist?.isAutomatic == true) {
                        _uiEvent.emit("Cannot remove songs from automatic playlists.")
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            showRemoveSongConfirmationDialog = true,
                            audioFileToRemove = event.audioFile
                        )
                    }
                }
                PlaylistDetailEvent.DismissRemoveSongConfirmation -> {
                    _uiState.update {
                        it.copy(
                            showRemoveSongConfirmationDialog = false,
                            audioFileToRemove = null
                        )
                    }
                }
                PlaylistDetailEvent.ConfirmRemoveSong -> {
                    val audioFileToRemove = _uiState.value.audioFileToRemove
                    val playlistId = currentPlaylistId

                    if (playlistId != null && audioFileToRemove != null) {
                        if (currentPlaylist?.isAutomatic == true) {
                            _uiEvent.emit("Cannot remove songs from automatic playlists.")
                            return@launch
                        }
                        try {
                            playlistRepository.removeSongFromPlaylist(playlistId, audioFileToRemove.id)
                            playerController.removeFromQueue(audioFileToRemove)
                            _uiEvent.emit("Removed '${audioFileToRemove.title}' from playlist.")
                            Log.d(TAG, "Removed song ID: ${audioFileToRemove.id} from playlist ID: $playlistId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to remove song from playlist: ${e.message}", e)
                            _uiEvent.emit("Failed to remove song!")
                        } finally {
                            _uiState.update {
                                it.copy(
                                    showRemoveSongConfirmationDialog = false,
                                    audioFileToRemove = null
                                )
                            }
                        }
                    } else {
                        _uiEvent.emit("No song or playlist selected for removal.")
                        _uiState.update {
                            it.copy(
                                showRemoveSongConfirmationDialog = false,
                                audioFileToRemove = null
                            )
                        }
                    }
                }
                is PlaylistDetailEvent.RenamePlaylist -> {
                    if (currentPlaylist?.isAutomatic == true) {
                        _uiEvent.emit("Cannot rename automatic playlists.")
                        _uiState.update { it.copy(showRenameDialog = false) }
                        return@launch
                    }
                    currentPlaylistId?.let {
                        if (currentPlaylist != null && event.newName.isNotBlank()) {
                            try {
                                val updatedPlaylist = currentPlaylist.copy(name = event.newName)
                                playlistRepository.updatePlaylist(updatedPlaylist)
                                _uiState.update { it.copy(showRenameDialog = false) }
                                _uiEvent.emit("Playlist renamed to '${event.newName}'.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to rename playlist: ${e.message}", e)
                                _uiEvent.emit("Failed to rename playlist!")
                            }
                        } else {
                            _uiEvent.emit("Invalid playlist name!")
                        }
                    }
                }
                PlaylistDetailEvent.ShowRenameDialog -> {
                    if (currentPlaylist?.isAutomatic == true) {
                        _uiEvent.emit("Cannot rename automatic playlists.")
                        return@launch
                    }
                    _uiState.update { it.copy(showRenameDialog = true, error = null) }
                }
                PlaylistDetailEvent.HideRenameDialog -> {
                    _uiState.update { it.copy(showRenameDialog = false, error = null) }
                }
                is PlaylistDetailEvent.AddSong -> {
                    if (currentPlaylist?.isAutomatic == true) {
                        _uiEvent.emit("Cannot manually add songs to automatic playlists.")
                        return@launch
                    }
                    currentPlaylistId?.let { playlistId ->
                        val playlistSongs = uiState.value.playlist?.songs?.map { it.id } ?: emptyList()
                        if (!playlistSongs.contains(event.audioFile.id)) {
                            try {
                                playlistRepository.addSongToPlaylist(playlistId, event.audioFile)
                                _uiEvent.emit("Song added to playlist.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to add song to playlist: ${e.message}", e)
                                _uiEvent.emit("Failed to add song to playlist!")
                            }
                        } else {
                            Log.d(TAG, "Song ${event.audioFile.title} is already in playlist.")
                            _uiEvent.emit("Song already in this playlist.")
                        }
                    }
                }
                is PlaylistDetailEvent.PlaySong -> {
                    startAudioPlayback(event.audioFile)
                }
                PlaylistDetailEvent.ShufflePlay -> {
                    uiState.value.playlist?.songs?.let {
                        if (it.isNotEmpty()) {
                            playerController.setShuffleMode(ShuffleMode.ON)
                            startAudioPlayback(it.shuffled()[0])
                        } else {
                            _uiEvent.emit("Playlist is empty, cannot shuffle play.")
                        }
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
                    playerController.skipToNext()
                }
                PlaylistDetailEvent.PlayPause -> {
                    playerController.playPause()
                }

                PlaylistDetailEvent.PlayPrev -> {
                    playerController.skipToPrevious()
                }

                is PlaylistDetailEvent.AddedSongToPlaylist -> {
                    val audioFile = _uiState.value.audioToAddToPlaylist
                    if (audioFile != null) {
                        val songAlreadyInPlaylist =
                            event.playlist.songs.any { it.id == audioFile.id }
                        if (songAlreadyInPlaylist) {
                            _uiEvent.emit("Song already in playlist")
                        } else {
                            try {
                                playlistRepository.addSongToPlaylist(event.playlist.id, audioFile)
                                _uiEvent.emit("Added successfully!")
                            } catch (ex: Exception) {
                                Log.e(TAG, "Failed to add song to another playlist: ${ex.message}", ex)
                                _uiEvent.emit("Failed to add song to playlist!")
                            }
                        }
                    }
                }
                PlaylistDetailEvent.DismissAddToPlaylistDialog -> {
                    Log.d(TAG, "DismissAddToPlaylistDialog event received")
                    _uiState.update { it.copy(audioToAddToPlaylist = null, showAddToPlaylistDialog = false) }
                }
                is PlaylistDetailEvent.ShowPlaylistsDialog -> {
                    Log.d(TAG, "ShowPlaylistsDialog event received")
                    _uiState.update {
                        it.copy(
                            audioToAddToPlaylist = event.audioFile,
                            showAddToPlaylistDialog = true
                        )
                    }
                }

                is PlaylistDetailEvent.SetPlayNext -> {
                    Log.d(TAG, "SetPlayNext event received")
                    playerController.addAudioToQueueNext(event.audioFile)
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