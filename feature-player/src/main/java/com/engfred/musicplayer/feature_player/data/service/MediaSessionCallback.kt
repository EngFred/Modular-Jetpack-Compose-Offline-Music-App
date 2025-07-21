// feature-player/data/service/MediaSessionCallback.kt
package com.engfred.musicplayer.feature_player.data.service

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.engfred.musicplayer.core.constants.COMMAND_REQUEST_AUDIO_SESSION_ID
import com.engfred.musicplayer.core.constants.KEY_AUDIO_SESSION_ID
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MediaSessionCallback(private val exoPlayer: ExoPlayer) : MediaSession.Callback {

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            COMMAND_REQUEST_AUDIO_SESSION_ID -> {
                // IMPORTANT: This will return C.AUDIO_SESSION_ID_UNSET (0) if player isn't active yet.
                // This is correct behavior, MediaControllerProvider must handle 0.
                val currentAudioSessionId = exoPlayer.audioSessionId
                val resultBundle = Bundle().apply {
                    putInt(KEY_AUDIO_SESSION_ID, currentAudioSessionId)
                }
                Log.d("MediaSessionCallback", "Responding to COMMAND_REQUEST_AUDIO_SESSION_ID with: $currentAudioSessionId")
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
            }
            // Add other custom commands here if needed.
            else -> super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}