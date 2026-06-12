package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
    showPreview: Boolean,
    isTemplateMode: Boolean,
    isReadOnly: Boolean,
    state: EditorScreenState,
    onToggleColorPicker: () -> Unit,
    onCreationDateEdit: () -> Unit,
    onSearchOpen: () -> Unit,
    scope: CoroutineScope
) {
    RadialMenuFabHideEffect(hide = false)
    if (showPreview || !isReadOnly) {
        EditorRadialMenuScrollBindings(
            showPreview = showPreview,
            state = state,
            shortcutAlpha = if (showPreview) {
                EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA
            } else {
                editorEditScrollShortcutAlpha(state)
            },
            scope = scope
        )
    }
    RadialMenuEffect(
        items = rememberEditorPreviewRadialMenuItems(
            showPreview,
            isTemplateMode,
            isReadOnly,
            onToggleColorPicker,
            onCreationDateEdit,
            onSearchOpen
        ),
        fabIcon = if (showPreview) Icons.Default.Tune else null,
        fabContentDescription = if (showPreview) "Open preview tools" else null
    )
}

@Composable
private fun EditorRadialMenuScrollBindings(
    showPreview: Boolean,
    state: EditorScreenState,
    shortcutAlpha: Float,
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
                    state.contentScrollState.animateQuickScrollToTop()
                }
            }
        },
        onScrollToBottom = {
            scrollRunner.launch {
                if (showPreview) {
                    state.previewListState.animateScrollToPreviewBottom()
                } else {
                    state.contentScrollState.animateQuickScrollToBottom()
                }
            }
        },
        shortcutAlpha = shortcutAlpha
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun editorEditScrollShortcutAlpha(state: EditorScreenState): Float {
    val layout = state.textLayoutResult ?: return EDITOR_SCROLL_SHORTCUT_NORMAL_ALPHA
    val cursorBoundsInViewport = editorCursorBoundsInViewportPx(
        layoutTextLength = layout.layoutInput.text.text.length,
        currentTextLength = state.contentTextFieldState.text.length,
        cursorOffset = state.contentTextFieldState.selection.end,
        scrollOffsetPx = state.contentScrollState.value,
        cursorBoundsProvider = { offset ->
            val rect = layout.getCursorRect(offset)
            EditorCursorVerticalBounds(
                topPx = rect.top,
                bottomPx = rect.bottom
            )
        }
    )

    return editorScrollShortcutAlpha(
        cursorBoundsInViewportPx = cursorBoundsInViewport,
        viewportHeightPx = state.unobscuredContentViewportHeightPx
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

@Composable
private fun rememberEditorPreviewRadialMenuItems(
    showPreview: Boolean,
    isTemplateMode: Boolean,
    isReadOnly: Boolean,
    onToggleColorPicker: () -> Unit,
    onCreationDateEdit: () -> Unit,
    onSearchOpen: () -> Unit
): List<RadialMenuItem> = remember(
    showPreview,
    isTemplateMode,
    isReadOnly,
    onToggleColorPicker,
    onCreationDateEdit,
    onSearchOpen
) {
    if (!showPreview) return@remember emptyList()

    buildList {
        if (!isTemplateMode && !isReadOnly) {
            add(
                RadialMenuItem(
                    icon = Icons.Default.Palette,
                    label = "",
                    action = onToggleColorPicker,
                    contentDescription = "Note background color"
                )
            )
            add(
                RadialMenuItem(
                    icon = Icons.Default.CalendarToday,
                    label = "",
                    action = onCreationDateEdit,
                    contentDescription = "Edit creation date"
                )
            )
        }
        add(
            RadialMenuItem(
                icon = Icons.Default.Search,
                label = "",
                action = onSearchOpen,
                contentDescription = "Search in note"
            )
        )
    }
}
