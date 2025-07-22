//package com.engfred.musicplayer.feature_player.presentation.components
//
//import androidx.annotation.OptIn
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.scaleIn
//import androidx.compose.animation.scaleOut
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.interaction.PressInteraction
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Favorite
//import androidx.compose.material.icons.filled.FavoriteBorder
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.hapticfeedback.HapticFeedbackType
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalHapticFeedback
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.media3.common.util.UnstableApi
//import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerEvent
//import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerViewModel
//
//@OptIn(UnstableApi::class)
//@Composable
//fun FavoriteButton(
//    uiState: PlayerUiState,
//    viewModel: PlayerViewModel,
//    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
//) {
//    val interactionSource = remember { MutableInteractionSource() }
//    AnimatedVisibility(
//        visible = true,
//        enter = scaleIn(animationSpec = spring(dampingRatio = 0.8f)),
//        exit = scaleOut(animationSpec = tween(200))
//    ) {
//        Icon(
//            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//            contentDescription = if (uiState.isFavorite) "Remove from Favorites" else "Add to Favorites",
//            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
//            modifier = Modifier
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
//                .clickable(
//                    interactionSource = interactionSource,
//                    indication = null
//                ) {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    uiState.currentAudioFile?.let {
//                        if (uiState.isFavorite) {
//                            viewModel.onEvent(PlayerEvent.RemoveFromFavorites(it.id))
//                        } else {
//                            viewModel.onEvent(PlayerEvent.AddToFavorites(it))
//                        }
//                    }
//                }
//                .padding(8.dp)
//        )
//    }
//}
//
//@Composable
//fun ControlButton(
//    icon: androidx.compose.ui.graphics.vector.ImageVector,
//    contentDescription: String,
//    isActive: Boolean,
//    onClick: () -> Unit,
//    size: androidx.compose.ui.unit.Dp = 36.dp
//) {
//    val interactionSource = remember { MutableInteractionSource() }
//    val haptic = LocalHapticFeedback.current
//    AnimatedVisibility(
//        visible = true,
//        enter = scaleIn(animationSpec = spring(dampingRatio = 0.8f)),
//        exit = scaleOut(animationSpec = tween(200))
//    ) {
//        Box(
//            modifier = Modifier
//                .size(size + 16.dp)
//                .clip(CircleShape)
//                .background(
//                    brush = Brush.radialGradient(
//                        colors = listOf(
//                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
//                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
//                        )
//                    ),
//                    shape = CircleShape
//                )
//                .shadow(4.dp, CircleShape)
//                .clickable(
//                    interactionSource = interactionSource,
//                    indication = null
//                ) { onClick() }
//                .pointerInput(Unit) {
//                    detectTapGestures(
//                        onPress = {
//                            val press = PressInteraction.Press(it)
//                            interactionSource.tryEmit(press)
//                            tryAwaitRelease()
//                            interactionSource.tryEmit(PressInteraction.Release(press))
//                        }
//                    )
//                },
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = contentDescription,
//                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
//                modifier = Modifier.size(size)
//            )
//        }
//    }
//}
