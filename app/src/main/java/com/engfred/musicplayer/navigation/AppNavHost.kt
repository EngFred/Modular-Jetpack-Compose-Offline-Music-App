package com.engfred.musicplayer.navigation

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.feature_library.presentation.screens.EditSongScreenContainer
import com.engfred.musicplayer.feature_player.presentation.screens.NowPlayingScreen
import com.engfred.musicplayer.feature_playlist.presentation.screens.PlaylistDetailScreen
import com.engfred.musicplayer.feature_playlist.presentation.viewmodel.detail.PlaylistDetailArgs
import com.engfred.musicplayer.feature_settings.presentation.screens.SettingsScreen
import com.engfred.musicplayer.ui.MainScreen
import com.engfred.musicplayer.ui.about.screen.CustomSplashScreen
import kotlinx.coroutines.delay

/**
 * Defines the main navigation graph for the application.
 */
@UnstableApi
@Composable
fun AppNavHost(
    rootNavController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrev: () -> Unit,
    playingAudioFile: AudioFile?,
    isPlaying: Boolean,
    context: Context,
    onNavigateToNowPlaying: () -> Unit,
    isPlayerActive: Boolean,
    isPlayingExternalUri: Boolean
) {

    // Set the start destination based on the condition
    val startDestination = remember {
        if (isPlayerActive || isPlayingExternalUri) {
            AppDestinations.MainGraph.route
        } else {
            AppDestinations.Splash.route
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = startDestination,
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            )
        )
    ) {
        // Splash screen
        composable(AppDestinations.Splash.route) {
            CustomSplashScreen()
            LaunchedEffect(Unit) {
                delay(3000) // 3-second delay
                rootNavController.navigate(AppDestinations.MainGraph.route) {
                    popUpTo(AppDestinations.Splash.route) { inclusive = true } // Remove splash from back stack
                }
            }
        }

        // Main Graph (with bottom nav)
        composable(AppDestinations.MainGraph.route) {
            MainScreen(

                onNavigateToNowPlaying = onNavigateToNowPlaying,
                onPlaylistClick = { playlistId ->
                    rootNavController.navigate(AppDestinations.PlaylistDetail.createRoute(playlistId))
                },
                onSettingsClick = {
                    rootNavController.navigate(AppDestinations.Settings.route)
                },
                onContactDeveloper = {
                    launchWhatsapp(context = context )
                },
                onPlayPause = onPlayPause,
                onPlayNext = onPlayNext,
                onPlayPrev = onPlayPrev,
                isPlaying = isPlaying,
                playingAudioFile = playingAudioFile,
                windowWidthSizeClass = windowWidthSizeClass,
                onEditSong = { audioFile ->
                    rootNavController.navigate(AppDestinations.EditSong.createRoute(audioFile.id))
                }
            )
        }

        // Player screen
        composable(
            route = AppDestinations.NowPlaying.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400)
                )
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight },
                    animationSpec = tween(durationMillis = 400)
                )
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400)
                )
            }
        ) {
            NowPlayingScreen(
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
                onNavigateBack = { rootNavController.navigateUp() },
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                windowWidthSizeClass = windowWidthSizeClass,
                onEditInfo = {
                    rootNavController.navigate(AppDestinations.EditSong.createRoute(it.id))
                }
            )
        }

        // Settings
        composable(AppDestinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = { rootNavController.navigateUp() }
            )
        }

        composable(
            route = AppDestinations.EditSong.route,
            arguments = listOf(navArgument("audioId") { type = NavType.LongType })
        ) { backStackEntry ->
            val audioId = backStackEntry.arguments?.getLong("audioId") ?: -1L
            // Compose screen that handles editing (ViewModel handles loading/saving)
            EditSongScreenContainer(
                audioId = audioId,
                onFinish = { rootNavController.navigateUp() }
            )
        }

        // About
//        composable(AppDestinations.About.route) {
//            AboutScreen(
//                onNavigateBack = { rootNavController.navigateUp() }
//            )
//        }
    }
}

private fun launchWhatsapp(context: Context) {
    try {
        Toast.makeText(context, "Opening whatsapp...", Toast.LENGTH_SHORT).show()
        val url = "https://wa.me/256754348118"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        //show toast
        Toast.makeText(context, "Error opening whatsapp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}