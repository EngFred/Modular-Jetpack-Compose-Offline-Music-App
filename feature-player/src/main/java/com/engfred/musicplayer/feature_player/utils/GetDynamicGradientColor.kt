package com.engfred.musicplayer.feature_player.utils

import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDynamicGradientColors(context: android.content.Context, uri: String?): List<Color> {
    if (uri == null) {
        return listOf(Color(0xFF1E1E1E), Color(0xFF333333)) // Dark default
    }

    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(uri)
        .allowHardware(false)
        .build()

    return withContext(Dispatchers.IO) {
        try {
            val bitmapDrawable = loader.execute(request).drawable as? BitmapDrawable
            val bitmap = bitmapDrawable?.bitmap

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()

                // Prioritize vibrant and dark colors, but fall back gracefully
                val vibrantColor = palette.vibrantSwatch?.rgb?.let { Color(it) }
                val darkVibrantColor = palette.darkVibrantSwatch?.rgb?.let { Color(it) }
                val mutedColor = palette.mutedSwatch?.rgb?.let { Color(it) }
                val darkMutedColor = palette.darkMutedSwatch?.rgb?.let { Color(it) }
                val dominantColor = palette.dominantSwatch?.rgb?.let { Color(it) }

                val colors = mutableListOf<Color>()

                // Strategy: Prefer a dark vibrant/muted, then a slightly lighter, and finally dominant
                val primaryGradientColor = darkVibrantColor ?: darkMutedColor ?: vibrantColor ?: dominantColor ?: Color.Black
                val secondaryGradientColor = mutedColor ?: vibrantColor ?: dominantColor ?: darkMutedColor ?: Color.DarkGray

                colors.add(primaryGradientColor.copy(alpha = 0.9f))
                colors.add(secondaryGradientColor.copy(alpha = 0.8f))

                // Ensure there are at least two distinct colors for the gradient
                if (colors.size < 2) {
                    // Fallback to a predefined darker gradient if palette extraction is insufficient
                    return@withContext listOf(Color(0xFF1A1A1A), Color(0xFF2B2B2B))
                }

                // Sort by luminance to ensure a smooth transition from dark to slightly lighter
                colors.sortBy { it.luminance() }

                // Cap at two colors for a clean vertical gradient
                colors.distinct().take(2)
            } else {
                listOf(Color(0xFF1E1E1E), Color(0xFF333333))
            }
        } catch (e: Exception) {
            println("Error generating palette from album art: ${e.message}")
            listOf(Color(0xFF1E1E1E), Color(0xFF333333)) // Fallback on error
        }
    }
}