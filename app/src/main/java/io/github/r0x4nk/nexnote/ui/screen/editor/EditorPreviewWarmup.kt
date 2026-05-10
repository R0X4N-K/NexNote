package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.r0x4nk.nexnote.ui.component.buildMarkdownBlockSourceRanges
import io.github.r0x4nk.nexnote.util.MarkdownColors
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
    val colors = rememberMarkdownColorsForWarmup()
    val warmupKey = uiState.directPreviewWarmupKey(colors.linkColor)

    LaunchedEffect(warmupKey, uiState.content) {
        if (warmupKey == null || !state.isDirectPreviewWarmupPending(warmupKey)) {
            return@LaunchedEffect
        }

        val startedAt = System.currentTimeMillis()
        warmUpMarkdownPreview(uiState.content, colors)
        waitForDirectPreviewRevealSlot(startedAt)

        if (state.isDirectPreviewWarmupPending(warmupKey)) {
            state.completedDirectPreviewWarmupKey = warmupKey
        }
    }
}

/**
 * Builds the same [MarkdownColors] bundle that `MarkdownPreview` uses on its
 * first composition, so the warmup populates the parser cache with the exact
 * key the preview will look up — otherwise the preview would miss the cache
 * and re-parse synchronously on its first frame.
 */
@Composable
private fun rememberMarkdownColorsForWarmup(): MarkdownColors {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val codeForeground = MaterialTheme.colorScheme.onSurfaceVariant
    return remember(linkColor, codeBackground, codeForeground) {
        MarkdownColors(
            linkColor            = linkColor,
            inlineCodeBackground = codeBackground,
            inlineCodeForeground = codeForeground
        )
    }
}

/**
 * Background pre-parse that keeps both preview caches warm while the user is
 * editing.
 *
 * The preview's first composition consults two caches:
 *  1. [MarkdownParser.getCached] for the parsed block list, and
 *  2. [buildMarkdownBlockSourceRanges] for the per-block source ranges used by
 *     scroll restoration and search highlighting.
 *
 * If either cache is cold when the user toggles to preview, the missing
 * computation runs synchronously inside Compose's `remember` block in
 * [EditorScreen] and stalls the main thread proportionally to the note size.
 * Warming both off-thread here keeps the preview toggle instant even for very
 * long notes.
 *
 * Debounces by [BACKGROUND_PREPARSE_DEBOUNCE_MS] to avoid excessive work during
 * rapid typing, and only runs in edit mode (not during preview or loading).
 */
@Composable
internal fun EditorBackgroundPreParseEffect(
    uiState: EditorUiState
) {
    val colors = rememberMarkdownColorsForWarmup()

    LaunchedEffect(uiState.content, uiState.contentVersion) {
        // Only pre-parse while in edit mode with non-trivial content
        if (uiState.isLoading || uiState.showPreview || uiState.content.length < PREPARSE_MIN_CHARS) {
            return@LaunchedEffect
        }

        delay(BACKGROUND_PREPARSE_DEBOUNCE_MS)
        withContext(Dispatchers.Default) {
            // Prime the parsed-blocks cache and the source-range cache in
            // parallel; the preview reads from both on its first frame.
            MarkdownParser.parseBlocks(text = uiState.content, colors = colors)
            buildMarkdownBlockSourceRanges(uiState.content)
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
    colors: MarkdownColors
) {
    withContext(Dispatchers.Default) {
        // Prime both preview caches off the main thread before first composition.
        MarkdownParser.parseBlocks(text = markdown, colors = colors)
        buildMarkdownBlockSourceRanges(markdown)
    }
}
