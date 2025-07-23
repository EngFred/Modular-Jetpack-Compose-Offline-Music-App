package com.engfred.musicplayer.feature_player.presentation.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel
import com.engfred.musicplayer.core.domain.model.repository.PlaybackState
import com.engfred.musicplayer.feature_player.presentation.components.layouts.EtherealFlowLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.MinimalistGrooveLayout
import com.engfred.musicplayer.feature_player.presentation.components.layouts.ImmersiveCanvasLayout
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val uiState: PlaybackState by viewModel.uiState.collectAsState()

    // --- State for layout selection now handled directly in the UI Composable ---
    var selectedLayout by remember { mutableStateOf(PlayerLayout.ETHEREAL_FLOW) }
    // --- End UI Composable state handling ---

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
        } else if (uiState.currentAudioFile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No song selected!!",
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Main layout content based on selectedLayout
            when (selectedLayout) {
                PlayerLayout.ETHEREAL_FLOW -> EtherealFlowLayout(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    windowSizeClass = windowWidthSizeClass,
                    selectedLayout = selectedLayout,
                    onLayoutSelected = { selectedLayout = it }
                )
                PlayerLayout.IMMERSIVE_CANVAS -> ImmersiveCanvasLayout(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    windowSizeClass = windowWidthSizeClass,
                    selectedLayout = selectedLayout,
                    onLayoutSelected = { selectedLayout = it }
                )
                PlayerLayout.MINIMALIST_GROOVE -> MinimalistGrooveLayout(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    selectedLayout = selectedLayout,
                    onLayoutSelected = { selectedLayout = it }
                )
            }
        }
    }
}