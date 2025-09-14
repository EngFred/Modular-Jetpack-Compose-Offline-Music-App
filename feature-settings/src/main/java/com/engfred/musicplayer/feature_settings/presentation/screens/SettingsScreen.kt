package com.engfred.musicplayer.feature_settings.presentation.screens

import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.domain.model.AudioFile
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.core.ui.MiniPlayer
import com.engfred.musicplayer.feature_settings.presentation.viewmodel.SettingsEvent
import com.engfred.musicplayer.feature_settings.presentation.viewmodel.SettingsViewModel
import java.time.Year

/**
 * SettingsScreen — accepts optional drawable resource IDs for social icons and an avatar drawable.
 *
 * Host (app) should call this and pass R.drawable.* icons and avatar from the app module.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    @DrawableRes githubIconRes: Int? = null,
    @DrawableRes linkedInIconRes: Int? = null,
    @DrawableRes emailIconRes: Int? = null,
    @DrawableRes developerAvatarRes: Int? = null,
    onMiniPlayerClick: () -> Unit,
    onMiniPlayPauseClick: () -> Unit,
    onMiniPlayNext: () -> Unit,
    onMiniPlayPrevious: () -> Unit,
    playingAudioFile: AudioFile?,
    isPlaying: Boolean,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CustomTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "Settings",
                showNavigationIcon = true,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            if (playingAudioFile != null) {
                MiniPlayer(
                    modifier = Modifier.navigationBarsPadding(),
                    onClick = onMiniPlayerClick,
                    onPlayPause = onMiniPlayPauseClick,
                    onPlayNext = onMiniPlayNext,
                    onPlayPrev = onMiniPlayPrevious,
                    playingAudioFile = playingAudioFile,
                    isPlaying = isPlaying
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Error message if any
            if (uiState.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Settings sections
            SettingsSection(
                title = "App Theme",
                subtitle = "Choose a look that suits you",
                icon = Icons.Rounded.Brush,
                items = AppThemeType.entries,
                selectedItem = uiState.selectedTheme,
                displayName = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.titlecase() } },
                onSelect = { viewModel.onEvent(SettingsEvent.UpdateTheme(it)) }
            )

            SettingsSection(
                title = "Now Playing Layout",
                subtitle = "Layout shown on the player screen",
                icon = Icons.Rounded.PlayArrow,
                items = PlayerLayout.entries,
                selectedItem = uiState.selectedPlayerLayout,
                displayName = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.titlecase() } },
                onSelect = { viewModel.onEvent(SettingsEvent.UpdatePlayerLayout(it)) }
            )

            SettingsSection(
                title = "Playlist Layout",
                subtitle = "How your playlists are displayed",
                icon = Icons.Rounded.QueueMusic,
                items = PlaylistLayoutType.entries,
                selectedItem = uiState.playlistLayoutType,
                displayName = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.titlecase() } },
                onSelect = { viewModel.onEvent(SettingsEvent.UpdatePlaylistLayout(it)) }
            )

            SettingsSection(
                title = "Audio Preset",
                subtitle = "Select an equalizer preset for playback",
                icon = Icons.Rounded.Equalizer,
                items = AudioPreset.entries,
                selectedItem = uiState.audioPreset,
                displayName = { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.titlecase() } },
                onSelect = { viewModel.onEvent(SettingsEvent.UpdateAudioPreset(it)) }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Changes are applied immediately and persist across launches.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Developer info: forward drawables and optional avatar
            // DeveloperInfoSection(
            //     developerName = "Engineer Fred",
            //     developerRole = "Software Engineer | Software Developer",
            //     email = "engfred88@gmail.com",
            //     githubUrl = "https://github.com/EngFred",
            //     linkedInUrl = "https://www.linkedin.com/in/fred-omongole-a5943b2b0/",
            //     githubIconRes = githubIconRes,
            //     linkedInIconRes = linkedInIconRes,
            //     emailIconRes = emailIconRes,
            //     developerAvatarRes = developerAvatarRes // host provides avatar drawable id
            // )

            // REPLACED: simple app version + copyright section
            AppVersionSection(
                copyrightText = "© 2025 Engineer Fred",
            )
        }
    }
}

/**
 * Generic settings section with radio items.
 */
