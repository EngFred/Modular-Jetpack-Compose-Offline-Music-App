package com.engfred.musicplayer.feature_player.presentation.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun FavoriteAnimation(
    trigger: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (!trigger) return

    LaunchedEffect(trigger) {
        delay(800)
        onAnimationEnd()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        repeat(5) {
            val animScale = remember { Animatable(0.5f) }
            val animAlpha = remember { Animatable(1f) }
            val animY = remember { Animatable(0f) }
            val animX = remember { Animatable(0f) }
            val animRotation = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                val randomFactor = Random.nextFloat() * 0.5f + 0.5f // Between 0.5 and 1.0
                animX.animateTo(
                    targetValue = (Random.nextFloat() - 0.5f) * 200f,
                    animationSpec = tween(800, easing = EaseOut)
                )
                animY.animateTo(
                    targetValue = -150f * randomFactor,
                    animationSpec = tween(800, easing = EaseOut)
                )
                animScale.animateTo(
                    targetValue = 1.5f * randomFactor,
                    animationSpec = tween(800, easing = EaseOut)
                )
                animAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(800, delayMillis = 200, easing = EaseOut)
                )
                animRotation.animateTo(
                    targetValue = (Random.nextFloat() - 0.5f) * 30f,
                    animationSpec = tween(800, easing = EaseOut)
                )
            }

            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = animX.value.dp, y = animY.value.dp)
                    .scale(animScale.value)
                    .alpha(animAlpha.value)
                    .rotate(animRotation.value)
            )
        }
    }
}