package com.engfred.musicplayer.feature_player.presentation.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.repository.PlaybackState
import com.engfred.musicplayer.feature_player.presentation.components.layouts.EtherealFlowLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.ImmersiveCanvasLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.MinimalistGrooveLayout
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel

@RequiresApi(Build.VERSION_CODES.M)
@UnstableApi
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onNavigateUp: () -> Unit
) {
    val uiState: PlaybackState by viewModel.uiState.collectAsState()
    val selectedLayout: PlayerLayout? by viewModel.playerLayoutState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.error != null) {
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
        } else {
            when (selectedLayout) {
                PlayerLayout.ETHEREAL_FLOW -> selectedLayout?.let {
                    EtherealFlowLayout(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        windowHeightSizeClass = windowHeightSizeClass,
                        windowWidthSizeClass = windowWidthSizeClass,
                        onLayoutSelected = { newLayout ->
                            viewModel.onEvent(PlayerEvent.SelectPlayerLayout(newLayout))
                        },
                        playingQueue = uiState.playingQueue,
                        currentSongIndex = uiState.playingSongIndex,
                        onPlayQueueItem = {
                            viewModel.onEvent(PlayerEvent.PlayAudioFile(it))
                        },
                        onNavigateUp = onNavigateUp,
                        playingAudio = uiState.currentAudioFile,
                        selectedLayout = it,
                        onRemoveQueueItem = { audio ->
                            viewModel.onEvent(PlayerEvent.RemovedFromQueue(audio))
                        }
                    )
                }
                PlayerLayout.IMMERSIVE_CANVAS -> selectedLayout?.let {
                    ImmersiveCanvasLayout(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        windowHeightSizeClass = windowHeightSizeClass,
                        windowWidthSizeClass = windowWidthSizeClass,
                        onLayoutSelected = { newLayout ->
                            viewModel.onEvent(PlayerEvent.SelectPlayerLayout(newLayout))
                        },
                        playingQueue = uiState.playingQueue,
                        currentSongIndex = uiState.playingSongIndex,
                        onPlayQueueItem = { audio ->
                            viewModel.onEvent(PlayerEvent.PlayAudioFile(audio))
                        },
                        onNavigateUp = onNavigateUp,
                        playingAudio = uiState.currentAudioFile,
                        selectedLayout = it,
                        onRemoveQueueItem = { audio ->
                            viewModel.onEvent(PlayerEvent.RemovedFromQueue(audio))
                        }
                    )
                }
                PlayerLayout.MINIMALIST_GROOVE -> selectedLayout?.let {
                    MinimalistGrooveLayout(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onLayoutSelected = { newLayout ->
                            viewModel.onEvent(PlayerEvent.SelectPlayerLayout(newLayout))
                        },
                        totalSongsInQueue = uiState.playingQueue.size,
                        currentSongIndex = uiState.playingSongIndex,
                        onNavigateUp = onNavigateUp,
                        selectedLayout = it,
                        windowHeightSizeClass = windowHeightSizeClass,
                        windowWidthSizeClass = windowWidthSizeClass,
                        playingQueue = uiState.playingQueue,
                        onPlayQueueItem = { audio ->
                            viewModel.onEvent(PlayerEvent.PlayAudioFile(audio))
                        },
                    )
                }
                null -> {}
            }
        }
    }
}