package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.NexIconButton

/**
 * Compact editor actions anchored above the IME while editing.
 *
 * The link-type chooser is exposed via [linkMenuExpanded] / [onLinkMenuExpandedChange]
 * so the parent can keep the toolbar mounted while the dropdown is open. This prevents
 * the dropdown's focusable popup — which collapses the IME for a moment — from also
 * tearing the toolbar (and the menu itself) down before the user can pick an option.
 */
@Composable
internal fun EditorKeyboardToolbar(
    visible: Boolean,
    isTemplateMode: Boolean,
    isDarkTheme: Boolean,
    hasCustomColor: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    noteBackground: Color,
    linkMenuExpanded: Boolean,
    onLinkMenuExpandedChange: (Boolean) -> Unit,
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
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 140)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .background(noteBackground)
                .padding(start = 6.dp, top = 0.dp, end = 6.dp, bottom = 1.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        expanded = linkMenuExpanded,
                        onExpandedChange = onLinkMenuExpandedChange,
                        onInsertWebLink = onInsertWebLink,
                        onInsertNoteLink = onInsertNoteLink
                    )
                }
                if (!isTemplateMode) {
                    EditorToolbarIcon(
                        onClick = onToggleColorPicker,
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Note background color",
                        selected = hasCustomColor
                    )
                }
                EditorToolbarIcon(
                    onClick = onThemeToggle,
                    imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
                )
            }
        }
    }
}

/**
 * Inline link-type chooser anchored to the toolbar's link button.
 *
 * State is fully hoisted: the parent owns [expanded] so it can keep the toolbar
 * (and therefore this dropdown anchor) composed even when the IME briefly collapses
 * after the focusable popup grabs window focus.
 */
@Composable
private fun EditorLinkMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit
) {
    Box {
        EditorToolbarIcon(
            onClick = { onExpandedChange(true) },
            imageVector = Icons.Default.Link,
            contentDescription = "Insert link",
            selected = expanded
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Web link") },
                onClick = {
                    onExpandedChange(false)
                    onInsertWebLink()
                }
            )
            DropdownMenuItem(
                text = { Text("Note link") },
                onClick = {
                    onExpandedChange(false)
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
    enabled: Boolean = true,
    selected: Boolean = false
) {
    NexIconButton(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = onClick,
        enabled = enabled,
        selected = selected
    )
}
