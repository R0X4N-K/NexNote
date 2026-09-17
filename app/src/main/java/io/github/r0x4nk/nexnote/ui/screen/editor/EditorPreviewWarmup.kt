package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import io.github.r0x4nk.nexnote.ui.component.buildMarkdownBlockSourceRanges
import io.github.r0x4nk.nexnote.ui.theme.rememberContentMarkdownColors
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
    val colors = rememberContentMarkdownColors()
    val warmupKey = uiState.directPreviewWarmupKey(colors.linkColor)

    LaunchedEffect(warmupKey, uiState.content, colors) {
        if (warmupKey == null || !state.isDirectPreviewWarmupPending(warmupKey)) {
            return@LaunchedEffect
        }

        val startedAt = System.currentTimeMillis()
        warmUpMarkdownPreview(uiState.content, colors)
        waitForDirectPreviewRevealSlot(startedAt)

        if (state.isDirectPreviewWarmupPending(warmupKey)) {
            state.completedDirectPreviewWarmupKey = warmupKey
            state.hasCompletedDirectPreviewReveal = true
        }
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
    val colors = rememberContentMarkdownColors()

    LaunchedEffect(uiState.content, uiState.contentVersion, colors) {
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

/**
 * Whether the loading placeholder should still cover a note opened directly in
 * preview.
 *
 * Once the first warmup has revealed the preview, later content or theme changes
 * re-key the warmup cache but must not bring the skeleton back.
 */
internal fun EditorScreenState.shouldShowDirectPreviewLoading(
    key: DirectPreviewWarmupKey?
): Boolean = shouldShowDirectPreviewLoading(
    hasCompletedReveal = hasCompletedDirectPreviewReveal,
    isWarmupPending = isDirectPreviewWarmupPending(key)
)

internal fun shouldShowDirectPreviewLoading(
    hasCompletedReveal: Boolean,
    isWarmupPending: Boolean
): Boolean = !hasCompletedReveal && isWarmupPending

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
