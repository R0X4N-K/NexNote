package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
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
    isKeyboardVisible: Boolean,
    showPreview: Boolean,
    isTemplateMode: Boolean,
    isDarkTheme: Boolean,
    state: EditorScreenState,
    onToggleColorPicker: () -> Unit,
    onThemeToggle: () -> Unit,
    onSearchOpen: () -> Unit,
    scope: CoroutineScope
) {
    RadialMenuFabHideEffect(hide = isKeyboardVisible || !showPreview)
    if (showPreview) {
        EditorRadialMenuScrollBindings(state, scope)
    }
    RadialMenuEffect(
        items = rememberEditorPreviewRadialMenuItems(
            showPreview,
            isTemplateMode,
            isDarkTheme,
            onToggleColorPicker,
            onThemeToggle,
            onSearchOpen
        ),
        fabIcon = if (showPreview) Icons.Default.Tune else null,
        fabContentDescription = if (showPreview) "Open preview tools" else null
    )
}

@Composable
private fun EditorRadialMenuScrollBindings(
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
                state.previewListState.animateScrollToPreviewTop()
            }
        },
        onScrollToBottom = {
            scrollRunner.launch {
                state.previewListState.animateScrollToPreviewBottom()
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

@Composable
private fun rememberEditorPreviewRadialMenuItems(
    showPreview: Boolean,
    isTemplateMode: Boolean,
    isDarkTheme: Boolean,
    onToggleColorPicker: () -> Unit,
    onThemeToggle: () -> Unit,
    onSearchOpen: () -> Unit
): List<RadialMenuItem> = remember(
    showPreview,
    isTemplateMode,
    isDarkTheme,
    onToggleColorPicker,
    onThemeToggle,
    onSearchOpen
) {
    if (!showPreview) return@remember emptyList()

    buildList {
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
                icon = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.DarkMode,
                label = "",
                action = onThemeToggle,
                contentDescription = if (isDarkTheme) {
                    "Switch to light theme"
                } else {
                    "Switch to dark theme"
                }
            )
        )
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
