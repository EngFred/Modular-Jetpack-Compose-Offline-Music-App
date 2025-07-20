package com.engfred.musicplayer.feature_player.domain.repository

import android.net.Uri
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {

    suspend fun setMediaItems(audioFileUris: List<Uri>, startIndex: Int)
    suspend fun addMediaItems(audioFileUris: List<Uri>)
    suspend fun play()
    suspend fun pause()
    suspend fun playPause()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)
    suspend fun seekToItem(index: Int)
    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun setShuffleMode(mode: ShuffleMode)
    suspend fun releasePlayer()

    fun getPlaybackState(): Flow<PlaybackState>

    suspend fun initiatePlayback(initialAudioFileUri: Uri)
}