package com.engfred.musicplayer.feature_playlist.data.repository

import com.engfred.musicplayer.feature_library.domain.model.AudioFile
import com.engfred.musicplayer.feature_playlist.data.local.dao.PlaylistDao
import com.engfred.musicplayer.feature_playlist.data.local.entity.PlaylistEntity
import com.engfred.musicplayer.feature_playlist.data.local.entity.PlaylistSongEntity
import com.engfred.musicplayer.feature_playlist.data.local.model.PlaylistWithSongs
import com.engfred.musicplayer.feature_playlist.domain.model.Playlist
import com.engfred.musicplayer.feature_playlist.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of PlaylistRepository that interacts with the local Room database.
 * It maps between Room entities/models and domain models.
 */
@Singleton // This repository will be a singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    // --- Mapper functions to convert between domain models and Room entities/models ---

    private fun PlaylistWithSongs.toDomain(): Playlist {
        return Playlist(
            playlistId = this.playlist.playlistId,
            name = this.playlist.name,
            createdAt = this.playlist.createdAt,
            songs = this.songs.map { it.toDomain() }
        )
    }

    private fun PlaylistEntity.toDomain(): Playlist {
        return Playlist(
            playlistId = this.playlistId,
            name = this.name,
            createdAt = this.createdAt
        )
    }

    private fun PlaylistSongEntity.toDomain(): AudioFile {
        return AudioFile(
            id = this.audioFileId,
            title = this.title,
            artist = this.artist,
            album = this.album,
            duration = this.duration,
            uri = this.uri,
            albumArtUri = this.albumArtUri,
            dateAdded = this.dateAdded
        )
    }

    private fun Playlist.toEntity(): PlaylistEntity {
        return PlaylistEntity(
            playlistId = this.playlistId,
            name = this.name,
            createdAt = this.createdAt
        )
    }

    private fun AudioFile.toPlaylistSongEntity(playlistId: Long): PlaylistSongEntity {
        return PlaylistSongEntity(
            playlistId = playlistId,
            audioFileId = this.id,
            title = this.title,
            artist = this.artist,
            album = this.album,
            duration = this.duration,
            uri = this.uri,
            albumArtUri = this.albumArtUri,
            dateAdded = this.dateAdded
        )
    }

    // --- Repository interface implementations ---

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsWithSongs().map { playlistWithSongsList ->
            playlistWithSongsList.map { it.toDomain() }
        }
    }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistWithSongsById(playlistId).map { playlistWithSongs ->
            playlistWithSongs?.toDomain()
        }
    }

    override suspend fun createPlaylist(playlist: Playlist): Long {
        return playlistDao.insertPlaylist(playlist.toEntity())
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.toEntity())
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, audioFile: AudioFile) {
        val playlistSongEntity = audioFile.toPlaylistSongEntity(playlistId)
        playlistDao.insertPlaylistSong(playlistSongEntity)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, audioFileId: Long) {
        playlistDao.deletePlaylistSong(playlistId, audioFileId)
    }
}