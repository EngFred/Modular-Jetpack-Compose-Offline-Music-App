package com.engfred.musicplayer.feature_library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.feature_library.domain.usecases.GetAllAudioFilesUseCase
import com.engfred.musicplayer.feature_library.domain.usecases.PermissionHandlerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for the music library screen.
 * Handles fetching audio files, managing permissions, and exposing UI state.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getAudioFilesUseCase: GetAllAudioFilesUseCase,
    private val permissionHandlerUseCase: PermissionHandlerUseCase,
    private val sharedAudioDataSource: SharedAudioDataSource
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
                println("Audio file clicked: ${event.audioFile.title}")
            }
            LibraryEvent.CheckPermission -> {
                uiState = uiState.copy(hasStoragePermission = permissionHandlerUseCase.hasAudioPermission())
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
                        isLoading = false,
                        error = null
                    )
                    // *** IMPORTANT: Publish the loaded files to the shared data source ***
                    sharedAudioDataSource.setAudioFiles(audioFiles)
                    android.util.Log.d("LibraryViewModel", "Loaded and published ${audioFiles.size} audio files to SharedAudioDataSource.")
                }
                is Resource.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = result.message
                    )
                    // Clear shared data on error to prevent using stale data
                    sharedAudioDataSource.clearAudioFiles()
                    android.util.Log.e("LibraryViewModel", "Error loading audio files: ${result.message}. Cleared SharedAudioDataSource.")
                }
            }
        }.launchIn(viewModelScope)
    }

    fun getRequiredPermission(): String {
        return permissionHandlerUseCase.getRequiredPermission()
    }
}

