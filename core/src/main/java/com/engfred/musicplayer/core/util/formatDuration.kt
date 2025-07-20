package com.engfred.musicplayer.core.util

import java.util.Locale

/**
 * Utility function to format duration in milliseconds to MM:SS format.
 */
fun formatDuration(milliseconds: Long): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}