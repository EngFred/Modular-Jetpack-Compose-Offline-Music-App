package com.engfred.musicplayer.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerArgs

/**
 * Sealed class defining all the navigation destinations in the application.
 * This centralizes route definitions and allows for type-safe navigation.
 */
sealed class AppDestinations(val route: String) {

    data object MainGraph : AppDestinations("main_graph") // Represents the graph with bottom nav

    data object Player {
        const val route = "player?${PlayerArgs.AUDIO_FILE_URI}={${PlayerArgs.AUDIO_FILE_URI}}&${PlayerArgs.FROM_MINI_PLAYER}={${PlayerArgs.FROM_MINI_PLAYER}}"
        fun createRoute(audioFileUri: String?, fromMiniPlayer: Boolean): String {
            val encodedUri = audioFileUri?.let { Uri.encode(it) } ?: ""
            return "player?${PlayerArgs.AUDIO_FILE_URI}=$encodedUri&${PlayerArgs.FROM_MINI_PLAYER}=$fromMiniPlayer"
        }
    }

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
        data object Equalizer : BottomNavItem("equalizer", Icons.Default.Equalizer, "Equalizer")
    }

    // Settings screen
    data object Settings : AppDestinations("settings")
}