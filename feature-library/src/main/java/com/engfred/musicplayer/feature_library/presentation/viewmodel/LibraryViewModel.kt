package com.engfred.musicplayer.feature_library.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
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
    private val permissionHandlerUseCase: PermissionHandlerUseCase
) : ViewModel() {

    // UI state for the list of audio files
    var uiState by mutableStateOf(LibraryScreenState())
        private set // Only this ViewModel can modify this state

    init {
        // Initial check for permission when the ViewModel is created
        // This will trigger a re-composition if permission status changes
        // and the LaunchedEffect in the screen will react.
        uiState = uiState.copy(hasStoragePermission = permissionHandlerUseCase.hasAudioPermission())
        if (uiState.hasStoragePermission) {
            onEvent(LibraryEvent.LoadAudioFiles)
        }
    }

    /**
     * Processes events from the UI and updates the ViewModel's state accordingly.
     */
    fun onEvent(event: LibraryEvent) {
        when (event) {
            LibraryEvent.LoadAudioFiles -> {
                loadAudioFiles()
            }
            LibraryEvent.PermissionGranted -> {
                // Update permission status and load files if not already loading/loaded
                if (!uiState.hasStoragePermission) {
                    uiState = uiState.copy(hasStoragePermission = true)
                    loadAudioFiles()
                }
            }
            is LibraryEvent.OnAudioFileClick -> {
                // Handle audio file click event, e.g., pass to a shared player ViewModel
                // For now, we'll just log or prepare for navigation/playback
                // The actual playback logic will be in feature-player
                println("Audio file clicked: ${event.audioFile.title}")
                // You might emit a one-time event here for navigation
                // For example: _navigationEvents.emit(NavigationEvent.ToPlayer(event.audioFile.uri.toString()))
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
                    uiState = uiState.copy(
                        audioFiles = result.data ?: emptyList(),
                        isLoading = false,
                        error = null
                    )
                }
                is Resource.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Returns the permission string required for accessing audio files.
     */
    fun getRequiredPermission(): String {
        return permissionHandlerUseCase.getRequiredPermission()
    }
}


