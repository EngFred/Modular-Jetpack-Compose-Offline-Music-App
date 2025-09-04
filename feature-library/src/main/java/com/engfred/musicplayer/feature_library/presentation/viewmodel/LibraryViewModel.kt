package com.engfred.musicplayer.feature_library.presentation.viewmodel

import LibraryEvent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.activity.result.IntentSenderRequest
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.repository.PlaybackController
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.core.domain.model.FilterOption
import com.engfred.musicplayer.feature_library.domain.usecases.GetAllAudioFilesUseCase
import com.engfred.musicplayer.core.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.util.MediaUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playbackController: PlaybackController,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryScreenState())
    val uiState: StateFlow<LibraryScreenState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    private val _deleteRequest = MutableSharedFlow<IntentSenderRequest>()
    val deleteRequest: SharedFlow<IntentSenderRequest> = _deleteRequest.asSharedFlow()

    init {
        observePermissionState()
        startObservingPlaybackState()
        observePlaylists()
        observeFilterOption()
    }

    private fun observePermissionState() {
        val granted = permissionHandlerUseCase.hasAudioPermission() && permissionHandlerUseCase.hasWriteStoragePermission()
        _uiState.update { it.copy(hasStoragePermission = granted) }
    }

    private fun startObservingPlaybackState() {
        playbackController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                currentState.copy(
                    currentPlayingId = state.currentAudioFile?.id,
                    isPlaying = state.isPlaying
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observePlaylists() {
        playlistRepository.getPlaylists().onEach { playlists ->
            _uiState.update { it.copy(playlists = playlists.filterNot { playlist -> playlist.isAutomatic }) }
        }.launchIn(viewModelScope)
    }

    private fun observeFilterOption() {
        viewModelScope.launch {
            settingsRepository.getFilterOption().collectLatest { filterOption ->
                _uiState.update { it.copy(currentFilterOption = filterOption) }
                applySearchAndFilter()
            }
        }
    }

    fun onEvent(event: LibraryEvent) {
        viewModelScope.launch {
            when (event) {
                LibraryEvent.LoadAudioFiles -> {
                    if (_uiState.value.audioFiles.isEmpty() || _uiState.value.error != null) {
                        loadAudioFiles()
                    }
                }

                is LibraryEvent.PermissionGranted -> {
                    val granted = permissionHandlerUseCase.hasAudioPermission() &&
                            permissionHandlerUseCase.hasWriteStoragePermission()
                    _uiState.update { it.copy(hasStoragePermission = granted) }
                    if (granted && _uiState.value.audioFiles.isEmpty()) {
                        loadAudioFiles()
                    }
                }
                LibraryEvent.CheckPermission -> {
                    val granted = permissionHandlerUseCase.hasAudioPermission() &&
                            permissionHandlerUseCase.hasWriteStoragePermission()
                    _uiState.update { it.copy(hasStoragePermission = granted) }
                    if (!granted) {
                        _uiEvent.emit("Storage permission denied. Cannot load music.")
                    }
                }

                is LibraryEvent.PlayAudio -> startAudioPlayback(event.audioFile)

                is LibraryEvent.SearchQueryChanged -> {
                    _uiState.update { it.copy(searchQuery = event.query) }
                    applySearchAndFilter()
                }

                is LibraryEvent.FilterSelected -> {
                    _uiState.update { it.copy(currentFilterOption = event.filterOption) }
                    settingsRepository.updateFilterOption(event.filterOption)
                    applySearchAndFilter()
                }

                is LibraryEvent.AddedToPlaylist -> {
                    _uiState.update {
                        it.copy(showAddToPlaylistDialog = true, audioToAddToPlaylist = event.audioFile)
                    }
                }

                is LibraryEvent.AddedSongToPlaylist -> {
                    val audioFile = _uiState.value.audioToAddToPlaylist
                    if (audioFile != null) {
                        val songAlreadyExists = event.playlist.songs.any { it.id == audioFile.id }
                        if (songAlreadyExists) {
                            _uiEvent.emit("Song already in playlist")
                        } else {
                            try {
                                playlistRepository.addSongToPlaylist(event.playlist.id, audioFile)
                                _uiEvent.emit("Added to playlist successfully!")
                            } catch (e: Exception) {
                                _uiEvent.emit("Failed to add song to playlist: ${e.message}")
                            }
                        }
                        _uiState.update { it.copy(audioToAddToPlaylist = null, showAddToPlaylistDialog = false) }
                    } else {
                        _uiEvent.emit("Failed to add song to playlist (no song selected).")
                    }
                }

                LibraryEvent.DismissAddToPlaylistDialog -> {
                    _uiState.update {
                        it.copy(showAddToPlaylistDialog = false, audioToAddToPlaylist = null)
                    }
                }

                is LibraryEvent.ShowDeleteConfirmation -> {
                    _uiState.update {
                        it.copy(showDeleteConfirmationDialog = true, audioFileToDelete = event.audioFile)
                    }
                }

                LibraryEvent.DismissDeleteConfirmationDialog -> {
                    _uiState.update {
                        it.copy(showDeleteConfirmationDialog = false, audioFileToDelete = null)
                    }
                }

                LibraryEvent.ConfirmDeleteAudioFile -> {
                    _uiState.value.audioFileToDelete?.let { audioFile ->
                        val intentSender = MediaUtils.deleteAudioFile(context, audioFile) { success, errorMessage ->
                            onEvent(LibraryEvent.DeletionResult(audioFile, success, errorMessage))
                        }

                        if (intentSender != null) {
                            _deleteRequest.emit(IntentSenderRequest.Builder(intentSender).build())
                        }
                    } ?: _uiEvent.emit("No song selected for deletion.")
                    _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
                }

                is LibraryEvent.DeletionResult -> {
                    val audioFile = event.audioFile
                    if (event.success) {
                        _uiState.update { currentState ->
                            val updatedList = currentState.audioFiles.filter { it.id != audioFile.id }

                            val filteredList = updatedList.filter {
                                it.title.contains(currentState.searchQuery, ignoreCase = true) ||
                                        it.artist?.contains(currentState.searchQuery, ignoreCase = true) == true ||
                                        it.album?.contains(currentState.searchQuery, ignoreCase = true) == true
                            }

                            val sorted = sortAudioFiles(filteredList, currentState.currentFilterOption)

                            sharedAudioDataSource.setPlayingQueue(sorted)

                            currentState.copy(
                                audioFiles = updatedList,
                                filteredAudioFiles = sorted,
                                showDeleteConfirmationDialog = false,
                                audioFileToDelete = null
                            )
                        }

                        playbackController.onAudioFileRemoved(audioFile)
                        sharedAudioDataSource.deleteAudioFile(audioFile)
                        _uiEvent.emit("Successfully deleted '${audioFile.title}'.")
                    } else {
                        _uiEvent.emit(event.errorMessage ?: "Failed to delete '${audioFile.title}'.")
                        _uiState.update { it.copy(showDeleteConfirmationDialog = false, audioFileToDelete = null) }
                    }
                }

                is LibraryEvent.PlayedNext -> {
                    playbackController.addAudioToQueueNext(event.audioFile)
                    _uiEvent.emit("Added '${event.audioFile.title}' to play next.")
                }

                LibraryEvent.Retry -> loadAudioFiles()
            }
        }
    }

    private fun applySearchAndFilter() {
        _uiState.update { current ->
            val filtered = if (current.searchQuery.isBlank()) {
                current.audioFiles
            } else {
                current.audioFiles.filter {
                    it.title.contains(current.searchQuery, ignoreCase = true) ||
                            it.artist?.contains(current.searchQuery, ignoreCase = true) == true ||
                            it.album?.contains(current.searchQuery, ignoreCase = true) == true
                }
            }

            val sortedFiltered = sortAudioFiles(filtered, current.currentFilterOption)
            sharedAudioDataSource.setPlayingQueue(sortedFiltered)

            current.copy(filteredAudioFiles = sortedFiltered)
        }
    }

    private fun sortAudioFiles(list: List<AudioFile>, filterOption: FilterOption): List<AudioFile> {
        return when (filterOption) {
            FilterOption.DATE_ADDED_ASC -> list.sortedBy { it.dateAdded }
            FilterOption.DATE_ADDED_DESC -> list.sortedByDescending { it.dateAdded }
            FilterOption.LENGTH_ASC -> list.sortedBy { it.duration }
            FilterOption.LENGTH_DESC -> list.sortedByDescending { it.duration }
            FilterOption.ALPHABETICAL_ASC -> list.sortedBy { it.title.lowercase() }
            FilterOption.ALPHABETICAL_DESC -> list.sortedByDescending { it.title.lowercase() }
        }
    }

    private suspend fun startAudioPlayback(audioFile: AudioFile) {
        val list = _uiState.value.filteredAudioFiles.ifEmpty { _uiState.value.audioFiles }
        sharedAudioDataSource.setPlayingQueue(list)
        playbackController.initiatePlayback(audioFile.uri)
    }

    fun getRequiredPermission(): String {
        return permissionHandlerUseCase.getRequiredReadPermission()
    }

    private fun loadAudioFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getAudioFilesUseCase().collect { result ->
                _uiState.update { currentState ->
                    when (result) {
                        is Resource.Success -> {
                            val audioFiles = result.data ?: emptyList()
                            val sortedFiltered = sortAudioFiles(audioFiles, currentState.currentFilterOption)
                            sharedAudioDataSource.setDeviceAudioFiles(audioFiles)
                            sharedAudioDataSource.setPlayingQueue(sortedFiltered)
                            currentState.copy(
                                audioFiles = audioFiles,
                                filteredAudioFiles = sortedFiltered,
                                isLoading = false,
                                error = null
                            )
                        }

                        is Resource.Error -> {
                            sharedAudioDataSource.clearPlayingQueue()
                            _uiEvent.emit("Failed to load songs: ${result.message}")
                            currentState.copy(
                                isLoading = false,
                                error = result.message,
                                filteredAudioFiles = emptyList()
                            )
                        }

                        is Resource.Loading -> currentState
                    }
                }
            }
        }
    }
}