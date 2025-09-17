package com.engfred.musicplayer.feature_player.data.service

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple factory to create ExoPlayer instances on demand.
 * Avoids providing ExoPlayer as an app-scoped singleton which can hold a dead Handler after service restart.
 */
@Singleton
class ExoPlayerProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(): ExoPlayer {
        return ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }
    }
}
