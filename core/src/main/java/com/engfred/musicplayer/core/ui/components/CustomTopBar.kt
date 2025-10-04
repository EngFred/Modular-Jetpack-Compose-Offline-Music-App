package com.engfred.musicplayer.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState

/**
 * A custom composable for the top application bar, now more flexible with subtitle support.
 */
@Composable
fun CustomTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    showNavigationIcon: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val animatedHeight by animateDpAsState(
        targetValue = 56.dp + if (subtitle != null) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "topBarHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showNavigationIcon) {
                IconButton(
                    onClick = onNavigateBack ?: {},
                    enabled = onNavigateBack != null
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(0.dp))
            }

            Column(  // Wrap title/subtitle in Column for vertical stack
                horizontalAlignment = Alignment.Start,  // Left-aligned (start)
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (showNavigationIcon) 0.dp else 16.dp)  // Consistent left padding
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(
                    visible = subtitle != null,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(300)
                    ),
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(300)
                    )
                ) {
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,  // Smaller, lighter for subtlety
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),  // Faded for hierarchy
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)  // Tiny gap between title/subtitle
                        )
                    }
                }
            }

            // Actions slot
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}