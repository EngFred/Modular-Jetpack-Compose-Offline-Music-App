package com.engfred.musicplayer.feature_player.presentation.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun RotatingAlbumArt(
    albumArtUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotationSpeedMillis = 8000L
    var targetRotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = rotationSpeedMillis.toInt(), easing = LinearEasing),
        label = "albumArtRotation"
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val currentAnimatedValue = animatedRotation
            val rotationsCompleted = (currentAnimatedValue / 360f).roundToInt()
            targetRotationAngle = (rotationsCompleted * 360f) + 360f
            while (true) {
                targetRotationAngle += 360f
                delay(rotationSpeedMillis)
            }
        } else {
            targetRotationAngle = animatedRotation
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .rotate(animatedRotation),
        contentAlignment = Alignment.Center
    ) {
        CoilImage(
            imageModel = { albumArtUri },
            imageOptions = ImageOptions(
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop
            ),
            modifier = Modifier.fillMaxSize(),
            failure = {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "No Album Art Available",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize(0.6f)
                )
            },
            loading = {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize(0.4f)
                )
            }
        )
    }
}