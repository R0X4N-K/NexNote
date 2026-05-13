package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuFabHideEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuScrollEffect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@Composable
internal fun EditorRadialMenuBindings(
    isKeyboardVisible: Boolean,
    showPreview: Boolean,
    isTemplateMode: Boolean,
    state: EditorScreenState,
    launchImagePickerAtCursor: () -> Unit,
    onInsertChecklist: () -> Unit,
    onInsertNoteLink: () -> Unit,
    onToggleColorPicker: () -> Unit,
    insertAtCursor: (String) -> Unit,
    scope: CoroutineScope
) {
    RadialMenuFabHideEffect(hide = isKeyboardVisible)
    EditorRadialMenuScrollBindings(showPreview, state, scope)
    RadialMenuEffect(
        items = rememberEditorRadialMenuItems(
            launchImagePickerAtCursor,
            isTemplateMode,
            onInsertChecklist,
            onInsertNoteLink,
            onToggleColorPicker,
            insertAtCursor
        ),
        fabIcon = Icons.Default.Tune
    )
}

@Composable
private fun EditorRadialMenuScrollBindings(
    showPreview: Boolean,
    state: EditorScreenState,
    scope: CoroutineScope
) {
    val scrollRunner = remember(scope) { EditorScrollShortcutRunner(scope) }

    DisposableEffect(scrollRunner) {
        onDispose { scrollRunner.cancel() }
    }

    RadialMenuScrollEffect(
        onScrollToTop = {
            scrollRunner.launch {
                if (showPreview) {
                    state.previewListState.animateScrollToPreviewTop()
                } else {
                    state.selectContentEdge(offset = 0)
                    state.contentScrollState.animateQuickScrollToTop()
                }
            }
        },
        onScrollToBottom = {
            scrollRunner.launch {
                if (showPreview) {
                    state.previewListState.animateScrollToPreviewBottom()
                } else {
                    state.selectContentEdge(offset = state.contentFieldValue.text.length)
                    state.contentScrollState.animateQuickScrollToBottom()
                }
            }
        }
    )
}

internal class EditorScrollShortcutRunner(
    private val scope: CoroutineScope
) {
    private var activeJob: Job? = null

    fun launch(block: suspend () -> Unit) {
        val previousJob = activeJob
        val nextJob = scope.launch(start = CoroutineStart.LAZY) {
            previousJob?.cancelAndJoin()
            block()
        }
        activeJob = nextJob
        nextJob.invokeOnCompletion {
            if (activeJob === nextJob) {
                activeJob = null
            }
        }
        nextJob.start()
    }

    fun cancel() {
        activeJob?.cancel()
        activeJob = null
    }
}

private fun EditorScreenState.selectContentEdge(offset: Int) {
    val current = currentContentTextFieldValue()
    val safeOffset = offset.coerceIn(0, current.text.length)
    setContentFieldValue(
        TextFieldValue(
            text = current.text,
            selection = TextRange(safeOffset)
        )
    )
}

@Composable
private fun rememberEditorRadialMenuItems(
    launchImagePickerAtCursor: () -> Unit,
    isTemplateMode: Boolean,
    onInsertChecklist: () -> Unit,
    onInsertNoteLink: () -> Unit,
    onToggleColorPicker: () -> Unit,
    insertAtCursor: (String) -> Unit
): List<RadialMenuItem> = remember(
    launchImagePickerAtCursor,
    isTemplateMode,
    onInsertChecklist,
    onInsertNoteLink,
    onToggleColorPicker,
    insertAtCursor
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
                action = onInsertChecklist,
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
    }
}
