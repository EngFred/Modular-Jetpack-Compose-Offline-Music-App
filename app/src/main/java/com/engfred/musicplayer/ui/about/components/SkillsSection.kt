package com.engfred.musicplayer.ui.about.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SkillsSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Specialties",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
//            mainAxisSpacing = 8.dp,
//            crossAxisSpacing = 8.dp
        ) {
            SkillChip("Mobile Apps Development")
            SkillChip("Web Apps Development")
            SkillChip("Rest APIs Development")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Languages & Tools",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
//            mainAxisSpacing = 8.dp,
//            crossAxisSpacing = 8.dp
        ) {
            listOf("Kotlin", "Flutter (Dart)", "Express.js", "TypeScript").forEach {
                SkillChip(it)
            }
        }
    }
}

@Composable
fun SkillChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
