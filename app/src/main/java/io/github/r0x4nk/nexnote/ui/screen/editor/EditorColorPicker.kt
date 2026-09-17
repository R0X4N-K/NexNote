package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.NOTE_COLOR_PALETTE
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.theme.rememberNoteColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoteColorPicker(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    noteBackground: Color,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .background(noteBackground)
            .selectableGroup()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
    val colors = rememberNoteColors(color)
    val swatchColor = colors.container
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    val nameRes = noteColorNameRes(NOTE_COLOR_PALETTE.indexOf(color))
    val name = stringResource(nameRes)
    val description = stringResource(R.string.note_color_description, name)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
            .clip(CircleShape)
            .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(color) }
            .padding(6.dp)
            .background(swatchColor, CircleShape)
            .border(borderWidth, if (isSelected) colors.outline else borderColor, CircleShape)
    ) {
        if (isSelected) Icon(Icons.Default.Check, null, tint = colors.onContainer, modifier = Modifier.size(20.dp))
    }
}

@StringRes
private fun noteColorNameRes(paletteIndex: Int): Int = when (paletteIndex) {
    0 -> R.string.note_color_default
    1 -> R.string.note_color_red
    2 -> R.string.note_color_orange
    3 -> R.string.note_color_yellow
    4 -> R.string.note_color_green
    5 -> R.string.note_color_blue
    6 -> R.string.note_color_purple
    7 -> R.string.note_color_warm_grey
    else -> R.string.note_color_custom
}