@Composable
private fun <T> SettingsSection(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<T>,
    selectedItem: T,
    displayName: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Optional subtitle
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }

        val cardElevation by animateDpAsState(targetValue = 6.dp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(cardElevation, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedItem == item

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 220,
                            easing = FastOutSlowInEasing
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .background(backgroundColor)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(item) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = displayName(item),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    if (index < items.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppVersionSection(
    copyrightText: String
) {
    val context = LocalContext.current
    val cardElevation by animateDpAsState(targetValue = 6.dp)

    // Safe package info retrieval with fallback values
    val (versionName, versionCode) = try {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use the new PackageInfoFlags API on Android T+
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        val name = pkgInfo.versionName ?: "1.0.0"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toString()
        }
        name to code
    } catch (e: Exception) {
        "1.0.0" to "1"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "App version card" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "App Version",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version: v$versionName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Build: $versionCode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = copyrightText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Made with love • v$versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Developer info / copyright section.
 * Accepts optional drawable resource ids; falls back to Material icons and initials if not provided.
 *
 * NOTE: The invocation of this composable in SettingsScreen is currently commented out.
 * Keep this function if you want to re-enable the full developer card later.
 */
@Composable
private fun DeveloperInfoSection(
    developerName: String,
    developerRole: String,
    email: String,
    githubUrl: String,
    linkedInUrl: String,
    @DrawableRes githubIconRes: Int? = null,
    @DrawableRes linkedInIconRes: Int? = null,
    @DrawableRes emailIconRes: Int? = null,
    @DrawableRes developerAvatarRes: Int? = null // optional avatar drawable
) {
    val cardElevation by animateDpAsState(targetValue = 6.dp)
    val year = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Year.now().value.toString()
        } else {
            java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
        }
    } catch (e: Exception) {
        java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // safe painter loader, returns null if resource invalid
    @Composable
    fun loadPainter(@DrawableRes res: Int?) = res?.let { androidx.compose.ui.res.painterResource(id = it) }

    val githubPainter = loadPainter(githubIconRes)
    val linkedInPainter = loadPainter(linkedInIconRes)
    val emailPainter = loadPainter(emailIconRes)
    val avatarPainter = loadPainter(developerAvatarRes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Developer info card" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar: if avatarPainter exists, show circular image; otherwise initials box
                if (avatarPainter != null) {
                    Image(
                        painter = avatarPainter,
                        contentDescription = "Developer avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = "Developer avatar" },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = developerName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .semantics { contentDescription = "Developer avatar" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = developerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = developerRole,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }

            // Actions: each item is a clickable row (icon + label) to make click targets clear and accessible.
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Reusable small helper to render icon + label as a clickable button-like row
                @Composable
                fun ActionItem(
                    painter: androidx.compose.ui.graphics.painter.Painter?,
                    fallbackVector: androidx.compose.ui.graphics.vector.ImageVector,
                    label: String,
                    onClick: () -> Unit,
                    tintIcon: Boolean = true
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onClick, role = Role.Button)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .semantics { contentDescription = label }
                    ) {
                        if (painter != null) {
                            Image(
                                painter = painter,
                                contentDescription = label,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = if (tintIcon) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null
                            )
                        } else {
                            Icon(
                                imageVector = fallbackVector,
                                contentDescription = label,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                ActionItem(
                    painter = githubPainter,
                    fallbackVector = Icons.Rounded.Link,
                    label = "GitHub",
                    onClick = { uriHandler.openUri(githubUrl) },
                    tintIcon = true
                )

                ActionItem(
                    painter = linkedInPainter,
                    fallbackVector = Icons.Rounded.Link,
                    label = "LinkedIn",
                    onClick = { uriHandler.openUri(linkedInUrl) },
                    tintIcon = false
                )

                ActionItem(
                    painter = emailPainter,
                    fallbackVector = Icons.Rounded.Email,
                    label = "Email",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:$email".toUri()
                        }
                        context.startActivity(intent)
                    },
                    tintIcon = false
                )

            }

            Spacer(modifier = Modifier.height(6.dp))

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "© $year $developerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Made with love • v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}