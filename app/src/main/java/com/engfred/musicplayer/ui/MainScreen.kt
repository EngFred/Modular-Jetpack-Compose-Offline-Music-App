package com.engfred.musicplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.core.ui.MiniPlayer
import com.engfred.musicplayer.core.util.restartApp
import com.engfred.musicplayer.feature_favorites.presentation.screen.FavoritesScreen
import com.engfred.musicplayer.feature_library.presentation.screens.LibraryScreen
import com.engfred.musicplayer.feature_playlist.presentation.screens.PlaylistsScreen
import com.engfred.musicplayer.navigation.AppDestinations

/**
 * Main screen of the application, hosting the custom bottom navigation bar and
 * managing the primary feature screens.
 */
@Composable
fun MainScreen(
    onNavigateToNowPlaying: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onContactDeveloper: () -> Unit,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrev: () -> Unit,
    playingAudioFile: AudioFile?,
    isPlaying: Boolean,
    onEditSong: (AudioFile) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    audioItems: List<AudioFile>,
    onReleasePlayer: () -> Unit,
    onCreatePlaylist: () -> Unit,
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Ensure the top bar respects the status bar inset so content is NOT drawn under the system status bar.
            Box(modifier = Modifier.statusBarsPadding()) {
                CustomTopBar(
                    modifier = Modifier.padding(start = 10.dp),
                    title = "Music",
                    showNavigationIcon = false,
                    onNavigateBack = null,
                    actions = {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "more icon")
                        }
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false },
                            offset = DpOffset(x = (-16).dp, y = 0.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showDropdownMenu = false
                                    onSettingsClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Contact Developer", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showDropdownMenu = false
                                    onContactDeveloper()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restart Music", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showDropdownMenu = false
                                    showRestartDialog = true
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            // Use navigationBarsPadding() not systemBarsPadding() — we only need the bottom inset here.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (playingAudioFile != null && audioItems.isNotEmpty()) {
                    MiniPlayer(
                        onClick = onNavigateToNowPlaying,
                        modifier = Modifier.fillMaxWidth(),
                        onPlayPause = onPlayPause,
                        onPlayNext = onPlayNext,
                        onPlayPrev = onPlayPrev,
                        isPlaying = isPlaying,
                        playingAudioFile = playingAudioFile,
                    )
                } else {
                    PlayShuffleBar(
                        onPlayAll = onPlayAll,
                        onShuffleAll = onShuffleAll,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                //removed bottom = 8.dp from the Row padding — navigationBarsPadding() already reserves safe area.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.baseRoute
                        } == true
                        CustomBottomNavItem(
                            item = item,
                            isSelected = selected,
                            onClick = {
                                bottomNavController.navigate(item.baseRoute) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Apply the scaffold inner padding to the NavHost so each destination sits below the topBar and above the bottomBar.
        NavHost(
            navController = bottomNavController,
            startDestination = AppDestinations.BottomNavItem.Library.baseRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(AppDestinations.BottomNavItem.Library.baseRoute) {
                LibraryScreen(
                    onEditSong = onEditSong
                ) // LibraryScreen will be laid out inside NavHost's padded area
            }
            composable(AppDestinations.BottomNavItem.Playlists.baseRoute) {
                PlaylistsScreen(onPlaylistClick = onPlaylistClick, onCreatePlaylist = onCreatePlaylist)
            }
            composable(AppDestinations.BottomNavItem.Favorites.baseRoute) {
                FavoritesScreen(onEditSong)
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart Music") },
            text = {
                Column {
                    Text(
                        "Are you sure you want to restart Music? This will stop current playback."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        // brief, non-technical explanation
                        "Note: On some phones the system may stop the app from starting again (battery or system settings). If that happens the app will close — just open it again manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        restartApp(
                            context = context.applicationContext ?: context,
                            delayMs = 300,
                            toastMessage = "Restarting music player...",
                            onBeforeRestart = {
                                // release player / stop services before kill
                                onReleasePlayer()
                            }
                        )
                    }
                ) {
                    Text("Restart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )

    }
}

@Composable
private fun CustomBottomNavItem(
    item: AppDestinations.BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 300), label = "background_color_animation"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300), label = "content_color_animation"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(animatedBackgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = animatedContentColor,
            modifier = Modifier.size(24.dp)
        )
        if (isSelected) {
            Text(
                text = item.label,
                color = animatedContentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}