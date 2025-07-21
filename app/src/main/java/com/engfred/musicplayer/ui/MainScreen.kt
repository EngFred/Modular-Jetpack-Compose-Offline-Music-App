package com.engfred.musicplayer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.engfred.musicplayer.feature_equalizer.presentation.screens.EqualizerScreen
import com.engfred.musicplayer.core.ui.CustomTopBar

/**
 * Main screen of the application, hosting the bottom navigation bar and
 * managing the primary feature screens.
 */
@Composable
fun MainScreen(
    onAudioClick: (String, Boolean) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val bottomNavItems = listOf(
        AppDestinations.BottomNavItem.Library,
        AppDestinations.BottomNavItem.Playlists,
        AppDestinations.BottomNavItem.Favorites,
        AppDestinations.BottomNavItem.Equalizer
    )

    Scaffold(
        bottomBar = {
            Column {
                // MiniPlayer placed above the NavigationBar
                MiniPlayer(
                    onMiniPlayerClick = { uri -> onAudioClick(uri, true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f), // Ensure MiniPlayer is above NavigationBar
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
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            CustomTopBar(
                title = "Music Player",
                showNavigationIcon = false,
                onNavigateBack = null,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            NavHost(
                navController = bottomNavController,
                startDestination = AppDestinations.BottomNavItem.Library.baseRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp, bottom = paddingValues.calculateBottomPadding())
            ) {
                composable(AppDestinations.BottomNavItem.Library.baseRoute) {
                    LibraryScreen()
                }
                composable(AppDestinations.BottomNavItem.Playlists.baseRoute) {
                    PlaylistsScreen(onPlaylistClick = onPlaylistClick)
                }
                composable(AppDestinations.BottomNavItem.Favorites.baseRoute) {
                    FavoritesScreen()
                }
                composable(AppDestinations.BottomNavItem.Equalizer.baseRoute) {
                    EqualizerScreen()
                }
            }
        }
    }
}