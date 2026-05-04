package com.example.nexnote.ui.screen.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

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
        state.syncedContentVersion = uiState.contentVersion
    }
}

@Composable
internal fun EditorPreviewScrollRestorationEffect(
    uiState: EditorUiState,
    state: EditorScreenState,
    density: Density
) {
    LaunchedEffect(uiState.showPreview, state.pendingContentScrollAnchor) {
        val anchor = state.pendingContentScrollAnchor ?: return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }

        val viewportHeight = state.contentViewportHeightPx.coerceAtLeast(1)
        val anchorOffset = anchor.charOffset.coerceIn(0, state.contentFieldValue.text.length)
        val anchorY = restoredAnchorY(state, uiState.showPreview, anchorOffset, density)
        val targetScroll = if (anchorY != null) {
            (anchorY - viewportHeight * anchor.viewportFraction)
                .toInt()
                .coerceIn(0, state.contentScrollState.maxValue)
        } else {
            state.contentScrollState.value.coerceIn(0, state.contentScrollState.maxValue)
        }

        state.contentScrollState.scrollTo(targetScroll)
        delay(50)
        state.isRestoringContentScroll[0] = false
        state.pendingContentScrollAnchor = null
    }
}

private suspend fun restoredAnchorY(
    state: EditorScreenState,
    showPreview: Boolean,
    anchorOffset: Int,
    density: Density
): Float? {
    return if (showPreview) {
        val layouts = withTimeoutOrNull(500) {
            snapshotFlow { state.previewSourceLayouts }.first { it.isNotEmpty() }
        }.orEmpty()
        layouts.previewYForSourceOffset(anchorOffset)
    } else {
        val layout = withTimeoutOrNull(500) {
            snapshotFlow { state.textLayoutResult }
                .first { it != null && it.layoutInput.text.text == state.contentFieldValue.text }
        }
        val contentTopPadPx = with(density) { 8.dp.roundToPx() }
        if (layout != null) {
            state.setContentFieldValue(
                state.contentFieldValue.copy(selection = TextRange(anchorOffset))
            )
            contentTopPadPx + layout.getCursorRect(anchorOffset).top
        } else {
            null
        }
    }
}

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
