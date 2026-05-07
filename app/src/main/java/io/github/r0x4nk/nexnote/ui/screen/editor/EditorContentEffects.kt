package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Marks the content animations as enabled once the underlying note has emitted
 * its first persisted [EditorUiState.lastModifiedDate]. Holding off on the
 * AnimatedContent transition until the note is loaded prevents the entry
 * animation from playing over a still-empty editor on cold open.
 */
@Composable
internal fun EditorContentAnimationsReadyEffect(
    uiState: EditorUiState,
    state: EditorScreenState
) {
    LaunchedEffect(uiState.lastModifiedDate) {
        if (!state.contentAnimationsEnabled && uiState.lastModifiedDate != null) {
            state.contentAnimationsEnabled = true
        }
    }
}

/**
 * Pushes ViewModel content into the [TextFieldState] whenever the model emits a
 * new [EditorUiState.contentVersion] (e.g. on initial load, undo/redo, or
 * external content replacement).
 *
 * Re-keying on `noteId`, `templateId`, `isTemplateMode`, and `contentVersion`
 * keeps the editor immune to spurious recompositions: as long as the model has
 * not advanced its content version, the field state is left alone and typing
 * cannot be clobbered by a stale model snapshot.
 */
@Composable
internal fun EditorContentSyncEffect(
    uiState: EditorUiState,
    state: EditorScreenState
) {
    LaunchedEffect(
        uiState.noteId,
        uiState.templateId,
        uiState.isTemplateMode,
        uiState.contentVersion
    ) {
        val cursorPos = if (uiState.contentVersion <= 1) {
            0
        } else {
            uiState.contentSelectionOffset ?: uiState.content.length
        }.coerceIn(0, uiState.content.length)
        val syncedValue = TextFieldValue(
            text = uiState.content,
            selection = TextRange(cursorPos)
        )
        NexNoteDebugLog.editor(
            event = "contentSyncEffect",
            details = "cursor=$cursorPos noteId=${uiState.noteId} " +
                "version=${uiState.contentVersion} " +
                NexNoteDebugLog.textSummary("content", uiState.content)
        )
        state.setContentFieldValue(syncedValue)
        state.markContentCommitted()
        state.syncedContentVersion = uiState.contentVersion
    }
}

/**
 * Debounces user edits before flushing them into the ViewModel.
 *
 * Each keystroke bumps [EditorScreenState.contentEditRevision]; this effect
 * waits [CONTENT_MODEL_SYNC_DEBOUNCE_MS] of quiet, then commits the field's
 * current text and selection in one shot. This keeps every keystroke off the
 * persistence hot path while still committing fast enough that autosave,
 * search, and undo see fresh content within a fraction of a second.
 */
@Composable
internal fun EditorPendingContentCommitEffect(
    uiState: EditorUiState,
    state: EditorScreenState,
    viewModel: EditorViewModel
) {
    val revision = state.contentEditRevision
    LaunchedEffect(revision) {
        if (revision == 0 || !state.hasPendingContentCommit) return@LaunchedEffect

        delay(CONTENT_MODEL_SYNC_DEBOUNCE_MS)
        if (!state.hasPendingContentCommit || revision != state.contentEditRevision) {
            return@LaunchedEffect
        }

        state.commitContentTextFieldValue(
            modelContent = uiState.content,
            modelContentVersion = uiState.contentVersion,
            onContentChange = viewModel::onContentChange
        )
        if (state.noteSearch.isActive) {
            state.noteSearch = state.noteSearch.refresh(state.contentFieldValue.text)
        }
    }
}

/**
 * Restores the scroll position after toggling between edit and preview modes.
 *
 * In **preview mode** the anchor is mapped to a lazy item plus an intra-item
 * offset, so the preview keeps the same approximate viewport position without
 * eagerly composing every markdown block.
 *
 * In **edit mode** the anchor is mapped to a pixel Y via [TextLayoutResult]
 * and the [ScrollState] scrolls to that position.
 */
