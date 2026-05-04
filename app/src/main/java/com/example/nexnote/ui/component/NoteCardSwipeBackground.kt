package com.example.nexnote.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun NoteCardSwipeBackground(state: SwipeToDismissBoxState) {
    val isThreshold = state.targetValue == SwipeToDismissBoxValue.EndToStart

    val bgColor by animateColorAsState(
        targetValue = if (isThreshold) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "swipe_bg_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        val progress = state.progress
        if (progress > 0.05f || isThreshold) {
            SwipeDeleteIcon(isThreshold)
        }
    }
}

@Composable
private fun SwipeDeleteIcon(isThreshold: Boolean) {
    Icon(
        imageVector = if (isThreshold) Icons.Default.Delete else Icons.Outlined.Delete,
        contentDescription = "Move to trash",
        tint = if (isThreshold) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        },
        modifier = Modifier.graphicsLayer {
            val scale = if (isThreshold) 1.15f else 1.0f
            scaleX = scale
            scaleY = scale
        }
    )
}
