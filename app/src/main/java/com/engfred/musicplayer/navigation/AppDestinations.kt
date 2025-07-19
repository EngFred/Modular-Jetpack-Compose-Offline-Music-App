package com.engfred.musicplayer.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class defining all the navigation destinations in the application.
 * This centralizes route definitions and allows for type-safe navigation.
 */
sealed class AppDestinations(val route: String) {

    data object MainGraph : AppDestinations("main_graph") // New: Represents the graph with bottom nav

    data object Player : AppDestinations("player/{audioFileUri}") {
        // Function to create the route with arguments
        fun createRoute(audioFileUri: String): String {
            return "player/${Uri.encode(audioFileUri)}" // Ensure URI is encoded
        }
    }
    data object Playlists : AppDestinations("playlists")
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
        data object Library : BottomNavItem("library", Icons.Default.Home, "Library")
        data object Playlists : BottomNavItem("playlists", Icons.Default.List, "Playlists")
        data object Favorites : BottomNavItem("favorites", Icons.Default.Favorite, "Favorites")
        data object Search : BottomNavItem("search", Icons.Default.Search, "Search")
    }

    // Settings screen
    data object Settings : AppDestinations("settings")
}