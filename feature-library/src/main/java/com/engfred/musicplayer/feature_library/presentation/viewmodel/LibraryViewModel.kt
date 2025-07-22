package com.engfred.musicplayer.feature_library.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import com.engfred.musicplayer.feature_library.domain.models.AudioMenuOption
import com.engfred.musicplayer.feature_library.domain.usecases.GetAllAudioFilesUseCase
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import com.engfred.musicplayer.feature_library.presentation.components.FilterOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow // Import
import kotlinx.coroutines.flow.StateFlow // Import
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow // Import
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update // Import
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the Library screen's state and events.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryScreenState())
    val uiState: StateFlow<LibraryScreenState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        if (permissionHandlerUseCase.hasAudioPermission()) {
            _uiState.update { it.copy(hasStoragePermission = true) }
            onEvent(LibraryEvent.LoadAudioFiles)
        } else {
            _uiState.update { it.copy(hasStoragePermission = false) }
        }

        playerController.getPlaybackState().onEach { state ->
            _uiState.update { currentState ->
                if (state.currentAudioFile != null && state.isPlaying) {
                    currentState.copy(currentPlayingId = state.currentAudioFile!!.id)
                } else if (!state.isPlaying) {
                    // If paused or stopped, clear current playing ID if it's the same song
                    if (currentState.currentPlayingId == state.currentAudioFile?.id) {
                        currentState.copy(currentPlayingId = null)
                    } else {
                        currentState
                    }
                } else {
                    currentState
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: LibraryEvent) {
        viewModelScope.launch {
            when (event) {
                LibraryEvent.LoadAudioFiles -> {
                    loadAudioFiles()
                }
                LibraryEvent.PermissionGranted -> {
                    if (!uiState.value.hasStoragePermission) {
                        _uiState.update { it.copy(hasStoragePermission = true) }
                        loadAudioFiles()
                    }
                }
                is LibraryEvent.OnAudioFileClick -> {
                    val audioFiles = uiState.value.filteredAudioFiles.ifEmpty { uiState.value.audioFiles }
                    sharedAudioDataSource.setAudioFiles(audioFiles)
                    try {
                        playerController.initiatePlayback(event.audioFile.uri)
                        _uiState.update { it.copy(currentPlayingId = event.audioFile.id) }
                        Log.d("LibraryViewModel", "Initiated playback for: ${event.audioFile.title}")
                    } catch (e: Exception) {
                        Log.e("LibraryViewModel", "Error playing audio file from Library: ${e.message}", e)
                        _uiEvent.emit("Failed to play song: ${e.message}")
                    }
                }
                is LibraryEvent.OnSwipeToNowPlaying -> {
                    Log.d("LibraryViewModel", "Swipe to now-playing triggered for: ${event.audioFile.title}")
                }
                is LibraryEvent.OnSearchQueryChanged -> {
                    _uiState.update { it.copy(searchQuery = event.query) }
                    applySearchAndFilter()
                }
                is LibraryEvent.OnFilterSelected -> {
                    _uiState.update { it.copy(currentFilterOption = event.filterOption) }
                    applySearchAndFilter()
                }
                is LibraryEvent.OnMenuOptionSelected -> {
                    when (event.option) {
                        AudioMenuOption.PLAY_NEXT -> {
                            Log.d("LibraryViewModel", "Play Next selected for: ${event.audioFile.title}")
                            playerController.addAudioToQueueNext(event.audioFile)
                        }
                        AudioMenuOption.ADD_TO_ALBUM -> {
                            Log.d("LibraryViewModel", "Add to Album selected for: ${event.audioFile.title}")
                            _uiEvent.emit("Added '${event.audioFile.title}' to a playlist (feature not yet implemented).")
                        }
                        AudioMenuOption.DELETE -> {
                            Log.d("LibraryViewModel", "Delete selected for: ${event.audioFile.title}")
                            _uiEvent.emit("Deleting '${event.audioFile.title}' (feature not yet implemented).")
                        }
                        AudioMenuOption.SHARE -> {
                            Log.d("LibraryViewModel", "Share selected for: ${event.audioFile.title}")
                            _uiEvent.emit("Sharing '${event.audioFile.title}' (feature not yet implemented).")
                        }
                    }
                }
                LibraryEvent.CheckPermission -> {
                    _uiState.update { it.copy(hasStoragePermission = permissionHandlerUseCase.hasAudioPermission()) }
                }
            }
        }
    }

    private fun loadAudioFiles() {
        getAudioFilesUseCase().onEach { result ->
            _uiState.update { currentState ->
                when (result) {
                    is Resource.Loading -> {
                        currentState.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Resource.Success -> {
                        val audioFiles = result.data ?: emptyList()
                        currentState.copy(
                            audioFiles = audioFiles,
                            isLoading = false,
                            error = null
                        ).also {
                            // Apply search and filter immediately after loading
                            applySearchAndFilter()
                            Log.d("LibraryViewModel", "Loaded and published ${audioFiles.size} audio files.")
                        }
                    }
                    is Resource.Error -> {
                        sharedAudioDataSource.clearAudioFiles()
                        Log.e("LibraryViewModel", "Error loading audio files: ${result.message}. Cleared SharedAudioDataSource.")
                        viewModelScope.launch {
                            _uiEvent.emit("Failed to load songs: ${result.message}")
                        }
                        currentState.copy(
                            isLoading = false,
                            error = result.message,
                            filteredAudioFiles = emptyList()
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun applySearchAndFilter() {
        val currentUiState = uiState.value
        var tempFilteredList = if (currentUiState.searchQuery.isBlank()) {
            currentUiState.audioFiles
        } else {
            currentUiState.audioFiles.filter { audioFile ->
                audioFile.title.contains(currentUiState.searchQuery, ignoreCase = true) ||
                        audioFile.artist?.contains(currentUiState.searchQuery, ignoreCase = true) == true ||
                        audioFile.album?.contains(currentUiState.searchQuery, ignoreCase = true) == true
            }
        }

        tempFilteredList = sortAudioFiles(tempFilteredList, currentUiState.currentFilterOption)

        _uiState.update { it.copy(filteredAudioFiles = tempFilteredList) }
        Log.d("LibraryViewModel", "Applied search and filter. Result: ${tempFilteredList.size} items for query '${currentUiState.searchQuery}' and filter '${currentUiState.currentFilterOption}'")

        sharedAudioDataSource.setAudioFiles(tempFilteredList)
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

    fun getRequiredPermission(): String {
        return permissionHandlerUseCase.getRequiredPermission()
    }
}