package com.engfred.musicplayer.feature_audio_trim.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.ui.components.CustomTopBar
import com.engfred.musicplayer.feature_audio_trim.presentation.components.AudioInfoCard
import com.engfred.musicplayer.feature_audio_trim.presentation.components.CustomTrimLoadingIndicator
import com.engfred.musicplayer.feature_audio_trim.presentation.components.TrimSlider

@Composable
fun TrimScreen(
    onNavigateUp: () -> Unit,
    viewModel: TrimViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val previewPosition by viewModel.previewPositionMs.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showConfirmBackDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isTrimming) {
        showConfirmBackDialog = true
    }

    Scaffold(
        topBar = {
            CustomTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Trim Audio",
                onNavigateBack = {
                    if (uiState.isTrimming) {
                        showConfirmBackDialog = true
                    } else {
                        onNavigateUp()
                    }
                },
                showNavigationIcon = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .animateContentSize()
        ) {
            val state = uiState
            state.audioFile?.let { audioFile ->
                AudioInfoCard(audioFile = audioFile)

                Spacer(modifier = Modifier.height(16.dp))

                // Use the TrimSlider component
                TrimSlider(
                    durationMs = audioFile.duration,
                    startMs = state.startTimeMs,
                    endMs = state.endTimeMs,
                    currentPositionMs = previewPosition + state.startTimeMs,
                    isPlaying = isPreviewPlaying,
                    onStartChange = { viewModel.updateStartTime(it) },
                    onEndChange = { viewModel.updateEndTime(it) },
                    onTogglePlay = { viewModel.togglePreview() },
                    onSeekToStart = { viewModel.seekPreviewToStart() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error display
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Reset button
                OutlinedButton(
                    onClick = { viewModel.resetTrim() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isTrimming
                ) {
                    Text("Reset")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                val trimDurationMs = state.endTimeMs - state.startTimeMs
                val hasCriticalError = state.error != null && !state.error.contains("File too large")
                Button(
                    onClick = { showSaveDialog = true },
                    enabled = !state.isTrimming && !hasCriticalError && trimDurationMs >= 30000L && state.trimResult == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isTrimming) "Trimming..." else "Save Trimmed File")
                }

                // Trim result display (if success)
                state.trimResult?.let { result ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Trim Successful!", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                if (state.isTrimming) {
                    Spacer(modifier = Modifier.height(16.dp).weight(1f))
                    Box(
                        Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomTrimLoadingIndicator()
                    }
                }

            } ?: if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // optional empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audio file selected")
                }
            }
        }
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Trimmed File") },
            text = {
                Text("The trimmed file will be saved alongside the original file.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        viewModel.trimAudio()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm back dialog during trimming
    if (showConfirmBackDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmBackDialog = false },
            title = { Text("Cancel Trimming?") },
            text = { Text("This will stop the trim operation.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelTrim()
                    showConfirmBackDialog = false
                    onNavigateUp()
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmBackDialog = false }) { Text("No") }
            }
        )
    }
}