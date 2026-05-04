package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorToolbar(
    showPreview: Boolean,
    isTemplateMode: Boolean,
    isDarkTheme: Boolean,
    hasCustomColor: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    noteBackground: Color,
    onTogglePreview: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertChecklist: () -> Unit,
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit,
    onThemeToggle: () -> Unit,
    onToggleColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(noteBackground)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorToolbarIcon(
                onClick = onTogglePreview,
                imageVector = if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                contentDescription = if (showPreview) "Back to editing" else "Preview",
                tint = if (showPreview) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            EditorToolbarIcon(
                onClick = onUndo,
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Undo",
                enabled = canUndo
            )
            EditorToolbarIcon(
                onClick = onRedo,
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Redo",
                enabled = canRedo
            )
            if (!isTemplateMode) {
                EditorToolbarIcon(onInsertImage, Icons.Default.Image, "Insert image")
            }
            EditorToolbarIcon(onInsertChecklist, Icons.Default.CheckBox, "Insert checklist")
            EditorLinkMenu(
                onInsertWebLink = onInsertWebLink,
                onInsertNoteLink = onInsertNoteLink
            )
        }
        if (!isTemplateMode) {
            EditorToolbarIcon(
                onClick = onToggleColorPicker,
                imageVector = Icons.Default.Palette,
                contentDescription = "Note background color",
                tint = if (hasCustomColor) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        EditorToolbarIcon(
            onClick = onThemeToggle,
            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
        )
    }
}

@Composable
private fun EditorLinkMenu(
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        EditorToolbarIcon(
            onClick = { expanded = true },
            imageVector = Icons.Default.Link,
            contentDescription = "Insert link"
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Web link") },
                onClick = {
                    expanded = false
                    onInsertWebLink()
                }
            )
            DropdownMenuItem(
                text = { Text("Note link") },
                onClick = {
                    expanded = false
                    onInsertNoteLink()
                }
            )
        }
    }
}

@Composable
private fun EditorToolbarIcon(
    onClick: () -> Unit,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    enabled: Boolean = true
) {
    val iconTint = if (enabled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconTint
        )
    }
}
