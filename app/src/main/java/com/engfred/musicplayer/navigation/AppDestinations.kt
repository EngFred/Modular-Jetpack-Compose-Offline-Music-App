package com.engfred.musicplayer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestinations(val route: String) {
    data object Splash : AppDestinations("splash")
    data object MainGraph : AppDestinations("main_graph")
    data object NowPlaying : AppDestinations("now_playing")
    data object PlaylistDetail : AppDestinations("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }

    data object EditAudioInfo : AppDestinations("edit_song/{audioId}") {
        fun createRoute(audioId: Long) = "edit_song/$audioId"
    }

    data object CreatePlaylist : AppDestinations("create_playlist")

    sealed class BottomNavItem(val baseRoute: String, val icon: ImageVector, val label: String) {
        data object Library : BottomNavItem("library", Icons.Rounded.LibraryMusic, "Library")
        data object Playlists : BottomNavItem("playlists", Icons.AutoMirrored.Rounded.List, "Playlists")
        data object Favorites : BottomNavItem("favorites", Icons.Rounded.Favorite, "Favorites")
    }

    data object Settings : AppDestinations("settings")
}