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

@Composable
fun AppNavHost(rootNavController: NavHostController) {
    NavHost(
        navController = rootNavController,
        startDestination = AppDestinations.MainGraph.route
    ) {
        // Main Graph (with bottom nav)
        composable(AppDestinations.MainGraph.route) {
            MainScreen(
                onAudioClick = { audioFileUri, fromMiniPlayer ->
                    val encodedUri = Uri.encode(audioFileUri)
                    rootNavController.navigate(AppDestinations.Player.createRoute(encodedUri, fromMiniPlayer = fromMiniPlayer))
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
                },
                navArgument(PlayerArgs.FROM_MINI_PLAYER) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            PlayerScreen()
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
                    rootNavController.navigate(AppDestinations.Player.createRoute(encodedUri, fromMiniPlayer = false))
                }
            )
        }

        // Settings
        composable(AppDestinations.Settings.route) {
            SettingsScreen(onNavigateBack = { rootNavController.popBackStack() })
        }
    }
}