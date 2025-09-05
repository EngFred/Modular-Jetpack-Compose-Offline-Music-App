package com.engfred.musicplayer.ui.about.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ConnectingAirports
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Whatsapp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactSection(
    onEmailClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onLinkedInClick: () -> Unit,
    onGitHubClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Contact Me",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        ContactButton(icon = Icons.Rounded.Email, label = "Email Me", onClick = onEmailClick)
        ContactButton(icon = Icons.Rounded.Whatsapp, label = "WhatsApp", onClick = onWhatsAppClick)
        ContactButton(icon = Icons.Rounded.Phone, label = "Call", onClick = onPhoneClick)
        ContactButton(icon = Icons.Rounded.ConnectingAirports, label = "LinkedIn", onClick = onLinkedInClick)
        ContactButton(icon = Icons.Rounded.Code, label = "GitHub", onClick = onGitHubClick)
    }
}

@Composable
fun ContactButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Row(

            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
