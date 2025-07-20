package com.engfred.musicplayer.feature_library.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryEvent
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryScreenState
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryViewModel

@Composable
fun LibraryContent(
    uiState: LibraryScreenState,
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        uiState.filteredAudioFiles.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs found for '${uiState.searchQuery}'",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        uiState.audioFiles.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audio files found on your device.",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.filteredAudioFiles) { audioFile ->
                    AudioFileItem(
                        audioFile = audioFile,
                        onClick = { clickedAudioFile ->
                            viewModel.onEvent(LibraryEvent.OnAudioFileClick(clickedAudioFile))
                        }
                    )
                }
            }
        }
    }
}