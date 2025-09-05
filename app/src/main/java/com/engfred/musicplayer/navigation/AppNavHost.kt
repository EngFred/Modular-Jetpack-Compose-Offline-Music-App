package com.engfred.musicplayer.navigation

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_player.presentation.screens.PlayerScreen
import com.engfred.musicplayer.feature_playlist.presentation.screens.PlaylistDetailScreen
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail.PlaylistDetailArgs
import com.engfred.musicplayer.feature_settings.presentation.screens.SettingsScreen
import com.engfred.musicplayer.ui.MainScreen
import com.engfred.musicplayer.ui.about.screen.AboutScreen

/**
 * Defines the main navigation graph for the application.
 */
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun AppNavHost(
    rootNavController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrev: () -> Unit,
    playingAudioFile: AudioFile?,
    isPlaying: Boolean
) {
    NavHost(
        navController = rootNavController,
        startDestination = AppDestinations.MainGraph.route
    ) {
        // Main Graph (with bottom nav)
        composable(AppDestinations.MainGraph.route) {
            MainScreen(
                onNavigateToNowPlaying = {
                    rootNavController.navigate(AppDestinations.NowPlaying.route)
                },
                onPlaylistClick = { playlistId ->
                    rootNavController.navigate(AppDestinations.PlaylistDetail.createRoute(playlistId))
                },
                onSettingsClick = {
                    rootNavController.navigate(AppDestinations.Settings.route)
                },
                onAboutClick = {
                    rootNavController.navigate(AppDestinations.About.route)
                },
                onPlayPause = onPlayPause,
                onPlayNext = onPlayNext,
                onPlayPrev = onPlayPrev,
                isPlaying = isPlaying,
                playingAudioFile = playingAudioFile,
                windowWidthSizeClass = windowWidthSizeClass
            )
        }

        // Player screen
        composable(
            route = AppDestinations.NowPlaying.route
        ) {
            PlayerScreen(
                windowWidthSizeClass = windowWidthSizeClass,
                windowHeightSizeClass = windowHeightSizeClass,
                onNavigateUp = {
                    rootNavController.navigateUp()
                }
            )
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
                onNavigateToNowPlaying = {
                    rootNavController.navigate(AppDestinations.NowPlaying.route)
                },
                windowWidthSizeClass = windowWidthSizeClass
            )
        }

        // Settings
        composable(AppDestinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        // about
        composable(AppDestinations.About.route) {
            AboutScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

    }
}