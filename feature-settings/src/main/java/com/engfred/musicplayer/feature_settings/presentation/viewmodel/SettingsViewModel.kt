package com.engfred.musicplayer.feature_settings.presentation.viewmodel

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.feature_settings.domain.usecases.GetAppSettingsUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdateAudioPresetUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdatePlayerLayoutUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdatePlaylistLayoutUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdateThemeUseCase
import com.engfred.musicplayer.feature_settings.domain.usecases.UpdateWidgetBackgroundModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getAppSettingsUseCase: GetAppSettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updatePlayerLayoutUseCase: UpdatePlayerLayoutUseCase,
    private val updatePlaylistLayoutUseCase: UpdatePlaylistLayoutUseCase,
    private val updateAudioPresetUseCase: UpdateAudioPresetUseCase,
    private val updateWidgetBackgroundModeUseCase: UpdateWidgetBackgroundModeUseCase
) : ViewModel() {

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
                    audioPreset = appSettings.audioPreset,
                    widgetBackgroundMode = appSettings.widgetBackgroundMode,
                    isLoading = false, // Settings loaded, so not loading
                    error = null // Clear any previous error
                )
            }
        }.launchIn(viewModelScope) // Launch collection in ViewModel's scope
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.UpdateTheme -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    try {
                        updateThemeUseCase(event.theme)
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
                is SettingsEvent.UpdateAudioPreset -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    try {
                        updateAudioPresetUseCase(event.preset)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = "Failed to update audio preset: ${e.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }
                is SettingsEvent.UpdateWidgetBackgroundMode -> {
                    val appContext = event.context
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    try {
                        updateWidgetBackgroundModeUseCase(event.mode)
                        // Notify widget provider in the app module. Use strings so we don't depend on that module.
                        try {
                            val intent = Intent().apply {
                                // explicit ComponentName (package, fully-qualified receiver class name)
                                component = ComponentName(
                                    appContext.packageName,
                                    "com.engfred.musicplayer.widget.PlayerWidgetProvider"
                                )
                                action = "com.engfred.musicplayer.ACTION_UPDATE_WIDGET"
                                `package` = appContext.packageName
                            }
                            appContext.sendBroadcast(intent)
                        } catch (bex: Exception) {
                            Log.w("SettingsViewModel", "Failed to notify widget provider: ${bex.message}")
                        }

                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                error = "Failed to update widget mode: ${e.localizedMessage}",
                                isLoading = false
                            )
                        }
                    }
                }

            }
        }
    }
}
