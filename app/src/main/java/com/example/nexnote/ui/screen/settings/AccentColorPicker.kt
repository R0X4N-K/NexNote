package com.example.nexnote.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nexnote.domain.model.AccentColor
import com.example.nexnote.ui.theme.NexBluePrimary
import com.example.nexnote.ui.theme.NexGreenPrimary
import com.example.nexnote.ui.theme.NexOrangePrimary
import com.example.nexnote.ui.theme.NexPrimary
import com.example.nexnote.ui.theme.NexRedPrimary
import com.example.nexnote.ui.theme.NexTealPrimary

@Composable
internal fun AccentColorPicker(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    val swatches = listOf(
        AccentColor.VIOLET to NexPrimary,
        AccentColor.BLUE to NexBluePrimary,
        AccentColor.GREEN to NexGreenPrimary,
        AccentColor.ORANGE to NexOrangePrimary,
        AccentColor.RED to NexRedPrimary,
        AccentColor.TEAL to NexTealPrimary
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        swatches.forEach { (accent, swatch) ->
            AccentColorSwatch(
                accent = accent,
                swatch = swatch,
                isSelected = accent == selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun AccentColorSwatch(
    accent: AccentColor,
    swatch: Color,
    isSelected: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .accentColorSwatchStyle(
                isSelected = isSelected,
                borderColor = borderColor,
                swatch = swatch,
                onClick = { onSelect(accent) }
            )
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Modifier.accentColorSwatchStyle(
    isSelected: Boolean,
    borderColor: Color,
    swatch: Color,
    onClick: () -> Unit
): Modifier {
    return size(44.dp)
        .clip(CircleShape)
        .border(
            width = if (isSelected) 3.dp else 0.dp,
            color = borderColor,
            shape = CircleShape
        )
        .padding(if (isSelected) 3.dp else 0.dp)
        .clip(CircleShape)
        .background(swatch)
        .clickable(onClick = onClick)
}
