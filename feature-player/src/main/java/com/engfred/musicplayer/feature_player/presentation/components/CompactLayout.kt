//package com.engfred.musicplayer.feature_player.presentation.components
//
//import androidx.compose.foundation.layout.fillMaxSize
//import com.engfred.musicplayer.core.ui.ErrorIndicator
//
//package com.engfred.musicplayer.feature_player.presentation.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Pause
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.Repeat
//import androidx.compose.material.icons.filled.RepeatOne
//import androidx.compose.material.icons.filled.Shuffle
//import androidx.compose.material.icons.rounded.SkipNext
//import androidx.compose.material.icons.rounded.SkipPrevious
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Slider
//import androidx.compose.material3.SliderDefaults
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.hapticfeedback.HapticFeedbackType
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalHapticFeedback
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import coil.compose.AsyncImage
//import com.engfred.musicplayer.core.domain.model.repository.RepeatMode
//import com.engfred.musicplayer.core.domain.model.repository.ShuffleMode
//import com.engfred.musicplayer.core.util.formatDuration
//import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
//import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel
//
//@Composable
//fun CompactLayout(
//    uiState: PlayerUiState,
//    viewModel: PlayerViewModel,
//    sliderValue: Float,
//    onSliderValueChange: (Float) -> Unit
//) {
//    val haptic = LocalHapticFeedback.current
//
//    // Update slider value with playback position
//    LaunchedEffect(uiState.playbackPositionMs) {
//        onSliderValueChange(uiState.playbackPositionMs.toFloat())
//    }
//
//    if (uiState.error != null) {
//        ErrorIndicator(message = uiState.error)
//        return
//    }
//    if (uiState.currentAudioFile == null) {
//        ErrorIndicator(message = "No audio file selected")
//        return
//    }
//
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        AsyncImage(
//            model = uiState.currentAudioFile?.albumArtUri,
//            contentDescription = "Album Art",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier
//                .size(200.dp)
//                .clip(RoundedCornerShape(16.dp))
//                .shadow(6.dp, RoundedCornerShape(16.dp))
//                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(
//            text = uiState.currentAudioFile?.title ?: "Unknown Title",
//            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
//            color = MaterialTheme.colorScheme.onBackground,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//        Text(
//            text = uiState.currentAudioFile?.artist ?: "Unknown Artist",
//            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
//            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        FavoriteButton(uiState, viewModel, haptic)
//        Spacer(modifier = Modifier.height(16.dp))
//        Slider(
//            value = sliderValue,
//            onValueChange = onSliderValueChange,
//            onValueChangeFinished = {
//                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//                viewModel.onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
//            },
//            valueRange = 0f..uiState.totalDurationMs.toFloat().coerceAtLeast(0f),
//            colors = SliderDefaults.colors(
//                thumbColor = MaterialTheme.colorScheme.primary,
//                activeTrackColor = MaterialTheme.colorScheme.primary,
//                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
//            ),
//            modifier = Modifier.fillMaxWidth()
//        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = formatDuration(uiState.playbackPositionMs),
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//            )
//            Text(
//                text = formatDuration(uiState.totalDurationMs),
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//            )
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceEvenly,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            ControlButton(
//                icon = Icons.Default.Shuffle,
//                contentDescription = "Shuffle",
//                isActive = uiState.shuffleMode == ShuffleMode.ON,
//                onClick = {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
//                    viewModel.onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
//                },
//                size = 32.dp
//            )
//            ControlButton(
//                icon = Icons.Rounded.SkipPrevious,
//                contentDescription = "Skip Previous",
//                isActive = false,
//                onClick = {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    viewModel.onEvent(PlayerEvent.SkipToPrevious)
//                },
//                size = 40.dp
//            )
//            ControlButton(
//                icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
//                isActive = uiState.isPlaying,
//                onClick = {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    viewModel.onEvent(PlayerEvent.PlayPause)
//                },
//                size = 56.dp
//            )
//            ControlButton(
//                icon = Icons.Rounded.SkipNext,
//                contentDescription = "Skip Next",
//                isActive = false,
//                onClick = {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    viewModel.onEvent(PlayerEvent.SkipToNext)
//                },
//                size = 40.dp
//            )
//            ControlButton(
//                icon = when (uiState.repeatMode) {
//                    RepeatMode.OFF -> Icons.Default.Repeat
//                    RepeatMode.ONE -> Icons.Default.RepeatOne
//                    RepeatMode.ALL -> Icons.Default.Repeat
//                },
//                contentDescription = "Repeat Mode",
//                isActive = uiState.repeatMode != RepeatMode.OFF,
//                onClick = {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    val newRepeatMode = when (uiState.repeatMode) {
//                        RepeatMode.OFF -> RepeatMode.ALL
//                        RepeatMode.ALL -> RepeatMode.ONE
//                        RepeatMode.ONE -> RepeatMode.OFF
//                    }
//                    viewModel.onEvent(PlayerEvent.SetRepeatMode(newRepeatMode))
//                },
//                size = 32.dp
//            )
//        }
//    }
//}