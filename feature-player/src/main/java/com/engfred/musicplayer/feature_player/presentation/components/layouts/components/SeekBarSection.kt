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
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.util.formatDuration
import com.engfred.musicplayer.core.domain.model.PlayerLayout

/**
 * Composable for displaying the seek bar and playback timings.
 * It provides custom thumb and track visuals and adapts its layout based on the [PlayerLayout].
 *
 * @param sliderValue The current value of the slider.
 * @param totalDurationMs The total duration of the current song in milliseconds.
 * @param playbackPositionMs The current playback position in milliseconds.
 * @param isSeeking Boolean indicating if the user is currently dragging the slider.
 * @param onSliderValueChange Callback invoked when the slider's value changes.
 * @param onSliderValueChangeFinished Callback invoked when the user stops dragging the slider.
 * @param playerLayout The currently active player layout, used for styling adjustments.
 * @param modifier The modifier to be applied to the seek bar section.
 * @param horizontalPadding Optional horizontal padding for the content within the seek bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBarSection(
    sliderValue: Float,
    totalDurationMs: Long,
    playbackPositionMs: Long,
    isSeeking: Boolean,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    playerLayout: PlayerLayout,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp // Added for responsiveness
) {
    // Custom thumb: A small, animated circular indicator
    val customThumb: @Composable (SliderState) -> Unit = { sliderState ->
        val thumbScale by animateFloatAsState(
            // React to the `isSeeking` parameter from the parent, or sliderState.isThumbBeingDragged
            targetValue = if (isSeeking) 1.2f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "sliderThumbScale"
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(if (isSeeking) 8.dp else 4.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }

    // Custom track: A layered track for active and inactive portions
    val customTrack: @Composable (SliderState) -> Unit = { sliderState ->
        val activeTrackColor = MaterialTheme.colorScheme.primary
        val inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.15f) // Adjusted alpha for better contrast

        // Calculate progress directly from SliderState's value and valueRange
        val totalRange = sliderState.valueRange.endInclusive - sliderState.valueRange.start
        val currentProgressFraction = if (totalRange > 0) {
            (sliderState.value - sliderState.valueRange.start) / totalRange
        } else {
            0f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(inactiveTrackColor) // Inactive part fills the whole width initially
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentProgressFraction) // Active part covers a fraction of the width
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeTrackColor)
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val sliderValueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(0f)

    if (playerLayout == PlayerLayout.IMMERSIVE_CANVAS) {
        // ImmersiveCanvas style: 00:12 --------- 03:20 (inline times)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding), // Use the passed horizontalPadding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(playbackPositionMs),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 8.dp)
            )

            Slider(
                value = sliderValue,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueChangeFinished,
                valueRange = sliderValueRange,
                colors = SliderDefaults.colors( // Colors still apply for consistency, though track uses custom
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.2f)
                ),
                thumb = customThumb,
                track = customTrack,
                enabled = true,
                interactionSource = interactionSource,
                steps = 0,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatDuration(totalDurationMs),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    } else { // EtherealFlowLayout or MinimalistGrooveLayout styles: Times below slider
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding) // Use the passed horizontalPadding
        ) {
            Slider(
                value = sliderValue,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueChangeFinished,
                valueRange = sliderValueRange,
                colors = SliderDefaults.colors( // Colors still apply for consistency, though track uses custom
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.2f)
                ),
                thumb = customThumb,
                track = customTrack,
                enabled = true,
                interactionSource = interactionSource,
                steps = 0,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playbackPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
                Text(
                    text = formatDuration(totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        }
    }
}