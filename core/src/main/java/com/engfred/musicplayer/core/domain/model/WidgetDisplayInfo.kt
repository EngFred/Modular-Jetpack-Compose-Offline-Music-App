package com.engfred.musicplayer.core.domain.model

import android.net.Uri

data class WidgetDisplayInfo(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val positionMs: Long,
    val artworkUri: Uri?
)