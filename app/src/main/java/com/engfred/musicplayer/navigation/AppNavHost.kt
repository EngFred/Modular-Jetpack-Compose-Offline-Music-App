package com.engfred.musicplayer.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.engfred.musicplayer.feature_player.presentation.screens.PlayerScreen
import com.engfred.musicplayer.feature_player.presentation.viewmodel.PlayerArgs
import com.engfred.musicplayer.feature_playlist.presentation.screens.PlaylistDetailScreen
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.PlaylistDetailArgs
import com.engfred.musicplayer.feature_settings.presentation.screens.SettingsScreen
import com.engfred.musicplayer.ui.MainScreen

/**
 * Defines the main navigation graph for the application.
 */
@Composable
fun AppNavHost(
    rootNavController: NavHostController,
) {
    NavHost(
        navController = rootNavController,
        startDestination = AppDestinations.MainGraph.route
    ) {
        // Main Graph (with bottom nav)
        composable(AppDestinations.MainGraph.route) {
            MainScreen(
                onPlayAudio = { audioFileUri ->
                    val encodedUri = Uri.encode(audioFileUri)
                    rootNavController.navigate(AppDestinations.Player.createRoute(encodedUri))
                },
                onPlaylistClick = { playlistId ->
                    rootNavController.navigate(AppDestinations.PlaylistDetail.createRoute(playlistId))
                },
                onSettingsClick = {
                    rootNavController.navigate(AppDestinations.Settings.route)
                }
            )
        }

        // Player screen
        composable(
            route = AppDestinations.Player.route,
            arguments = listOf(
                navArgument(PlayerArgs.AUDIO_FILE_URI) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            PlayerScreen()
            // PlayerScreen typically doesn't have a top bar, or manages its own fully.
            // If you need it to set the top bar, you'd add onSetTopBar here and to PlayerScreen.
            // For now, let's assume it doesn't need to control the main app bar.
        }

        // Playlist Detail screen
        composable(
            route = AppDestinations.PlaylistDetail.route,
            arguments = listOf(
                navArgument(PlaylistDetailArgs.PLAYLIST_ID) {
                    type = NavType.LongType
                }
            )
        ) {
            PlaylistDetailScreen(
                onNavigateBack = { rootNavController.popBackStack() },
                onAudioFileClick = { audioFileUri ->
                    val encodedUri = Uri.encode(audioFileUri)
                    rootNavController.navigate(AppDestinations.Player.createRoute(encodedUri))
                },
            )
        }

        // Settings
        composable(AppDestinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
    }
}