package com.engfred.musicplayer.feature_player.presentation.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.engfred.musicplayer.core.util.formatDuration
import com.engfred.musicplayer.core.domain.model.PlayerLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBarSection(
    sliderValue: Float,
    totalDurationMs: Long,
    playbackPositionMs: Long,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    playerLayout: PlayerLayout,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp
) {
    // Custom thumb: A static circular indicator
    val customThumb: @Composable (SliderState) -> Unit = {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }

    // Custom track: A layered track for active and inactive portions
    val customTrack: @Composable (SliderState) -> Unit = { sliderState ->
        val activeTrackColor = MaterialTheme.colorScheme.primary
        val inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.15f)

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
                .background(inactiveTrackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentProgressFraction)
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
                .padding(horizontal = horizontalPadding),
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
                colors = SliderDefaults.colors(
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
    } else {
        // EtherealFlowLayout or MinimalistGrooveLayout: times below slider, reduced gap
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            // Wrap Slider in Box with constrained height to reduce gap
            Box(
                modifier = Modifier.height(18.dp)
            ) {
                Slider(
                    value = sliderValue,
                    onValueChange = onSliderValueChange,
                    onValueChangeFinished = onSliderValueChangeFinished,
                    valueRange = sliderValueRange,
                    colors = SliderDefaults.colors(
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
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(playbackPositionMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
                Text(
                    text = formatDuration(totalDurationMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        }
    }
}
