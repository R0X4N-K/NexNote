package com.example.nexnote.ui.screen.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.nexnote.ui.component.buildMarkdownBlockSourceRanges
import com.example.nexnote.util.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
