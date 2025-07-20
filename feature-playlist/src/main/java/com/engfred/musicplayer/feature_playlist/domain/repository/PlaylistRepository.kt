package com.engfred.musicplayer.feature_playlist.domain.repository

import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for data operations related to playlists.
 * This interface is part of the domain layer, making it independent of data source implementation.
 */
interface PlaylistRepository {
    /**
     * Gets a flow of all playlists.
     */
    fun getPlaylists(): Flow<List<Playlist>>

    /**
     * Gets a specific playlist by its ID.
     */
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>

    /**
     * Creates a new playlist.
     * @return The ID of the newly created playlist.
     */
    suspend fun createPlaylist(playlist: Playlist): Long

    /**
     * Updates an existing playlist (e.g., changing its name).
     */
    suspend fun updatePlaylist(playlist: Playlist)

    /**
     * Deletes a playlist by its ID.
     */
    suspend fun deletePlaylist(playlistId: Long)

    /**
     * Adds a song to an existing playlist.
     */
    suspend fun addSongToPlaylist(playlistId: Long, audioFile: AudioFile)

    /**
     * Removes a song from a playlist.
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, audioFileId: Long)
}