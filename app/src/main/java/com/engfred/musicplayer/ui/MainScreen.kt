package com.engfred.musicplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
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

/**
 * Main screen of the application, hosting the bottom navigation bar and
 * managing the primary feature screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAudioClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val bottomNavItems = listOf(
        AppDestinations.BottomNavItem.Library,
        AppDestinations.BottomNavItem.Playlists,
        AppDestinations.BottomNavItem.Favorites,
        AppDestinations.BottomNavItem.Search
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Player") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // MiniPlayer placed above the NavigationBar
                MiniPlayer(
                    onMiniPlayerClick = onAudioClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f), // Ensure MiniPlayer is above NavigationBar,
                )
                NavigationBar {
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
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Main content respects the top bar and system insets, but not the bottom bar
        NavHost(
            navController = bottomNavController,
            startDestination = AppDestinations.BottomNavItem.Library.baseRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp, // Let MiniPlayer handle bottom spacing
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            composable(AppDestinations.BottomNavItem.Library.baseRoute) {
                LibraryScreen(onAudioFileClick = onAudioClick)
            }

            composable(AppDestinations.BottomNavItem.Playlists.baseRoute) {
                PlaylistsScreen(onPlaylistClick = onPlaylistClick)
            }

            composable(AppDestinations.BottomNavItem.Favorites.baseRoute) {
                FavoritesScreen(onAudioFileClick = onAudioClick)
            }

            composable(AppDestinations.BottomNavItem.Search.baseRoute) {
                Text("Search Screen Placeholder")
            }
        }
    }
}

