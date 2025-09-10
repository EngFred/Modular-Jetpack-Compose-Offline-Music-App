package com.engfred.musicplayer.feature_library.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.engfred.musicplayer.feature_library.presentation.viewmodel.LibraryScreenState

@Composable
fun LibraryContent(
    uiState: LibraryScreenState,
    onAudioClick: (AudioFile) -> Unit,
    onRemoveOrDelete: (AudioFile) -> Unit,
    onPlayNext: (AudioFile) -> Unit,
    onAddToPlaylist: (AudioFile) -> Unit,
    onRetry: () -> Unit,
    isAudioPlaying: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    onEditSong: (AudioFile) -> Unit
) {
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
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    val audios = uiState.filteredAudioFiles.ifEmpty { uiState.audioFiles }
                    items(audios, key = { it.id }) { audioFile ->
                        Column {
                            AudioFileItem(
                                audioFile = audioFile,
                                isCurrentPlayingAudio = uiState.currentPlayingId == audioFile.id,
                                isAudioPlaying = isAudioPlaying,
                                onClick = onAudioClick,
                                onPlayNext = onPlayNext,
                                onAddToPlaylist = onAddToPlaylist,
                                onRemoveOrDelete = onRemoveOrDelete,
                                modifier = Modifier.fillMaxWidth(),
                                isFromLibrary = true,
                                onEditInfo = onEditSong
                            )
                            if (audioFile != audios.lastOrNull()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 80.dp, end = 15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}