package com.engfred.musicplayer.feature_player.presentation.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.engfred.musicplayer.feature_player.presentation.components.EtherealFlowLayout
import com.engfred.musicplayer.feature_player.presentation.components.ImmersiveCanvasLayout
import com.engfred.musicplayer.feature_player.presentation.components.MinimalistGrooveLayout
import com.engfred.musicplayer.feature_player.presentation.components.LayoutSelectionBottomSheet // Keep this import
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout // Keep this import

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color // Not strictly needed, but might be from prior uses

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState: PlaybackState by viewModel.uiState.collectAsState()

    // --- State for layout selection now handled directly in the UI Composable ---
    var selectedLayout by remember { mutableStateOf(PlayerLayout.ETHEREAL_FLOW) }
    // --- End UI Composable state handling ---

    // State to control bottom sheet visibility
    var showLayoutSheet by remember { mutableStateOf(false) }

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
                PlayerLayout.ETHEREAL_FLOW -> EtherealFlowLayout(uiState = uiState, onEvent = viewModel::onEvent)
                PlayerLayout.IMMERSIVE_CANVAS -> ImmersiveCanvasLayout(uiState = uiState, onEvent = viewModel::onEvent)
                PlayerLayout.MINIMALIST_GROOVE -> MinimalistGrooveLayout(uiState = uiState, onEvent = viewModel::onEvent)
            }

            // --- Layout Switcher Button (Overlayed at the top right) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { showLayoutSheet = true }, // Show the bottom sheet
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Change Player Layout",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // --- Layout Selection Bottom Sheet ---
            if (showLayoutSheet) {
                LayoutSelectionBottomSheet(
                    currentLayout = selectedLayout,
                    onLayoutSelected = { newLayout ->
                        selectedLayout = newLayout // Directly update the local state
                        showLayoutSheet = false // Dismiss sheet after selection
                    },
                    onDismissRequest = { showLayoutSheet = false } // Dismiss sheet when tapped outside
                )
            }
        }
    }
}