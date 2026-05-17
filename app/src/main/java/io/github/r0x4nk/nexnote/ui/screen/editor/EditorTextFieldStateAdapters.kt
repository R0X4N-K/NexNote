package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog

@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldState.toTextFieldValue(): TextFieldValue {
    return TextFieldValue(
        text = text.toString(),
        selection = selection.coerceInText(text.length)
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal fun TextFieldState.setTextFieldValue(value: TextFieldValue) {
    val safeSelection = value.selection.coerceInText(value.text.length)
    val currentText = text
    val hasSameText = currentText.hasSameTextAs(value.text)
    if (hasSameText && selection == safeSelection) return

    edit {
        if (!hasSameText) {
            replace(0, length, value.text)
        }
        selection = safeSelection
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun EditorScreenState.setContentFieldValue(value: TextFieldValue) {
    contentFieldValue = value
    contentTextFieldState.setTextFieldValue(value)
}

@OptIn(ExperimentalFoundationApi::class)
internal fun EditorScreenState.currentContentTextFieldValue(): TextFieldValue {
    return contentTextFieldState.toTextFieldValue()
}

internal fun EditorScreenState.commitContentTextFieldValue(
    modelContent: String,
    modelContentVersion: Int,
    onContentChange: (String, Int?) -> Unit,
    redactContent: Boolean = false
): Boolean {
    val committedValue = EditorContentCommitPolicy.resolve(
        EditorContentCommitInput(
            fieldValue = currentContentTextFieldValue(),
            rememberedValue = contentFieldValue,
            modelContent = modelContent,
            modelContentVersion = modelContentVersion,
            syncedContentVersion = syncedContentVersion,
            hasPendingFieldEdit = hasPendingContentCommit
        )
    )
    NexNoteDebugLog.editor(
        event = "commitContentTextFieldValueResolved",
        details = "resolved=${committedValue?.text?.let { NexNoteDebugLog.textSummary("text", it, redact = redactContent) } ?: "null"} " +
            "modelVersion=$modelContentVersion syncedVersion=$syncedContentVersion " +
            "pendingEdit=$hasPendingContentCommit " +
            NexNoteDebugLog.textSummary("model", modelContent, redact = redactContent)
    )
    committedValue ?: return false

    setContentFieldValue(committedValue)
    onContentChange(
        committedValue.text,
        committedValue.selection.end.coerceIn(0, committedValue.text.length)
    )
    markContentCommitted()
    return true
}

private fun TextRange.coerceInText(textLength: Int): TextRange {
    return TextRange(
        start = start.coerceIn(0, textLength),
        end = end.coerceIn(0, textLength)
    )
}

private fun CharSequence.hasSameTextAs(other: String): Boolean {
    return this === other || (length == other.length && contentEquals(other))
}