@Composable
internal fun EditorPreviewScrollRestorationEffect(
    uiState: EditorUiState,
    state: EditorScreenState,
    density: Density
) {
    LaunchedEffect(uiState.showPreview, state.pendingContentScrollAnchor) {
        val anchor = state.pendingContentScrollAnchor ?: return@LaunchedEffect
        // Wait two frames so the new content mode has been laid out.
        withFrameNanos { }
        withFrameNanos { }

        val anchorOffset = anchor.charOffset.coerceIn(0, state.contentFieldValue.text.length)

        if (uiState.showPreview) {
            restorePreviewScroll(state, anchorOffset)
        } else {
            restoreEditScroll(state, anchorOffset, anchor.viewportFraction, density)
        }

        delay(50)
        state.isRestoringContentScroll[0] = false
        state.pendingContentScrollAnchor = null
    }
}

/**
 * Scrolls the preview [LazyColumn] to the source position represented by [anchorOffset].
 * Gracefully no-ops when source ranges or items are not yet available.
 */
private suspend fun restorePreviewScroll(
    state: EditorScreenState,
    anchorOffset: Int
) {
    state.previewListState.scrollToSourceOffset(
        sourceRanges = state.currentSourceRanges,
        sourceOffset = anchorOffset,
        viewportFraction = CONTENT_SCROLL_ANCHOR_FRACTION,
        animated = false
    )
}

/**
 * Scrolls the edit [BasicTextField] so the line at [anchorOffset] sits at
 * the given viewport fraction.
 */
private suspend fun restoreEditScroll(
    state: EditorScreenState,
    anchorOffset: Int,
    viewportFraction: Float,
    density: Density
) {
    val viewportHeight = state.contentViewportHeightPx.coerceAtLeast(1)
    val anchorY = editAnchorY(state, anchorOffset, density)
    val targetScroll = if (anchorY != null) {
        (anchorY - viewportHeight * viewportFraction)
            .toInt()
            .coerceIn(0, state.contentScrollState.maxValue)
    } else {
        state.contentScrollState.value.coerceIn(0, state.contentScrollState.maxValue)
    }
    state.contentScrollState.scrollTo(targetScroll)
}

private suspend fun editAnchorY(
    state: EditorScreenState,
    anchorOffset: Int,
    density: Density
): Float? {
    val layout = withTimeoutOrNull(500) {
        snapshotFlow { state.textLayoutResult }
            .first { it != null && it.layoutInput.text.text == state.contentFieldValue.text }
    }
    val contentTopPadPx = with(density) { 8.dp.roundToPx() }
    return if (layout != null) {
        state.setContentFieldValue(
            state.contentFieldValue.copy(selection = TextRange(anchorOffset))
        )
        contentTopPadPx + layout.getCursorRect(anchorOffset).top
    } else {
        null
    }
}

/**
 * Hides the floating tag bar when the soft keyboard first appears so the user
 * has more vertical room to type. A small reverse scroll can reveal it again,
 * and closing the keyboard restores it if this effect hid it.
 */
@Composable
internal fun EditorKeyboardTagBarEffect(
    isKeyboardVisible: Boolean,
    state: EditorScreenState
) {
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            state.showColorPicker = false
            if (!state.tagsPinned && state.tagsVisible) {
                state.tagsVisible = false
                state.tagBarHiddenByKeyboard = true
            }
        } else if (state.tagBarHiddenByKeyboard) {
            state.tagBarHiddenByKeyboard = false
            state.tagsVisible = true
        }
    }
}

/**
 * Surfaces transient ViewModel errors as snackbars and clears them once shown,
 * so each error is reported exactly once per emission.
 */
@Composable
internal fun EditorErrorSnackbarEffect(
    uiState: EditorUiState,
    state: EditorScreenState,
    viewModel: EditorViewModel
) {
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            state.snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
}

/**
 * Requests initial focus when the editor opens.
 *
 * - Template-creation flow: focus the title so the user can name the template.
 * - Brand-new note: focus the content field so typing starts immediately.
 * - Existing note: keep the previous focus owner (no-op).
 */
@Composable
internal fun EditorInitialFocusEffect(
    noteId: Long,
    editTemplateId: Long,
    state: EditorScreenState
) {
    LaunchedEffect(noteId, editTemplateId) {
        when {
            editTemplateId != EditorViewModel.NO_ID -> {
                runCatching { state.titleFocusRequester.requestFocus() }
            }
            noteId == EditorViewModel.NO_ID -> {
                runCatching { state.contentFocusRequester.requestFocus() }
            }
        }
    }
}
