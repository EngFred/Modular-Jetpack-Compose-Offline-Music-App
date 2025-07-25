package com.engfred.musicplayer.core.domain.repository

import android.net.Uri
import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow

interface PlayerController {
    fun getPlaybackState(): Flow<PlaybackState>
    suspend fun initiatePlayback(initialAudioFileUri: Uri)
    suspend fun playPause()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)
    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun setShuffleMode(mode: ShuffleMode)
    suspend fun releasePlayer()
    suspend fun addAudioToQueueNext(audioFile: AudioFile)
    fun clearPlaybackError()
}