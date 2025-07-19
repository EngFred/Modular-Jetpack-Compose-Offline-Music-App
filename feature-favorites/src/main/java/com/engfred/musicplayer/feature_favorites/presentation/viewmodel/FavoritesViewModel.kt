package com.engfred.musicplayer.feature_favorites.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.feature_favorites.domain.model.FavoriteAudioFile
import com.engfred.musicplayer.feature_favorites.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Favorites screen.
 * Manages the state of favorite audio files and interacts with FavoritesRepository.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    // UI state for the favorites screen
    var uiState by mutableStateOf(FavoritesScreenState())
        private set

    init {
        // Load favorite audio files when the ViewModel is initialized
        loadFavoriteAudioFiles()
    }

    /**
     * Processes events from the UI and updates the ViewModel's state or triggers actions.
     */
    fun onEvent(event: FavoritesEvent) {
        viewModelScope.launch {
            when (event) {
                is FavoritesEvent.RemoveFavorite -> {
                    favoritesRepository.removeFavoriteAudioFile(event.audioFileId)
                }
                FavoritesEvent.LoadFavorites -> {
                    loadFavoriteAudioFiles()
                }
            }
        }
    }

    private fun loadFavoriteAudioFiles() {
        favoritesRepository.getFavoriteAudioFiles().onEach { favoriteAudioFiles ->
            uiState = uiState.copy(
                favoriteAudioFiles = favoriteAudioFiles,
                isLoading = false, // Assuming initial load is successful here
                error = null
            )
        }.launchIn(viewModelScope) // Launch collection in ViewModel's scope
    }
}

/**
 * Sealed class representing all possible events that can occur on the Favorites Screen.
 */
sealed class FavoritesEvent {
    data class RemoveFavorite(val audioFileId: Long) : FavoritesEvent()
    data object LoadFavorites : FavoritesEvent()
}

/**
 * Data class representing the complete UI state for the Favorites Screen.
 */
data class FavoritesScreenState(
    val favoriteAudioFiles: List<FavoriteAudioFile> = emptyList(),
    val isLoading: Boolean = true, // Start as loading
    val error: String? = null
)