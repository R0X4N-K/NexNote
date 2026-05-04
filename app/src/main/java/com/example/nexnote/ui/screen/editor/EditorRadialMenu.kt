package com.example.nexnote.ui.screen.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.nexnote.ui.component.radial.RadialMenuEffect
import com.example.nexnote.ui.component.radial.RadialMenuFabHideEffect
import com.example.nexnote.ui.component.radial.RadialMenuItem
import com.example.nexnote.ui.component.radial.RadialMenuScrollEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun EditorRadialMenuBindings(
    isKeyboardVisible: Boolean,
    showPreview: Boolean,
    isTemplateMode: Boolean,
    contentScrollState: ScrollState,
    launchImagePickerAtCursor: () -> Unit,
    onInsertNoteLink: () -> Unit,
    onToggleColorPicker: () -> Unit,
    insertAtCursor: (String) -> Unit,
    togglePreviewPreservingScroll: () -> Unit,
    scope: CoroutineScope
) {
    RadialMenuFabHideEffect(hide = isKeyboardVisible)
    EditorRadialMenuScrollBindings(contentScrollState, scope)
    RadialMenuEffect(
        items = rememberEditorRadialMenuItems(
            launchImagePickerAtCursor,
            showPreview,
            isTemplateMode,
            onInsertNoteLink,
            onToggleColorPicker,
            insertAtCursor,
            togglePreviewPreservingScroll
        ),
        fabIcon = Icons.Default.Tune
    )
}

@Composable
private fun EditorRadialMenuScrollBindings(
    contentScrollState: ScrollState,
    scope: CoroutineScope
) {
    RadialMenuScrollEffect(
        onScrollToTop = { scope.launch { contentScrollState.animateScrollTo(0) } },
        onScrollToBottom = {
            scope.launch { contentScrollState.animateScrollTo(contentScrollState.maxValue) }
        }
    )
}

@Composable
private fun rememberEditorRadialMenuItems(
    launchImagePickerAtCursor: () -> Unit,
    showPreview: Boolean,
    isTemplateMode: Boolean,
    onInsertNoteLink: () -> Unit,
    onToggleColorPicker: () -> Unit,
    insertAtCursor: (String) -> Unit,
    togglePreviewPreservingScroll: () -> Unit
): List<RadialMenuItem> = remember(
    launchImagePickerAtCursor,
    showPreview,
    isTemplateMode,
    onInsertNoteLink,
    onToggleColorPicker,
    insertAtCursor,
    togglePreviewPreservingScroll
) {
    buildList {
        if (!isTemplateMode) {
            add(
                RadialMenuItem(
                    icon = Icons.Default.Image,
                    label = "",
                    action = launchImagePickerAtCursor,
                    contentDescription = "Insert image"
                )
            )
        }
        add(
            RadialMenuItem(
                icon = Icons.Default.CheckBox,
                label = "",
                action = { insertAtCursor(MARKDOWN_CHECKLIST_SNIPPET) },
                contentDescription = "Insert checklist"
            )
        )
        add(
            RadialMenuItem(
                icon = Icons.Default.Link,
                label = "",
                action = { insertAtCursor(MARKDOWN_WEB_LINK_SNIPPET) },
                contentDescription = "Insert web link"
            )
        )
        add(
            RadialMenuItem(
                icon = Icons.AutoMirrored.Filled.Note,
                label = "",
                action = onInsertNoteLink,
                contentDescription = "Insert note link"
            )
        )
        if (!isTemplateMode) {
            add(
                RadialMenuItem(
                    icon = Icons.Default.Palette,
                    label = "",
                    action = onToggleColorPicker,
                    contentDescription = "Note background color"
                )
            )
        }
        add(
            RadialMenuItem(
                icon = if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                label = "",
                action = togglePreviewPreservingScroll,
                contentDescription = if (showPreview) "Back to editing" else "Preview"
            )
        )
    }
}
