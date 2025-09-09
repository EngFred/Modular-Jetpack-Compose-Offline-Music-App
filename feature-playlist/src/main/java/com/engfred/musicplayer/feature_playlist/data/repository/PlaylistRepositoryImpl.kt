package com.engfred.musicplayer.feature_playlist.data.repository

import android.util.Log
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_playlist.data.local.dao.PlaylistDao
import com.engfred.musicplayer.feature_playlist.data.local.entity.PlaylistEntity
import com.engfred.musicplayer.feature_playlist.data.local.entity.PlaylistSongEntity
import com.engfred.musicplayer.feature_playlist.data.local.model.PlaylistWithSongs
import com.engfred.musicplayer.core.domain.model.Playlist
import com.engfred.musicplayer.core.domain.model.AutomaticPlaylistType
import com.engfred.musicplayer.core.domain.repository.PlaylistRepository
import com.engfred.musicplayer.feature_playlist.data.local.entity.SongPlayEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of PlaylistRepository that interacts with the local Room database.
 * It maps between Room entities/models and domain models.
 */
@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val sharedAudioDataSource: SharedAudioDataSource
) : PlaylistRepository {

    private val TAG = "PlaylistRepositoryImpl"

    // --- Mapper functions to convert between domain models and Room entities/models ---

    private fun PlaylistWithSongs.toDomain(): Playlist {
        return Playlist(
            id = this.playlist.playlistId,
            name = this.playlist.name,
            createdAt = this.playlist.createdAt,
            songs = this.songs.map { it.toDomain() },
            isAutomatic = false,
            type = null
        )
    }

    private fun PlaylistEntity.toDomain(): Playlist {
        return Playlist(
            id = this.playlistId,
            name = this.name,
            createdAt = this.createdAt,
            isAutomatic = false,
            type = null
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
            playlistId = this.id,
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
        // Combine user-created playlists with automatic playlists
        return combine(
            playlistDao.getPlaylistsWithSongs().map { playlistWithSongsList ->
                playlistWithSongsList.map { it.toDomain() }
            },
            getRecentlyAddedSongs(limit = 20), // Limit to 20 recently added songs
            getTopPlayedSongs(sinceTimestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L), limit = 20) // Top 20 songs from last 7 days (requires >= 3 plays)
        ) { userPlaylists, recentlyAddedSongs, topPlayedPairs ->
            val automaticPlaylists = mutableListOf<Playlist>()

            // Add Recently Added playlist if there are songs
            if (recentlyAddedSongs.isNotEmpty()) {
                automaticPlaylists.add(
                    Playlist(
                        id = -1, // Use a negative ID to distinguish from Room IDs
                        name = "Recently Added",
                        songs = recentlyAddedSongs,
                        isAutomatic = true,
                        type = AutomaticPlaylistType.RECENTLY_ADDED
                    )
                )
            }

            // Always add Top Songs playlist (even if empty)
            automaticPlaylists.add(
                Playlist(
                    id = -2, // Use another negative ID
                    name = "Weekly Most Played",
                    songs = topPlayedPairs.map { it.first },
                    isAutomatic = true,
                    type = AutomaticPlaylistType.MOST_PLAYED,
                    playCounts = topPlayedPairs.associate { it.first.id to it.second }
                )
            )

            // Combine automatic playlists (at the top) with user-created playlists
            automaticPlaylists + userPlaylists
        }
    }

    override fun getPlaylistById(playlistId: Long): Flow<Playlist?> {
        // Handle automatic playlist IDs
        return when (playlistId) {
            -1L -> getRecentlyAddedSongs(limit = 20).map { songs ->
                if (songs.isNotEmpty()) Playlist(
                    id = -1,
                    name = "Recently Added",
                    songs = songs,
                    isAutomatic = true,
                    type = AutomaticPlaylistType.RECENTLY_ADDED
                ) else null
            }
            -2L -> getTopPlayedSongs(sinceTimestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L), limit = 50).map { pairs ->
                // The flow returned by getTopPlayedSongs already filters out playCounts < 3
                Playlist(
                    id = -2,
                    name = "Weekly Most Played",
                    songs = pairs.map { it.first },
                    isAutomatic = true,
                    type = AutomaticPlaylistType.MOST_PLAYED,
                    playCounts = pairs.associate { it.first.id to it.second }
                )
            }
            else -> playlistDao.getPlaylistWithSongsById(playlistId).map { playlistWithSongs ->
                playlistWithSongs?.toDomain()
            }
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

    // NEW: Removes the song from all playlists by querying playlist IDs containing the song and removing from each.
    override suspend fun removeSongFromAllPlaylists(audioFileId: Long) {
        val playlistIds = playlistDao.getPlaylistIdsContainingSong(audioFileId)
        playlistIds.forEach { playlistId ->
            removeSongFromPlaylist(playlistId, audioFileId)
        }
        Log.d(TAG, "Removed song ID: $audioFileId from all playlists (${playlistIds.size} playlists affected).")
    }

    /**
     * Retrieves a flow of recently added songs from the shared audio data source,
     * sorted by date added (descending).
     * @param limit The maximum number of songs to retrieve.
     */
    override fun getRecentlyAddedSongs(limit: Int): Flow<List<AudioFile>> {
        return sharedAudioDataSource.deviceAudioFiles.map { allAudioFiles ->
            allAudioFiles
                .sortedByDescending { it.dateAdded }
                .take(limit)
        }
    }

    /**
     * Retrieves a flow of top played songs by querying play events in the database.
     * It then maps these top played audio file IDs to actual AudioFile objects
     * from the shared audio data source.
     *
     * NOTE: This function enforces that only songs with at least 3 plays are included.
     *
     * @param sinceTimestamp The timestamp (milliseconds) from which to count play events.
     * @param limit The maximum number of songs to retrieve.
     */
    override fun getTopPlayedSongs(sinceTimestamp: Long, limit: Int): Flow<List<Pair<AudioFile, Int>>> {
        return combine(
            playlistDao.getTopPlayedAudioFileIds(sinceTimestamp, limit),
            sharedAudioDataSource.deviceAudioFiles
        ) { topPlayedIds, allAudioFiles ->
            val audioFileMap = allAudioFiles.associateBy { it.id } // Create a map for efficient lookup

            // Filter to require at least 3 plays, preserve order returned by DAO (assumed descending by play count),
            // and then limit the number of results.
            val filtered = topPlayedIds
                .asSequence()
                .filter { it.playCount >= 3 } // enforce minimum plays
                .take(limit)
                .toList()

            filtered.mapNotNull { topId ->
                audioFileMap[topId.audioFileId]?.let { audioFile ->
                    audioFile to topId.playCount
                }
            }
        }
    }

    /**
     * Records a play event for a given audio file by inserting it into the database.
     * @param audioFileId The ID of the audio file that was played.
     */
    override suspend fun recordSongPlayEvent(audioFileId: Long) {
        try {
            val playEvent = SongPlayEventEntity(audioFileId = audioFileId, timestamp = System.currentTimeMillis())
            playlistDao.insertSongPlayEvent(playEvent)
            Log.d(TAG, "Recorded play event for audioFileId: $audioFileId")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording song play event for audioFileId: $audioFileId", e)
        }
    }
}