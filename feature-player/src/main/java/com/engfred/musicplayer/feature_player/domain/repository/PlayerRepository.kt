package com.engfred.musicplayer.feature_player.domain.repository

import android.net.Uri
import androidx.media3.session.MediaController
import com.engfred.musicplayer.feature_player.domain.model.PlaybackState
import com.engfred.musicplayer.feature_player.domain.model.RepeatMode
import com.engfred.musicplayer.feature_player.domain.model.ShuffleMode
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for music playback operations.
 * This interface is part of the domain layer.
 */
interface PlayerRepository {

    /**
     * Provides a MediaController instance to be used for playback operations.
     * Must be called before any playback-related methods.
     */
    fun setMediaController(mediaController: MediaController)

    /**
     * Observes the current playback state of the player.
     * @return A Flow emitting PlaybackState updates.
     */
    fun getPlaybackState(): Flow<PlaybackState>

    /**
     * Sets the list of audio files to be played.
     * @param audioFileUris A list of URIs for the audio files.
     * @param startIndex The index of the song to start playing from.
     */
    suspend fun setMediaItems(audioFileUris: List<Uri>, startIndex: Int = 0)

    /**
     * Adds a list of audio files to the existing playlist.
     * @param audioFileUris A list of URIs to add to the playlist.
     */
    suspend fun addMediaItems(audioFileUris: List<Uri>)

    /**
     * Plays the current media item.
     */
    fun play()

    /**
     * Pauses the current media item.
     */
    fun pause()

    /**
     * Toggles play/pause state.
     */
    fun playPause()

    /**
     * Skips to the next media item in the playlist.
     */
    fun skipToNext()

    /**
     * Skips to the previous media item in the playlist.
     */
    fun skipToPrevious()

    /**
     * Seeks to a specific position in the current media item.
     * @param positionMs The position in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /**
     * Seeks to a specific media item in the playlist.
     * @param index The index of the media item to seek to.
     */
    fun seekToItem(index: Int)

    /**
     * Sets the repeat mode for playback.
     * @param mode The desired RepeatMode.
     */
    fun setRepeatMode(mode: RepeatMode)

    /**
     * Sets the shuffle mode for playback.
     * @param mode The desired ShuffleMode.
     */
    fun setShuffleMode(mode: ShuffleMode)

    /**
     * Releases the player resources. Should be called when the player is no longer needed.
     */
    fun releasePlayer()
}