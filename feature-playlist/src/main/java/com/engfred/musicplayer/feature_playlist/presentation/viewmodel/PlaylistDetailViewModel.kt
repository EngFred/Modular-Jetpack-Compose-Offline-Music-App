package com.engfred.musicplayer.feature_playlist.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var uiState by mutableStateOf(PlaylistDetailScreenState())
        private set

    private var currentPlaylistId: Long? = null
    private var allDeviceAudioFiles: List<AudioFile> = emptyList()

    init {
        // Collect all audio files from SharedAudioDataSource for UI state
        sharedAudioDataSource.allAudioFiles.onEach { audioFiles ->
            uiState = uiState.copy(allAudioFiles = audioFiles)
            // Store initial device songs for AddSongsDialog (before playback queue changes)
            if (allDeviceAudioFiles.isEmpty()) {
                allDeviceAudioFiles = audioFiles
                android.util.Log.d("PlaylistDetailViewModel", "Stored ${allDeviceAudioFiles.size} device songs for AddSongsDialog.")
            }
        }.launchIn(viewModelScope)

        // Load playlist details
        savedStateHandle.get<Long>(PlaylistDetailArgs.PLAYLIST_ID)?.let { playlistId ->
            currentPlaylistId = playlistId
            loadPlaylistDetails(playlistId)
        } ?: run {
            uiState = uiState.copy(error = "Playlist ID not provided.")
        }
    }

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
                        val currentPlaylist = uiState.playlist
                        if (currentPlaylist != null && event.newName.isNotBlank()) {
                            val updatedPlaylist = currentPlaylist.copy(name = event.newName)
                            playlistRepository.updatePlaylist(updatedPlaylist)
                            uiState = uiState.copy(showRenameDialog = false)
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
                PlaylistDetailEvent.ShowAddSongsDialog -> {
                    uiState = uiState.copy(showAddSongsDialog = true)
                }
                PlaylistDetailEvent.HideAddSongsDialog -> {
                    uiState = uiState.copy(showAddSongsDialog = false, error = null)
                }
                is PlaylistDetailEvent.AddSong -> {
                    currentPlaylistId?.let { playlistId ->
                        val currentSongs = uiState.playlist?.songs?.map { it.id } ?: emptyList()
                        if (!currentSongs.contains(event.audioFile.id)) {
                            playlistRepository.addSongToPlaylist(playlistId, event.audioFile)
                        }
                    }
                }
                is PlaylistDetailEvent.PlaySong -> {
                    val playlistSongs = uiState.playlist?.songs ?: emptyList()
                    if (playlistSongs.isNotEmpty()) {
                        // Update SharedAudioDataSource with playlist songs for playback queue
                        sharedAudioDataSource.setAudioFiles(playlistSongs)
                        android.util.Log.d("PlaylistDetailViewModel", "Set playback queue to ${playlistSongs.size} playlist songs.")
                        // Trigger playback of the selected song
                        event.onPlay(event.audioFile.uri.toString())
                    }
                }
            }
        }
    }

    fun getAllDeviceAudioFiles(): List<AudioFile> {
        return allDeviceAudioFiles
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
                    playlist = null
                )
            }
        }.launchIn(viewModelScope)
    }
}

sealed class PlaylistDetailEvent {
    data class RemoveSong(val audioFileId: Long) : PlaylistDetailEvent()
    data class RenamePlaylist(val newName: String) : PlaylistDetailEvent()
    data object ShowRenameDialog : PlaylistDetailEvent()
    data object HideRenameDialog : PlaylistDetailEvent()
    data object ShowAddSongsDialog : PlaylistDetailEvent()
    data object HideAddSongsDialog : PlaylistDetailEvent()
    data class AddSong(val audioFile: AudioFile) : PlaylistDetailEvent()
    data class PlaySong(val audioFile: AudioFile, val onPlay: (String) -> Unit) : PlaylistDetailEvent()
}

data class PlaylistDetailScreenState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRenameDialog: Boolean = false,
    val showAddSongsDialog: Boolean = false,
    val allAudioFiles: List<AudioFile> = emptyList()
)