package com.engfred.musicplayer.navigation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.engfred.musicplayer.ui.about.screen.CustomSplashScreen
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.core.net.toUri
import kotlinx.coroutines.delay

/**
 * Defines the main navigation graph for the application.
 */
@RequiresApi(Build.VERSION_CODES.M)
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
    isPlayerActive: Boolean
) {

    // Set the start destination based on the condition
    val startDestination = if (isPlayerActive) {
        AppDestinations.MainGraph.route
    } else {
        AppDestinations.Splash.route
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
                windowWidthSizeClass = windowWidthSizeClass
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
                onNavigateToNowPlaying = onNavigateToNowPlaying,
                windowWidthSizeClass = windowWidthSizeClass
            )
        }

        // Settings
        composable(AppDestinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        // About
//        composable(AppDestinations.About.route) {
//            AboutScreen(
//                onNavigateBack = { rootNavController.popBackStack() }
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