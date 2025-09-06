package com.engfred.musicplayer.feature_player.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDominantColorForAlbumArt(context: Context, albumArtUri: String?): Color {
    // Default color if no album art is available
    val defaultColor = Color.Gray

    if (albumArtUri == null) return defaultColor

    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(albumArtUri)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request).drawable
            val bitmap = result?.toBitmap()
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val dominantColor = palette.dominantSwatch?.rgb
                if (dominantColor != null) {
                    Color(dominantColor)
                } else {
                    defaultColor
                }
            } else {
                defaultColor
            }
        } catch (e: Exception) {
            defaultColor
        }
    }
}

@Composable
fun getContentColorForAlbumArt(context: Context, albumArtUri: String?): Color {
    val defaultContentColor = Color.White // Fallback content color
    val albumArtColor by produceState(initialValue = Color.Gray, albumArtUri) {
        value = getDominantColorForAlbumArt(context, albumArtUri)
    }
    return if (albumArtColor.luminance() > 0.5f) Color.Black else Color.White
}
