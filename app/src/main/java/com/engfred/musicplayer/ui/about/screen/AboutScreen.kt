package com.engfred.musicplayer.ui.about.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.engfred.musicplayer.core.ui.CustomTopBar
import com.engfred.musicplayer.ui.about.components.BioSection
import com.engfred.musicplayer.ui.about.components.ContactSection
import com.engfred.musicplayer.ui.about.components.ProfileHeader
import com.engfred.musicplayer.ui.about.components.SkillsSection
import kotlinx.coroutines.delay

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val showContent = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300) // Slight delay for entrance animation
        showContent.value = true
    }

    Scaffold(
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
                        // Keep the CustomTopBar as requested
                        CustomTopBar(
                            title = "About Developer",
                            showNavigationIcon = true,
                            onNavigateBack = onNavigateBack
                        )
                    }

                    item {
                        AnimatedVisibility(
                            visible = showContent.value,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ProfileHeader()
//                                Spacer(modifier = Modifier.height(32.dp))
//                                BioSection()
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