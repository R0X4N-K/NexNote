package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import io.github.r0x4nk.nexnote.ui.component.buildMarkdownBlockSourceRanges
import io.github.r0x4nk.nexnote.util.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Handles the warmup phase when a note is opened directly in preview mode.
 * Parses markdown blocks off-thread and populates the single-slot cache before
 * the loading placeholder is dismissed, ensuring instant content display.
 */
@Composable
internal fun EditorDirectPreviewWarmupEffect(
    uiState: EditorUiState,
    state: EditorScreenState
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val warmupKey = uiState.directPreviewWarmupKey(linkColor)

    LaunchedEffect(warmupKey, uiState.content) {
        if (warmupKey == null || !state.isDirectPreviewWarmupPending(warmupKey)) {
            return@LaunchedEffect
        }

        val startedAt = System.currentTimeMillis()
        warmUpMarkdownPreview(uiState.content, linkColor)
        waitForDirectPreviewRevealSlot(startedAt)

        if (state.isDirectPreviewWarmupPending(warmupKey)) {
            state.completedDirectPreviewWarmupKey = warmupKey
        }
    }
}

/**
 * Background pre-parse that keeps the single-slot block cache warm while the
 * user is editing. When the user eventually toggles to preview, the parsed
 * blocks are already available via [MarkdownParser.getCached], so the preview
 * composable can render on its very first frame — no blank gap.
 *
 * Debounces by [BACKGROUND_PREPARSE_DEBOUNCE_MS] to avoid excessive work during
 * rapid typing. Only runs in edit mode (not during preview or loading).
 */
@Composable
internal fun EditorBackgroundPreParseEffect(
    uiState: EditorUiState
) {
    val linkColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(uiState.content, uiState.contentVersion) {
        // Only pre-parse while in edit mode with non-trivial content
        if (uiState.isLoading || uiState.showPreview || uiState.content.length < PREPARSE_MIN_CHARS) {
            return@LaunchedEffect
        }

        delay(BACKGROUND_PREPARSE_DEBOUNCE_MS)
        withContext(Dispatchers.Default) {
            MarkdownParser.parseBlocks(text = uiState.content, linkColor = linkColor)
        }
    }
}

internal fun EditorUiState.directPreviewWarmupKey(linkColor: Color): DirectPreviewWarmupKey? {
    if (isLoading || !showPreview || !openedDirectlyInPreview || noteId == EditorViewModel.NO_ID) {
        return null
    }

    return DirectPreviewWarmupKey(
        noteId = noteId,
        contentVersion = contentVersion,
        contentHash = content.hashCode(),
        linkColorValue = linkColor.value
    )
}

internal fun EditorScreenState.isDirectPreviewWarmupPending(
    key: DirectPreviewWarmupKey?
): Boolean = key != null && completedDirectPreviewWarmupKey != key

private suspend fun waitForDirectPreviewRevealSlot(startedAt: Long) {
    val elapsedMs = System.currentTimeMillis() - startedAt
    val remainingMs = DIRECT_PREVIEW_FIRST_COMPOSITION_DELAY_MS - elapsedMs
    if (remainingMs > 0) delay(remainingMs)
}

private suspend fun warmUpMarkdownPreview(
    markdown: String,
    linkColor: Color
) {
    withContext(Dispatchers.Default) {
        // Prime both preview caches off the main thread before first composition.
        MarkdownParser.parseBlocks(text = markdown, linkColor = linkColor)
        buildMarkdownBlockSourceRanges(markdown)
    }
}
