package com.engfred.musicplayer.core.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Rotation helper that:
 *  - preserves rotation across recompositions (Animatable)
 *  - continues from the preserved angle on resume
 *  - optionally resets to 0 when [trackId] changes
 *  - optionally eases out (small spin) when paused instead of stopping abruptly
 *
 * @param trackId optional ID of the current track; when it changes the rotation snaps to 0.
 * @param easeOutOnPause if true, when isRotating flips to false we perform a short eased
 *        spin-out so it slows to a stop instead of halting immediately.
 * @param spinOutDegrees how much additional angle to animate on pause when easing out.
 * @param easeOutDurationMillis duration for the pause ease-out.
 */
@Composable
fun rememberAlbumRotationDegrees(
    isRotating: Boolean,
    durationMillis: Int = 4000,
    trackId: Any? = null,
    easeOutOnPause: Boolean = false,
    spinOutDegrees: Float = 45f,
    easeOutDurationMillis: Int = 350
): Float {
    val anim = remember { Animatable(0f) }

    // Reset immediately when the trackId changes (snap to 0)
    LaunchedEffect(trackId) {
        anim.snapTo(0f)
    }

    // Control spinning / pause behavior
    LaunchedEffect(isRotating) {
        if (isRotating) {
            // Continue spinning forever in +360 chunks; value is preserved between runs.
            try {
                while (isActive && isRotating) {
                    val start = anim.value
                    val target = start + 360f
                    anim.animateTo(
                        targetValue = target,
                        animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
                    )
                    // loop continues
                }
            } catch (_: CancellationException) {
                // cancelled when isRotating flips or composition changes — keep current value
            }
        } else {
            // Not rotating: optionally perform a short ease-out spin and then stop.
            if (easeOutOnPause) {
                try {
                    val target = anim.value + spinOutDegrees
                    anim.animateTo(
                        targetValue = target,
                        animationSpec = tween(durationMillis = easeOutDurationMillis, easing = FastOutSlowInEasing)
                    )
                } catch (_: CancellationException) {
                    // cancelled — state preserved
                }
            }
            // else: do nothing, preserve anim.value as-is
        }
    }

    // Keep returned value normalized to 0..360
    return (anim.value % 360f + 360f) % 360f
}
