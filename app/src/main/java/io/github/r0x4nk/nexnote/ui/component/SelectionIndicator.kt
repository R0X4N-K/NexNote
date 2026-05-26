package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .padding(start = 6.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) {
                Icons.Default.CheckCircle
            } else {
                Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = if (selected) "Selected" else "Not selected",
            tint = if (selected) {
                primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}
