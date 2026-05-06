package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.markdownListContinuationForLine
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

@OptIn(ExperimentalFoundationApi::class)
private class TextHighlightOutputTransformation(
    private val searchRanges: List<IntRange>,
    private val activeSearchRange: IntRange?,
    private val fallbackRange: IntRange?,
    private val searchColor: Color,
    private val activeSearchColor: Color,
    private val fallbackColor: Color
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        searchRanges.forEach { range ->
            addHighlightStyle(range, length, searchColor)
        }
        activeSearchRange?.let { range ->
            addHighlightStyle(range, length, activeSearchColor)
        }
        if (searchRanges.isEmpty() && activeSearchRange == null) {
            fallbackRange?.let { range ->
                addHighlightStyle(range, length, fallbackColor)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun TextFieldBuffer.addHighlightStyle(
    range: IntRange,
    textLength: Int,
    color: Color
) {
    val safeStart = range.first.coerceIn(0, textLength)
    val safeEnd = (range.last + 1).coerceIn(safeStart, textLength)
    if (safeEnd > safeStart) {
        addStyle(SpanStyle(background = color), safeStart, safeEnd)
    }
}

@Composable
internal fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    placeholder: String = "Title",
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
                inner()
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContentField(
    textFieldState: TextFieldState,
    scrollState: ScrollState,
    onContentEdited: () -> Unit,
    onSelectionChange: (TextRange) -> Unit = {},
    onLayoutResult: (TextLayoutResult) -> Unit = {},
    highlightRange: IntRange? = null,
    searchRanges: List<IntRange> = emptyList(),
    activeSearchRange: IntRange? = null,
    modifier: Modifier = Modifier
) {
    val fallbackHighlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    val searchHighlightColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    val activeSearchHighlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    val contentEditEvents = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }
    val inputTransformation = remember(contentEditEvents) {
        EditorContentInputTransformation {
            contentEditEvents.tryEmit(Unit)
        }
    }
    val outputTransformation = remember(
        highlightRange,
        searchRanges,
        activeSearchRange,
        fallbackHighlightColor,
        searchHighlightColor,
        activeSearchHighlightColor
    ) {
        if (highlightRange != null || searchRanges.isNotEmpty() || activeSearchRange != null) {
            TextHighlightOutputTransformation(
                searchRanges = searchRanges,
                activeSearchRange = activeSearchRange,
                fallbackRange = highlightRange,
                searchColor = searchHighlightColor,
                activeSearchColor = activeSearchHighlightColor,
                fallbackColor = fallbackHighlightColor
            )
        } else {
            null
        }
    }

    LaunchedEffect(textFieldState, contentEditEvents) {
        contentEditEvents.conflate().collect {
            yield()
            onContentEdited()
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.selection }
            .distinctUntilChanged()
            .collect { onSelectionChange(it) }
    }

    BasicTextField(
        state = textFieldState,
        modifier = modifier,
        inputTransformation = inputTransformation,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        outputTransformation = outputTransformation,
        scrollState = scrollState,
        onTextLayout = { getResult ->
            getResult()?.let(onLayoutResult)
        },
        decorator = TextFieldDecorator { inner ->
            ContentFieldDecoration(textFieldState.text.isEmpty(), inner)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
private class EditorContentInputTransformation(
    private val onContentEdited: () -> Unit
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return

        if (length > MAX_CONTENT_LENGTH) {
            revertAllChanges()
            return
        }

        continueMarkdownListIfNeeded()

        if (length > MAX_CONTENT_LENGTH) {
            revertAllChanges()
            return
        }

        if (changes.changeCount > 0) {
            onContentEdited()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun TextFieldBuffer.continueMarkdownListIfNeeded() {
    if (changes.changeCount != 1) return

    val range = changes.getRange(0)
    val originalRange = changes.getOriginalRange(0)
    if (!originalRange.collapsed || range.max - range.min != 1) return

    val insertedAt = range.min
    if (insertedAt !in 0 until length || charAt(insertedAt) != '\n') return

    val cursorPos = insertedAt + 1
    if (!selection.collapsed || selection.end != cursorPos) return

    val insertion = markdownListContinuationForLine(lineBefore(insertedAt)) ?: return
    insert(cursorPos, insertion)
    selection = TextRange(cursorPos + insertion.length)
}

@OptIn(ExperimentalFoundationApi::class)
private fun TextFieldBuffer.lineBefore(offset: Int): String {
    val end = offset.coerceIn(0, length)
    var start = end
    while (start > 0 && charAt(start - 1) != '\n') {
        start--
    }

    return buildString(end - start) {
        for (index in start until end) {
            append(charAt(index))
        }
    }
}

/**
 * State-based text fields emit their current buffer as soon as observation
 * starts. When the editor attaches to an already loaded note, that first buffer
 * can still be the empty construction value. Drop only that stale empty value;
 * every real user change, including an intentional later clear, must flow
 * through to the ViewModel and persistence layer.
 */
internal fun Flow<TextFieldValue>.userTextFieldChanges(
    expectedText: () -> String
): Flow<TextFieldValue> {
    return flow {
        var isFirstSnapshot = true
        collect { value ->
            val expected = expectedText()
            val isStaleInitialSnapshot =
                isFirstSnapshot && value.text.isEmpty() && expected.isNotEmpty()
            NexNoteDebugLog.editor(
                event = "textFieldSnapshot",
                details = "first=$isFirstSnapshot staleInitial=$isStaleInitialSnapshot " +
                    "${NexNoteDebugLog.textSummary("value", value.text)} " +
                    NexNoteDebugLog.textSummary("expected", expected)
            )
            isFirstSnapshot = false
            if (!isStaleInitialSnapshot) emit(value)
        }
    }.distinctUntilChanged()
}

@Composable
private fun ContentFieldDecoration(
    isEmpty: Boolean,
    inner: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.TopStart) {
        if (isEmpty) {
            Text(
                text = "Write something…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            )
        }
        inner()
    }
}
