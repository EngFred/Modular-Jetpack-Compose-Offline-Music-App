package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdateThemeUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdatePlayerLayoutUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdatePlaylistLayoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings Screen.
 * Manages UI state related to app settings and handles user interactions.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    getAppSettingsUseCase: GetAppSettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updatePlayerLayoutUseCase: UpdatePlayerLayoutUseCase,
    private val updatePlaylistLayoutUseCase: UpdatePlaylistLayoutUseCase
) : ViewModel() {

    // Change from 'var uiState by mutableStateOf' to MutableStateFlow
    private val _uiState = MutableStateFlow(SettingsScreenState())
    val uiState: StateFlow<SettingsScreenState> = _uiState.asStateFlow()

    init {
        // Observe app settings from the repository via the use case
        getAppSettingsUseCase().onEach { appSettings ->
            _uiState.update {
                it.copy(
                    selectedTheme = appSettings.selectedTheme,
                    selectedPlayerLayout = appSettings.selectedPlayerLayout,
                    playlistLayoutType = appSettings.playlistLayoutType,
                    isLoading = false, // Settings loaded, so not loading
                    error = null // Clear any previous error
                )
            }
        }.launchIn(viewModelScope) // Launch collection in ViewModel's scope
    }

    /**
     * Processes events from the UI and updates the ViewModel's state or triggers actions.
     */
    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.UpdateTheme -> {
                    _uiState.update { it.copy(isLoading = true, error = null) } // Indicate saving
                    try {
                        updateThemeUseCase(event.theme)
                        // UI state will be updated by the Flow observation in init block
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = "Failed to update theme: ${e.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }
                is SettingsEvent.UpdatePlayerLayout -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    try {
                        updatePlayerLayoutUseCase(event.layout)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = "Failed to update player layout: ${e.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }
                is SettingsEvent.UpdatePlaylistLayout -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    try {
                        updatePlaylistLayoutUseCase(event.layout)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = "Failed to update playlist layout: ${e.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }
}