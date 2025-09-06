package com.engfred.musicplayer.feature_player.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log // Import Log for proper logging
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDynamicGradientColors(context: Context, uri: String?): List<Color> {
    // Default dark gradient colors
    val defaultColors = listOf(Color(0xFF1E1E1E), Color(0xFF333333))

    if (uri == null) {
        return defaultColors
    }

    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(uri)
        .allowHardware(false) // Required for BitmapDrawable on some older Android versions
        .build()

    return withContext(Dispatchers.IO) {
        try {
            val bitmapDrawable = loader.execute(request).drawable as? BitmapDrawable
            val bitmap = bitmapDrawable?.bitmap

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()

                // Prioritize swatches for a dark, rich gradient
                val vibrantSwatch = palette.vibrantSwatch
                val darkVibrantSwatch = palette.darkVibrantSwatch
                val mutedSwatch = palette.mutedSwatch
                val darkMutedSwatch = palette.darkMutedSwatch
                val dominantSwatch = palette.dominantSwatch

                // Strategy for primary (top) color: prefer dark vibrant, then dark muted, then vibrant, then muted, then dominant
                val primaryColor = (darkVibrantSwatch ?: darkMutedSwatch ?: vibrantSwatch ?: mutedSwatch ?: dominantSwatch)?.rgb?.let { Color(it) }

                // Strategy for secondary (bottom) color: complement primary, ensure variety
                // Try a slightly less dark version or a more muted tone
                val secondaryColor = when {
                    primaryColor == null -> null // No primary, no secondary
                    darkMutedSwatch != null && darkMutedSwatch.rgb != primaryColor.toArgb() -> Color(darkMutedSwatch.rgb)
                    vibrantSwatch != null && vibrantSwatch.rgb != primaryColor.toArgb() -> Color(vibrantSwatch.rgb)
                    mutedSwatch != null && mutedSwatch.rgb != primaryColor.toArgb() -> Color(mutedSwatch.rgb)
                    else -> primaryColor.lighten(0.1f) // Lighten primary slightly if no distinct secondary found
                }?.let { Color(it.toArgb()) } // Ensure alpha is 1.0 initially for calculation

                // If a primary color is found, but secondary isn't distinct enough, adjust
                val finalPrimary = primaryColor?.copy(alpha = 0.9f) ?: defaultColors[0]
                val finalSecondary = if (secondaryColor != null && secondaryColor != finalPrimary) {
                    secondaryColor.copy(alpha = 0.8f) // Slightly less alpha for the secondary color
                } else {
                    finalPrimary.darken(0.1f).copy(alpha = 0.8f) // Fallback: Darken the primary color
                }

                // Ensure the list always has two colors
                val colors = mutableListOf(finalPrimary, finalSecondary).distinct().take(2)

                // Sort by luminance to ensure a smooth transition from dark to slightly lighter
                // If only one distinct color, duplicate it for the gradient
                if (colors.size < 2) {
                    listOf(colors.first(), colors.first().darken(0.1f)) // Ensure two colors for the gradient
                } else {
                    colors.sortedBy { it.luminance() }
                }

            } else {
                Log.w("EtherealFlowLayout", "Bitmap could not be extracted from URI: $uri")
                defaultColors
            }
        } catch (e: Exception) {
            Log.e("EtherealFlowLayout", "Error generating palette from album art for URI: $uri", e)
            defaultColors // Fallback on error
        }
    }
}

// Extension functions for color manipulation
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}