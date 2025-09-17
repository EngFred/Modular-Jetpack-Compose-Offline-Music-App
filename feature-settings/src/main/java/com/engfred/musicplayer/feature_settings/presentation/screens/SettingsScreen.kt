package com.engfred.musicplayer.feature_settings.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.engfred.musicplayer.core.domain.model.AudioPreset
import com.engfred.musicplayer.core.domain.model.PlayerLayout
import com.engfred.musicplayer.core.domain.model.PlaylistLayoutType
import com.engfred.musicplayer.core.ui.theme.AppThemeType
import com.engfred.musicplayer.feature_settings.presentation.components.AppVersionSection
import com.engfred.musicplayer.feature_settings.presentation.components.SettingsSection
import com.engfred.musicplayer.feature_settings.presentation.viewmodel.SettingsEvent
import com.engfred.musicplayer.feature_settings.presentation.viewmodel.SettingsViewModel

/**
 * SettingsScreen — accepts optional drawable resource IDs for social icons and an avatar drawable.
 *
 * Host (app) should call this and pass R.drawable.* icons and avatar from the app module.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
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
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
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
                textAlign = TextAlign.Center,
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

            //app version + copyright section
            AppVersionSection(
                copyrightText = "© 2025 Engineer Fred",
            )
        }
    }
}