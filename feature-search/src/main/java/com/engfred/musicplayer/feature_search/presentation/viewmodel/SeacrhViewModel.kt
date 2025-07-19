package com.engfred.musicplayer.feature_search.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engfred.musicplayer.core.common.Resource
import com.engfred.musicplayer.feature_library.domain.model.AudioFile
import com.engfred.musicplayer.feature_search.domain.usecases.SearchAudioFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Search screen.
 * Manages the search query, triggers search operations, and exposes results.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAudioFilesUseCase: SearchAudioFilesUseCase
) : ViewModel() {

    // UI state for the search screen
    var uiState by mutableStateOf(SearchScreenState())
        private set

    private var searchJob: Job? = null // To debounce search queries

    /**
     * Processes events from the UI and updates the ViewModel's state or triggers actions.
     */
    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.UpdateSearchQuery -> {
                uiState = uiState.copy(searchQuery = event.query)
                // Debounce the search to avoid too many calls while typing
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(500L) // Wait for 500ms after last key press
                    performSearch(event.query)
                }
            }
            SearchEvent.ClearSearch -> {
                uiState = uiState.copy(searchQuery = "", searchResultsResource = Resource.Success(emptyList()))
                searchJob?.cancel() // Cancel any ongoing search
            }
        }
    }

    private fun performSearch(query: String) {
        searchAudioFilesUseCase(query).onEach { result ->
            uiState = uiState.copy(searchResultsResource = result)
        }.launchIn(viewModelScope)
    }
}

/**
 * Sealed class representing all possible events that can occur on the Search Screen.
 */
sealed class SearchEvent {
    data class UpdateSearchQuery(val query: String) : SearchEvent()
    data object ClearSearch : SearchEvent()
}

/**
 * Data class representing the complete UI state for the Search Screen.
 * Uses Resource for search results.
 */
data class SearchScreenState(
    val searchQuery: String = "",
    val searchResultsResource: Resource<List<AudioFile>> = Resource.Success(emptyList()) // Start with empty success
) {
    // Convenience getters for UI to access data and loading/error states
    val searchResults: List<AudioFile>
        get() = (searchResultsResource as? Resource.Success)?.data ?: emptyList()

    val isLoading: Boolean
        get() = searchResultsResource is Resource.Loading

    val hasError: Boolean
        get() = searchResultsResource is Resource.Error

    val errorMessage: String?
        get() = (searchResultsResource as? Resource.Error)?.message
}