package com.engfred.musicplayer.feature_library.presentation.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.ui.AudioFileItem
import com.engfred.musicplayer.core.ui.ErrorIndicator
import com.engfred.musicplayer.core.ui.InfoIndicator
import com.engfred.musicplayer.core.ui.LoadingIndicator
import com.engfred.musicplayer.feature_library.domain.models.AudioMenuOption
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryScreenState

/**
 * Composable to display the content of the Library screen, including a list of audio files or appropriate indicators.
 *
 * @param uiState The current UI state of the Library screen.
 * @param onAudioClick Callback when an audio file is clicked.
 * @param onSwipeToNowPlaying Callback when an audio file is swiped to navigate to the now-playing screen.
 * @param onMenuOptionSelected Callback when a menu option is selected for an "AudioFileItem".
 * @param onRetry Callback when the retry button is clicked on error.
 * @param snackbarHostState The SnackbarHostState to pass to AudioFileItem.
 * @param modifier Modifier for the composable.
 */
@Composable
fun LibraryContent(
    uiState: LibraryScreenState,
    onAudioClick: (AudioFile) -> Unit,
    onSwipeToNowPlaying: (AudioFile) -> Unit,
    onMenuOptionSelected: (AudioMenuOption, AudioFile) -> Unit,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
    ) {
        when {
            uiState.isLoading -> {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                ErrorIndicator(
                    message = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.filteredAudioFiles.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                InfoIndicator(
                    message = "No songs found for \"${uiState.searchQuery}\".",
                    icon = Icons.Outlined.SearchOff,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.audioFiles.isEmpty() -> {
                InfoIndicator(
                    message = "No audio files found on your device. Ensure storage permission is granted.",
                    icon = Icons.Outlined.FolderOff,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val audios = uiState.filteredAudioFiles.ifEmpty { uiState.audioFiles }
                    items(audios, key = { it.id }) { audioFile ->
                        AudioFileItem(
                            audioFile = audioFile,
                            isPlaying = uiState.currentPlayingId == audioFile.id,
                            onClick = onAudioClick,
                            onSwipeToNowPlaying = onSwipeToNowPlaying,
                            onPlayNext = { onMenuOptionSelected(AudioMenuOption.PLAY_NEXT, audioFile) },
                            onAddToAlbum = { onMenuOptionSelected(AudioMenuOption.ADD_TO_ALBUM, audioFile) },
                            onDelete = { onMenuOptionSelected(AudioMenuOption.DELETE, audioFile) },
                            onShare = { onMenuOptionSelected(AudioMenuOption.SHARE, audioFile) },
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.fillMaxWidth() // Add this back if it was removed unintentionally, or adjust as needed
                        )
                    }
                }
            }
        }
    }
}