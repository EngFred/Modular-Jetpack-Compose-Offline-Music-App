//package com.engfred.musicplayer.feature_player.presentation.screens
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.scaleIn
//import androidx.compose.animation.scaleOut
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
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
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
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
//fun DynamicLayout(
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
//        ErrorView(uiState.error)
//        return
//    }
//    if (uiState.currentAudioFile == null) {
//        EmptyView()
//        return
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        AnimatedVisibility(
//            visible = uiState.currentAudioFile != null,
//            enter = fadeIn(animationSpec = tween(600)) + scaleIn(animationSpec = spring()),
//            exit = fadeOut(animationSpec = tween(300))
//        ) {
//            AsyncImage(
//                model = uiState.currentAudioFile?.albumArtUri,
//                contentDescription = "Album Art",
//                contentScale = ContentScale.Crop,
//                modifier = Modifier
//                    .size(250.dp)
//                    .clip(RoundedCornerShape(20.dp))
//                    .shadow(10.dp, RoundedCornerShape(20.dp))
//                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
//                    .border(
//                        width = 1.dp,
//                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
//                        shape = RoundedCornerShape(20.dp)
//                    )
//            )
//        }
//        Spacer(modifier = Modifier.height(24.dp))
//        Text(
//            text = uiState.currentAudioFile?.title ?: "Unknown Title",
//            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
//            color = MaterialTheme.colorScheme.onBackground,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//        Text(
//            text = uiState.currentAudioFile?.artist ?: "Unknown Artist",
//            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
//            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//        Spacer(modifier = Modifier.height(24.dp))
//        FavoriteButton(uiState, viewModel, haptic)
//        Spacer(modifier = Modifier.height(24.dp))
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(16.dp))
//                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
//                .shadow(8.dp, RoundedCornerShape(16.dp))
//                .padding(16.dp)
//        ) {
//            Column {
//                Slider(
//                    value = sliderValue,
//                    onValueChange = onSliderValueChange,
//                    onValueChangeFinished = {
//                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//                        viewModel.onEvent(PlayerEvent.SeekTo(sliderValue.toLong()))
//                    },
//                    valueRange = 0f..uiState.totalDurationMs.toFloat().coerceAtLeast(0f),
//                    colors = SliderDefaults.colors(
//                        thumbColor = MaterialTheme.colorScheme.primary,
//                        activeTrackColor = Color.Transparent,
//                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
//                    ),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(
//                            brush = Brush.horizontalGradient(
//                                colors = listOf(
//                                    MaterialTheme.colorScheme.primary,
//                                    MaterialTheme.colorScheme.secondary
//                                )
//                            ),
//                            shape = RoundedCornerShape(8.dp)
//                        )
//                        .padding(2.dp)
//                )
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text(
//                        text = formatDuration(uiState.playbackPositionMs),
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//                    )
//                    Text(
//                        text = formatDuration(uiState.totalDurationMs),
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
//                    )
//                }
//                Spacer(modifier = Modifier.height(16.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceAround,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    ControlButton(
//                        icon = Icons.Default.Shuffle,
//                        contentDescription = "Shuffle",
//                        isActive = uiState.shuffleMode == ShuffleMode.ON,
//                        onClick = {
//                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                            val newShuffleMode = if (uiState.shuffleMode == ShuffleMode.ON) ShuffleMode.OFF else ShuffleMode.ON
//                            viewModel.onEvent(PlayerEvent.SetShuffleMode(newShuffleMode))
//                        }
//                    )
//                    ControlButton(
//                        icon = Icons.Rounded.SkipPrevious,
//                        contentDescription = "Skip Previous",
//                        isActive = false,
//                        onClick = {
//                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                            viewModel.onEvent(PlayerEvent.SkipToPrevious)
//                        },
//                        size = 48.dp
//                    )
//                    AnimatedVisibility(
//                        visible = true,
//                        enter = scaleIn(animationSpec = spring(dampingRatio = 0.8f)),
//                        exit = scaleOut(animationSpec = tween(200))
//                    ) {
//                        ControlButton(
//                            icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
//                            isActive = uiState.isPlaying,
//                            onClick = {
//                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                                viewModel.onEvent(PlayerEvent.PlayPause)
//                            },
//                            size = 72.dp
//                        )
//                    }
//                    ControlButton(
//                        icon = Icons.Rounded.SkipNext,
//                        contentDescription = "Skip Next",
//                        isActive = false,
//                        onClick = {
//                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                            viewModel.onEvent(PlayerEvent.SkipToNext)
//                        },
//                        size = 48.dp
//                    )
//                    ControlButton(
//                        icon = when (uiState.repeatMode) {
//                            RepeatMode.OFF -> Icons.Default.Repeat
//                            RepeatMode.ONE -> Icons.Default.RepeatOne
//                            RepeatMode.ALL -> Icons.Default.Repeat
//                        },
//                        contentDescription = "Repeat Mode",
//                        isActive = uiState.repeatMode != RepeatMode.OFF,
//                        onClick = {
//                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                            val newRepeatMode = when (uiState.repeatMode) {
//                                RepeatMode.OFF -> RepeatMode.ALL
//                                RepeatMode.ALL -> RepeatMode.ONE
//                                RepeatMode.ONE -> RepeatMode.OFF
//                            }
//                            viewModel.onEvent(PlayerEvent.SetRepeatMode(newRepeatMode))
//                        }
//                    )
//                }
//            }
//        }
//    }
//}