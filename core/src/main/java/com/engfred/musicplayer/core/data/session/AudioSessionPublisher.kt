package com.engfred.musicplayer.core.data.session
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioSessionIdPub"

/**
 * A singleton publisher for the ExoPlayer audio session ID.
 * This allows components within the same process (like MusicService and EqualizerController)
 * to observe the audio session ID without relying on MediaSession/MediaController IPC
 * which has proven problematic for this specific piece of data.
 */
@UnstableApi
@Singleton
class AudioSessionIdPublisher @Inject constructor() {

    private val _currentAudioSessionId = MutableStateFlow(C.AUDIO_SESSION_ID_UNSET)
    val currentAudioSessionId: StateFlow<Int> = _currentAudioSessionId.asStateFlow()

    fun updateAudioSessionId(sessionId: Int) {
        if (_currentAudioSessionId.value != sessionId) {
            _currentAudioSessionId.value = sessionId
            Log.d(TAG, "AudioSessionIdPublisher updated ID to: $sessionId")
        }
    }
}