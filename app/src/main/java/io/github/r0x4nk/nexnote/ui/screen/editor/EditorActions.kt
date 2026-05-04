package io.github.r0x4nk.nexnote.ui.screen.editor

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog

@Composable
internal fun rememberTogglePreviewPreservingScroll(
    state: EditorScreenState,
    showPreview: Boolean,
    content: String,
    contentVersion: Int,
    viewModel: EditorViewModel
): () -> Unit {
    val density = LocalDensity.current
    val showPreviewRef = rememberUpdatedState(showPreview)
    val contentRef = rememberUpdatedState(content)
    val contentVersionRef = rememberUpdatedState(contentVersion)
    val previewSourceLayoutsRef = rememberUpdatedState(state.previewSourceLayouts)
    val textLayoutResultRef = rememberUpdatedState(state.textLayoutResult)
    val contentViewportHeightPxRef = rememberUpdatedState(state.contentViewportHeightPx)

    return remember(state, density, viewModel) {
        {
            NexNoteDebugLog.editor(
                event = "togglePreviewClicked",
                details = "showPreview=${showPreviewRef.value} " +
                    "field=${state.currentContentTextFieldValue().text.length} " +
                    "modelVersion=${contentVersionRef.value} " +
                    NexNoteDebugLog.textSummary("modelContent", contentRef.value)
            )
            if (!showPreviewRef.value) {
                state.commitContentTextFieldValue(
                    modelContent = contentRef.value,
                    modelContentVersion = contentVersionRef.value,
                    onContentChange = viewModel::onContentChange
                )
            }
            val currentContent = state.currentContentTextFieldValue()
            val viewportHeight = contentViewportHeightPxRef.value.coerceAtLeast(1)
            val centerY = state.contentScrollState.value +
                viewportHeight * CONTENT_SCROLL_ANCHOR_FRACTION
            val anchorOffset = if (showPreviewRef.value) {
                previewSourceLayoutsRef.value.sourceOffsetForPreviewY(centerY)
                    ?: currentContent.selection.end.coerceIn(0, currentContent.text.length)
            } else {
                editModeAnchorOffset(state, currentContent, centerY, textLayoutResultRef.value, density)
            }
            state.pendingContentScrollAnchor = ContentScrollAnchor(charOffset = anchorOffset)
            state.isRestoringContentScroll[0] = true
            NexNoteDebugLog.editor(
                event = "togglePreviewAnchorResolved",
                details = "anchorOffset=$anchorOffset currentFieldLen=${currentContent.text.length}"
            )
            viewModel.togglePreview()
        }
    }
}

private fun editModeAnchorOffset(
    state: EditorScreenState,
    currentContent: TextFieldValue,
    centerY: Float,
    layout: androidx.compose.ui.text.TextLayoutResult?,
    density: androidx.compose.ui.unit.Density
): Int {
    return if (layout != null && layout.layoutInput.text.text == currentContent.text) {
        val contentTopPadPx = with(density) { 8.dp.roundToPx() }
        val textY = (centerY - contentTopPadPx).coerceAtLeast(0f)
        layout.getOffsetForPosition(Offset(x = 0f, y = textY))
    } else {
        currentContent.selection.end
    }.coerceIn(0, state.contentFieldValue.text.length)
}

@Composable
internal fun rememberInsertAtCursor(
    state: EditorScreenState,
    viewModel: EditorViewModel
): (String) -> Unit {
    return remember(state.contentFieldValueState, viewModel) {
        { insertion ->
            val current = state.contentFieldValue
            val cursor = current.selection.end
            val before = current.text.substring(0, cursor)
            val after = current.text.substring(cursor)
            val text = insertion.withoutSpuriousLeadingNewline(current, cursor)
            val newText = before + text + after
            val nextCursor = cursor + text.length
            NexNoteDebugLog.editor(
                event = "insertAtCursor",
                details = "cursor=$cursor nextCursor=$nextCursor " +
                    "${NexNoteDebugLog.textSummary("insertion", insertion)} " +
                    NexNoteDebugLog.textSummary("newText", newText)
            )
            state.setContentFieldValue(TextFieldValue(newText, TextRange(nextCursor)))
            viewModel.onContentChange(newText, nextCursor)
        }
    }
}

@Composable
internal fun rememberReplaceNoteLinkAutocomplete(
    state: EditorScreenState,
    viewModel: EditorViewModel
): (NoteLinkAutocompleteMatch, NoteLinkTarget) -> Unit {
    return remember(state.contentFieldValueState, viewModel) {
        { match, target ->
            val current = state.contentFieldValue
            val start = match.start.coerceIn(0, current.text.length)
            val end = match.endExclusive.coerceIn(start, current.text.length)
            val insertion = noteLinkMarkdownFor(target)
            val newText = current.text.substring(0, start) + insertion + current.text.substring(end)
            val nextCursor = start + insertion.length
            NexNoteDebugLog.editor(
                event = "replaceNoteLinkAutocomplete",
                details = "start=$start end=$end targetId=${target.id} nextCursor=$nextCursor " +
                    NexNoteDebugLog.textSummary("newText", newText)
            )
            state.setContentFieldValue(TextFieldValue(newText, TextRange(nextCursor)))
            viewModel.onContentChange(newText, nextCursor)
        }
    }
}

private fun String.withoutSpuriousLeadingNewline(
    current: TextFieldValue,
    cursor: Int
): String {
    return if (startsWith("\n") && (cursor == 0 || current.text.getOrNull(cursor - 1) == '\n')) {
        drop(1)
    } else {
        this
    }
}

@Composable
internal fun rememberLaunchImagePickerAtCursor(
    context: Context,
    state: EditorScreenState,
    viewModel: EditorViewModel
): () -> Unit {
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        handlePickedImage(uri, context, state, viewModel)
    }

    return remember(state.contentFieldValueState, imageLauncher, state.pendingImageInsertionOffsetState) {
        {
            val current = state.contentFieldValueState.value
            state.pendingImageInsertionOffsetState.value =
                current.selection.end.coerceIn(0, current.text.length)
            imageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}

private fun handlePickedImage(
    uri: Uri?,
    context: Context,
    state: EditorScreenState,
    viewModel: EditorViewModel
) {
    val insertionOffset = state.pendingImageInsertionOffsetState.value
        ?: state.contentFieldValueState.value.selection.end
    state.pendingImageInsertionOffsetState.value = null
    uri?.let {
        val resolver = context.applicationContext.contentResolver
        viewModel.onImagePicked(
            openImageInputStream = { resolver.openInputStream(it) },
            insertionOffset = insertionOffset
        )
    }
}
