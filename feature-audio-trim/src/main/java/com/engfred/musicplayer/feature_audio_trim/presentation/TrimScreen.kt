package com.engfred.musicplayer.feature_audio_trim.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.engfred.musicplayer.core.ui.components.CustomTopBar
import com.engfred.musicplayer.core.util.MediaUtils.formatDuration
import com.engfred.musicplayer.feature_audio_trim.domain.model.TrimResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(
    onNavigateUp: () -> Unit,
    viewModel: TrimViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var showConfirmBackDialog by remember { mutableStateOf(false) }

    // BackHandler for device back button
    BackHandler(
        enabled = uiState.isTrimming
    ) {
        showConfirmBackDialog = true
    }

    // Init preview player
    DisposableEffect(Unit) {
        exoPlayer = ExoPlayer.Builder(context).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {}
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    isPreviewPlaying = false
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying && isPreviewPlaying) {
                        // Auto-stop at end or error
                        isPreviewPlaying = false
                        viewModel.resumeMainPlayerIfPaused()
                    }
                }
            })
        }
        onDispose {
            exoPlayer?.release()
        }
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
            // Capture state locally to avoid recomposition races
            val state = uiState
            val progress = state.progress ?: 0  // Default to 0 for initial display

            state.audioFile?.let { audioFile ->
                // Audio Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = audioFile.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = audioFile.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Duration: ${formatDuration(audioFile.duration)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline Sliders - Using RangeSlider for better UX
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Start: ${formatDuration(state.startTimeMs)}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "End: ${formatDuration(state.endTimeMs)}",
                        fontWeight = FontWeight.Medium
                    )
                }
                RangeSlider(
                    value = state.startTimeMs.toFloat()..state.endTimeMs.toFloat(),
                    onValueChange = {
                        viewModel.updateStartTime(it.start.toLong())
                        viewModel.updateEndTime(it.endInclusive.toLong())
                    },
                    valueRange = 0f..audioFile.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Button
                Button(
                    onClick = {
                        val player = exoPlayer ?: return@Button
                        if (isPreviewPlaying) {
                            player.stop()
                            player.seekTo(0)  // Reset to start of clip
                            isPreviewPlaying = false
                            viewModel.resumeMainPlayerIfPaused()
                        } else {
                            // Pause main player if playing
                            viewModel.pauseMainPlayerIfPlaying()
                            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(state.startTimeMs)
                                .setEndPositionMs(state.endTimeMs)
                                .build()
                            val mediaItem = MediaItem.Builder()
                                .setUri(audioFile.uri)
                                .setClippingConfiguration(clippingConfig)
                                .build()
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.playWhenReady = true
                            player.seekTo(0)  // Seek to relative 0 (start of trimmed clip)
                            isPreviewPlaying = true
                            // Monitor position to enforce end (clipping may not always work perfectly)
                            coroutineScope.launch {
                                while (isPreviewPlaying && player.isPlaying) {
                                    if (player.currentPosition >= (state.endTimeMs - state.startTimeMs)) {
                                        player.pause()
                                        isPreviewPlaying = false
                                        viewModel.resumeMainPlayerIfPaused()
                                        break
                                    }
                                    delay(100) // Check every 100ms
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPreviewPlaying) "Stop Preview" else "Play Preview"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPreviewPlaying) "Stop Preview" else "Play Preview")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Button
                TextButton(
                    onClick = { viewModel.resetTrim() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Trim")
                }

                // Save Button
                val trimDurationMs = state.endTimeMs - state.startTimeMs
                Button(
                    onClick = { showSaveDialog = true },
                    enabled = !state.isTrimming && state.error == null && trimDurationMs >= 30000L && state.trimResult == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isTrimming) "Trimming..." else "Save Trimmed File")
                }

                if (state.isTrimming) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.toFloat() / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Trimming... $progress%",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // Error/Result
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row {
                        Button(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (error.contains("timeout")) {
                            Button(onClick = {
                                showSaveDialog = true  // Reopen dialog for retry
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                state.trimResult?.let { result ->
                    if (result is TrimResult.Success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Trim saved successfully!",
                            color = MaterialTheme.colorScheme.primary
                        )
                        LaunchedEffect(result) {
                            Toast.makeText(context, "Trim saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } ?: if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Fallback empty state
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("No audio file selected")
//                }
            }
        }
    }

    // Confirmation Dialog for Back Navigation During Trimming
    if (showConfirmBackDialog && uiState.isTrimming) {
        AlertDialog(
            onDismissRequest = { showConfirmBackDialog = false },
            title = { Text("Cancel Trimming?") },
            text = { Text("This action will cancel the ongoing trimming process and you may lose progress.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmBackDialog = false
                        viewModel.cancelTrim()
                        onNavigateUp()
                    }
                ) {
                    Text("Cancel Trimming")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmBackDialog = false }) {
                    Text("Continue Trimming")
                }
            }
        )
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
}