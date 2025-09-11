package com.engfred.musicplayer.core.domain.repository

import android.net.Uri
import com.engfred.musicplayer.core.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow

interface PlaybackController {
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
    suspend fun onAudioFileRemoved(deletedAudioFile: AudioFile)
    suspend fun removeFromQueue(audioFile: AudioFile)
    suspend fun initiateShufflePlayback(playingQueue: List<AudioFile>)
    suspend fun waitUntilReady(timeoutMs: Long = 5000): Boolean
}