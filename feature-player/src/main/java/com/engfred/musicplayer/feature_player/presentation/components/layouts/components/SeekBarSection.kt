package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState // Import SliderState for earlier M3 versions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember // Import remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.feature_player.domain.model.PlayerLayout // Import PlayerLayout
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class) // This annotation is crucial for custom thumb/track
@Composable
fun SeekBarSection(
    sliderValue: Float,
    totalDurationMs: Long,
    playbackPositionMs: Long,
    isSeeking: Boolean, // Keeping this as a parameter for consistency with your external state
    onSliderValueChange: (Float) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    playerLayout: PlayerLayout, // PlayerLayout enum
    modifier: Modifier = Modifier // Add modifier parameter
) {
    // Custom thumb now accepts SliderState as per your error message
    val customThumb: @Composable (SliderState) -> Unit = { sliderState ->
        val thumbScale by animateFloatAsState(
            // We use the `isSeeking` from the composable's parameters
            // If you want the thumb animation to react directly to user drag,
            // you could use `sliderState.isThumbBeingDragged` here instead.
            targetValue = if (isSeeking) 1.2f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "sliderThumbScale"
        )
        Box(
            modifier = Modifier
                .size(16.dp) // Small, professional dot size
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(if (isSeeking) 8.dp else 4.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }

    // Custom track now accepts SliderState as per your error message
    val customTrack: @Composable (SliderState) -> Unit = { sliderState ->
        val activeTrackColor = MaterialTheme.colorScheme.primary
        val inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.1f) // Adjusted alpha for subtle look

        // Calculate progress directly from SliderState's value and valueRange
        val totalRange = sliderState.valueRange.endInclusive - sliderState.valueRange.start
        val currentProgressFraction = if (totalRange > 0) {
            (sliderState.value - sliderState.valueRange.start) / totalRange
        } else {
            0f
        }

        // Layering two boxes to create the track effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp) // Keep the track thin
                .clip(RoundedCornerShape(2.dp))
                .background(inactiveTrackColor) // Inactive part fills the whole width initially
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(currentProgressFraction) // Active part covers a fraction of the width
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(activeTrackColor)
        )
    }

    // Standard interaction source for the slider
    val interactionSource = remember { MutableInteractionSource() }

    // Total range for the main Slider valueRange parameter
    val sliderValueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(0f)

    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
        // ImmersiveCanvas style: 00:12 --------- 03:20 (inline times)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMillis(playbackPositionMs),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 8.dp)
            )

            Slider(
                value = sliderValue,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueChangeFinished,
                valueRange = sliderValueRange, // Use the pre-calculated totalRange
                colors = SliderDefaults.colors( // Colors still apply for consistency, though track uses custom
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.2f)
                ),
                thumb = customThumb, // Apply the custom circular thumb
                track = customTrack, // Apply the custom track
                enabled = true, // Default to enabled
                interactionSource = interactionSource, // Provide interaction source
                steps = 0, // Continuous slider (no discrete steps)
                modifier = Modifier.weight(1f) // Slider takes up available space
            )

            Text(
                text = formatMillis(totalDurationMs),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    } else {
        // EtherealFlowLayout style: Times below slider
        Column(modifier = modifier.fillMaxWidth()) {
            Slider(
                value = sliderValue,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueChangeFinished,
                valueRange = sliderValueRange, // Use the pre-calculated totalRange
                colors = SliderDefaults.colors( // Colors still apply for consistency, though track uses custom
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.2f)
                ),
                thumb = customThumb, // Apply the custom circular thumb
                track = customTrack, // Apply the custom track
                enabled = true, // Default to enabled
                interactionSource = interactionSource, // Provide interaction source
                steps = 0, // Continuous slider
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMillis(playbackPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
                Text(
                    text = formatMillis(totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatMillis(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}