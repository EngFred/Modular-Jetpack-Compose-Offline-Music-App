package com.engfred.musicplayer.feature_player.presentation.components.layouts.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A dedicated Composable for a toggleable favorite button with a subtle scale animation.
 *
 * @param isFavorite Boolean indicating if the item is currently marked as favorite.
 * @param onToggleFavorite Callback to be invoked when the favorite status is toggled.
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "favoriteScale"
    )
    IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.size(50.dp) // Maintain consistent touch target size
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
            // Consider using MaterialTheme.colorScheme.error or a custom accent color
            // if this specific color is used broadly in your theme for 'favorite' states.
            tint = if (isFavorite) Color(0xFFE91E63) else LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.graphicsLayer {
                scaleX = favoriteScale
                scaleY = favoriteScale
            }
        )
    }
}