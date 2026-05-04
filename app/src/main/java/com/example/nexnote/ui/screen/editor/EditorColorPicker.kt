package com.example.nexnote.ui.screen.editor

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun NoteColorPicker(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    noteBackground: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(noteBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NOTE_COLOR_PALETTE.forEach { color ->
            NoteColorSwatch(
                color = color,
                isSelected = color == selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun NoteColorSwatch(
    color: Int?,
    isSelected: Boolean,
    onSelect: (Int?) -> Unit
) {
    val swatchColor = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(borderWidth, borderColor, CircleShape)
            .clickable { onSelect(color) }
    )
}
