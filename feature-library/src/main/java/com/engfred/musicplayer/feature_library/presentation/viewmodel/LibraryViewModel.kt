package com.engfred.musicplayer.feature_library.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.repository.PlayerController
import com.engfred.musicplayer.feature_library.domain.usecases.GetAllAudioFilesUseCase
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val sharedAudioDataSource: SharedAudioDataSource,
    private val playerController: PlayerController
) : ViewModel() {

    var uiState by mutableStateOf(LibraryScreenState())
        private set

    init {
        uiState = uiState.copy(hasStoragePermission = permissionHandlerUseCase.hasAudioPermission())
        if (uiState.hasStoragePermission) {
            onEvent(LibraryEvent.LoadAudioFiles)
        }
    }

    fun onEvent(event: LibraryEvent) {
        when (event) {
            LibraryEvent.LoadAudioFiles -> {
                loadAudioFiles()
            }
            LibraryEvent.PermissionGranted -> {
                if (!uiState.hasStoragePermission) {
                    uiState = uiState.copy(hasStoragePermission = true)
                    loadAudioFiles()
                }
            }
            is LibraryEvent.OnAudioFileClick -> {
                if (uiState.audioFiles.isNotEmpty()) {
                    sharedAudioDataSource.setAudioFiles(uiState.audioFiles)
                    Log.d("LibraryViewModel", "Set SharedAudioDataSource to library songs (${uiState.audioFiles.size} items) for playback.")
                    viewModelScope.launch {
                        try {
                            playerController.initiatePlayback(event.audioFile.uri)
                            Log.d("LibraryViewModel", "Initiated playback for: ${event.audioFile.title}")
                        } catch (e: Exception) {
                            Log.e("LibraryViewModel", "Error playing audio file from Library: ${e.message}", e)
                            uiState = uiState.copy(error = "Failed to play song: ${e.message}")
                        }
                    }
                }
            }
            LibraryEvent.CheckPermission -> {
                uiState = uiState.copy(hasStoragePermission = permissionHandlerUseCase.hasAudioPermission())
            }
            is LibraryEvent.OnSearchQueryChanged -> {
                uiState = uiState.copy(searchQuery = event.query)
                filterAudioFiles(event.query)
            }
        }
    }

    private fun loadAudioFiles() {
        getAudioFilesUseCase().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    uiState = uiState.copy(
                        isLoading = true,
                        error = null
                    )
                }
                is Resource.Success -> {
                    val audioFiles = result.data ?: emptyList()
                    uiState = uiState.copy(
                        audioFiles = audioFiles,
                        filteredAudioFiles = if (uiState.searchQuery.isEmpty()) audioFiles else filterAudioFiles(uiState.searchQuery),
                        isLoading = false,
                        error = null
                    )
                    sharedAudioDataSource.setAudioFiles(audioFiles)
                    Log.d("LibraryViewModel", "Loaded and published ${audioFiles.size} audio files to SharedAudioDataSource.")
                }
                is Resource.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = result.message,
                        filteredAudioFiles = emptyList()
                    )
                    sharedAudioDataSource.clearAudioFiles()
                    Log.e("LibraryViewModel", "Error loading audio files: ${result.message}. Cleared SharedAudioDataSource.")
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun filterAudioFiles(query: String): List<AudioFile> {
        val filteredList = if (query.isBlank()) {
            uiState.audioFiles
        } else {
            uiState.audioFiles.filter { audioFile ->
                audioFile.title.contains(query, ignoreCase = true) ||
                        audioFile.artist?.contains(query, ignoreCase = true) == true ||
                        audioFile.album?.contains(query, ignoreCase = true) == true
            }
        }
        uiState = uiState.copy(filteredAudioFiles = filteredList)
        Log.d("LibraryViewModel", "Filtered audio files: ${filteredList.size} items for query '$query'")
        return filteredList
    }

    fun getRequiredPermission(): String {
        return permissionHandlerUseCase.getRequiredPermission()
    }
}