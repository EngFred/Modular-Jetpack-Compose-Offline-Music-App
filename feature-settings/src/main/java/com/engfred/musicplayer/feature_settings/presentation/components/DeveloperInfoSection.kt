package com.engfred.musicplayer.feature_settings.presentation.components

import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import java.time.Year

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