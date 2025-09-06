package com.engfred.musicplayer.ui.about.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.ui.about.components.ContactSection
import com.engfred.musicplayer.ui.about.components.ProfileHeader
import com.engfred.musicplayer.ui.about.components.SkillsSection

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CustomTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = "About Developer",
                showNavigationIcon = true,
                onNavigateBack = onNavigateBack
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    item {
                        // All content is now a direct child of the LazyColumn item, without AnimatedVisibility
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ProfileHeader()
                            Spacer(modifier = Modifier.height(32.dp))
                            SkillsSection()
                            Spacer(modifier = Modifier.height(32.dp))
                            ContactSection(
                                onEmailClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:engfred88@gmail.com".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                onWhatsAppClick = {
                                    val url = "https://wa.me/256754348118"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = url.toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                onPhoneClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = "tel:0785243836".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                onLinkedInClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = "https://www.linkedin.com/in/fred-omongole-a5943b2b0/".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                onGitHubClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = "https://github.com/EngFred?tab=repositories".toUri()
                                    }
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }

                    item {
                        // Footer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "© 2025 Fred Omongole",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    )
}