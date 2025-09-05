package com.engfred.musicplayer.ui

import MiniPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.engfred.musicplayer.feature_favorites.presentation.screen.FavoritesScreen
import com.engfred.musicplayer.feature_library.presentation.screens.LibraryScreen
import com.engfred.musicplayer.feature_playlist.presentation.screens.PlaylistsScreen
import com.engfred.musicplayer.navigation.AppDestinations
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.engfred.musicplayer.core.ui.CustomTopBar
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.rounded.MoreVert
import com.engfred.musicplayer.core.domain.model.AudioFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.engfred.musicplayer.core.ui.theme.FrostPrimary
import com.engfred.musicplayer.core.util.restartApp

/**
 * Main screen of the application, hosting the bottom navigation bar and
 * managing the primary feature screens.
 */
@Composable
fun MainScreen(
    onNavigateToNowPlaying: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrev: () -> Unit,
    playingAudioFile: AudioFile?,
    isPlaying: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val bottomNavController = rememberNavController()
    val bottomNavItems = listOf(
        AppDestinations.BottomNavItem.Library,
        AppDestinations.BottomNavItem.Playlists,
        AppDestinations.BottomNavItem.Favorites,
    )

    var showDropdownMenu by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Get the current color scheme from the MaterialTheme to determine if it's a light theme
    // We assume FROSTBYTE is the only light theme. You can pass the selectedTheme down if you
    // have access to it here, or use compositionLocalOf for the theme.
    // For simplicity, we'll check based on a characteristic of LightColorScheme.
    val isFrostbyteTheme = MaterialTheme.colorScheme.primary == FrostPrimary

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(
                    onClick = onNavigateToNowPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    onPlayPause = onPlayPause,
                    onPlayNext = onPlayNext,
                    onPlayPrev = onPlayPrev,
                    isPlaying = isPlaying,
                    playingAudioFile = playingAudioFile,
                    windowWidthSizeClass = windowWidthSizeClass
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.baseRoute
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                bottomNavController.navigate(item.baseRoute) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                // If Frostbyte, use onSurface for selected text/icon, otherwise use onPrimaryContainer (white usually)
                                selectedIconColor = if (isFrostbyteTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = if (isFrostbyteTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            CustomTopBar(
                title = "Music Player",
                showNavigationIcon = false,
                onNavigateBack = null,
                actions = {
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "more icon")
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showDropdownMenu = false
                                onSettingsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About Developer") },
                            onClick = {
                                showDropdownMenu = false
                                onAboutClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restart Player") },
                            onClick = {
                                showDropdownMenu = false
                                showRestartDialog = true
                            }
                        )
                    }
                }
            )
            NavHost(
                navController = bottomNavController,
                startDestination = AppDestinations.BottomNavItem.Library.baseRoute,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                composable(AppDestinations.BottomNavItem.Library.baseRoute) {
                    LibraryScreen()
                }
                composable(AppDestinations.BottomNavItem.Playlists.baseRoute) {
                    PlaylistsScreen(onPlaylistClick = onPlaylistClick, windowWidthSizeClass = windowWidthSizeClass)
                }
                composable(AppDestinations.BottomNavItem.Favorites.baseRoute) {
                    FavoritesScreen()
                }
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart Application") },
            text = { Text("Are you sure you want to restart the music player? This will stop current playback.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        restartApp(context, "Restarting music player...")
                    }
                ) {
                    Text("Restart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}