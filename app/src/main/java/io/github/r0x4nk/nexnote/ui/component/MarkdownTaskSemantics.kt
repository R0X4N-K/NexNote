package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.util.MARKDOWN_TASK_LIST_ANNOTATION_TAG
import kotlin.math.roundToInt

/**
 * Exposes each task as a checkbox without splitting the styled text into lines.
 * Semantic bounds follow the rendered marker, including wrapping and nested lists.
 * Pointer events still use the shared annotation handler so embedded links keep priority.
 */
@Composable
internal fun MarkdownTaskText(
    text: AnnotatedString,
    style: TextStyle,
    openUri: (String) -> Unit,
    onNoteLinkClick: (Long) -> Unit,
    onTaskListItemClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val tasks = remember(text) {
        text.getStringAnnotations(MARKDOWN_TASK_LIST_ANNOTATION_TAG, 0, text.length)
    }
    Box(modifier) {
        BasicText(
            text = text,
            style = style,
            onTextLayout = { layout = it },
            modifier = Modifier.markdownAnnotationTapHandler(
                displayText = text,
                getLayoutResult = { layout },
                openUri = openUri,
                onNoteLinkClick = onNoteLinkClick,
                onTaskListItemClick = onTaskListItemClick
            )
        )
        val textLayout = layout ?: return@Box
        val density = LocalDensity.current
        tasks.forEach { annotation ->
            val lineIndex = annotation.item.toIntOrNull() ?: return@forEach
            val renderedLine = text.text.substring(annotation.start, annotation.end)
            val markerIndex = renderedLine.indexOfFirst { it == '☐' || it == '☑' }
            if (markerIndex < 0) return@forEach
            val checked = renderedLine[markerIndex] == '☑'
            val label = renderedLine.substring(markerIndex + 1).trim().ifEmpty {
                stringResource(R.string.task_unnamed)
            }
            val description = stringResource(
                if (checked) R.string.task_completed else R.string.task_not_completed
            )
            val actionLabel = stringResource(
                if (checked) R.string.task_mark_not_completed else R.string.task_mark_completed
            )
            val bounds = textLayout.getBoundingBox(annotation.start + markerIndex)
            Box(
                Modifier
                    .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                    .size(
                        with(density) { bounds.width.toDp() },
                        with(density) { bounds.height.toDp() }
                    )
                    .semantics {
                        contentDescription = label
                        role = Role.Checkbox
                        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                        stateDescription = description
                        if (onTaskListItemClick == null) {
                            disabled()
                        } else {
                            onClick(actionLabel) {
                                onTaskListItemClick(lineIndex)
                                true
                            }
                        }
                    }
            )
        }
    }
}
