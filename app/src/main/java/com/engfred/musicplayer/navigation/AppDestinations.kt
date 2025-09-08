package com.engfred.musicplayer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class defining all the navigation destinations in the application.
 * This centralizes route definitions and allows for type-safe navigation.
 */
sealed class AppDestinations(val route: String) {

    data object Splash : AppDestinations("splash")

    data object MainGraph : AppDestinations("main_graph")

    data object NowPlaying : AppDestinations("now_playing")

    data object PlaylistDetail : AppDestinations("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: Long): String {
            return "playlist_detail/$playlistId"
        }
    }

    // Bottom Navigation Bar Destinations (nested within MainGraph)
    sealed class BottomNavItem(
        val baseRoute: String, // Base route for the tab's graph
        val icon: ImageVector,
        val label: String
    ) {
        data object Library : BottomNavItem("library", Icons.Rounded.LibraryMusic, "Library")
        data object Playlists : BottomNavItem("playlists", Icons.AutoMirrored.Rounded.List, "Playlists")
        data object Favorites : BottomNavItem("favorites", Icons.Rounded.Favorite, "Favorites")
    }

    // Settings screen
    data object Settings : AppDestinations("settings")
}