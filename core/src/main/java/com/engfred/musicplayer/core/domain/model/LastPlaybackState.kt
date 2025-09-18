package com.engfred.musicplayer.core.domain.model

data class LastPlaybackState(
    val audioId: Long?,
    val positionMs: Long = 0L,
    val queueIds: List<Long>? = null
)