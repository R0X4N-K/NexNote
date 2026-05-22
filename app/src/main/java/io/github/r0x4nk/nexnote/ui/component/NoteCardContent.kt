package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.theme.adaptNoteColor
import io.github.r0x4nk.nexnote.util.MarkdownColors

private data class NoteCardVisuals(
    val primaryColor: Color,
    val containerColor: Color,
    val cardElevation: Dp,
    val accentAlpha: Float
)

private data class NoteCardTextState(
    val title: AnnotatedString,
    val content: AnnotatedString
)

/**
 * Renders the visual body of [NoteCard] after swipe and collapse handling.
 *
 * The public card owns gestures and dismissal; this component owns the stable
 * card surface, note colors, markdown-aware compact text, search highlights,
 * and pin affordance. Keeping that split lets the note body be tested and
 * evolved without coupling it to Material swipe state.
 */
@Composable
internal fun NoteCardContent(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onLongPress: () -> Unit,
    showPinAction: Boolean,
    noteCardStyle: NoteCardStyle,
    titleHighlightRanges: List<IntRange>,
    contentHighlightRanges: List<IntRange>
) {
    val visuals = rememberNoteCardVisuals(note)
    val textState = rememberNoteCardTextState(
        note = note,
        titleHighlightRanges = titleHighlightRanges,
        contentHighlightRanges = contentHighlightRanges,
        primaryColor = visuals.primaryColor
    )

    NoteCardSurface(
        note = note,
        onClick = onClick,
        onPin = onPin,
        onLongPress = onLongPress,
        showPinAction = showPinAction,
        noteCardStyle = noteCardStyle,
        visuals = visuals,
        textState = textState
    )
}

@Composable
private fun rememberNoteCardVisuals(note: Note): NoteCardVisuals {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }

    return NoteCardVisuals(
        primaryColor = primaryColor,
        containerColor = note.backgroundColor?.let { adaptNoteColor(it, isDark) }
            ?: if (note.isPinned) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        cardElevation = if (note.isPinned) 2.dp else 0.dp,
        accentAlpha = if (note.isPinned) 0.95f else 0.34f
    )
}

@Composable
private fun rememberNoteCardTextState(
    note: Note,
    titleHighlightRanges: List<IntRange>,
    contentHighlightRanges: List<IntRange>,
    primaryColor: Color
): NoteCardTextState {
    val markdownColors = rememberNoteCardMarkdownColors(primaryColor)
    val displayTitle = remember(note.title, note.content) {
        note.title.ifBlank {
            note.content.lines().firstOrNull { it.isNotBlank() }?.take(80) ?: "Untitled note"
        }
    }
    val effectiveRanges = if (note.title.isNotBlank()) titleHighlightRanges else emptyList()
    val titleAnnotated = remember(displayTitle, effectiveRanges, markdownColors, note.isMarkdown) {
        buildNoteCardDisplayText(
            sourceText = displayTitle,
            ranges = effectiveRanges,
            colors = markdownColors,
            highlightColor = primaryColor,
            renderMarkdown = note.isMarkdown
        )
    }

    val previewText = remember(note.id, note.content) { note.content.take(160) }
    val clampedContentRanges = remember(contentHighlightRanges, previewText.length) {
        contentHighlightRanges.mapNotNull { range ->
            val safeStart = range.first.coerceIn(0, previewText.length)
            val safeEnd = (range.last + 1).coerceIn(safeStart, previewText.length)
            if (safeStart < safeEnd) safeStart..<safeEnd else null
        }
    }
    val contentAnnotated = remember(previewText, clampedContentRanges, markdownColors, note.isMarkdown) {
        buildNoteCardDisplayText(
            sourceText = previewText,
            ranges = clampedContentRanges,
            colors = markdownColors,
            highlightColor = primaryColor,
            renderMarkdown = note.isMarkdown
        )
    }

    return NoteCardTextState(title = titleAnnotated, content = contentAnnotated)
}

/**
 * Bundles the theme-aware colors used by inline Markdown rendering inside
 * note cards. Uses the same `surfaceContainerHigh` / `onSurfaceVariant` pair
 * as the full preview so that a note's compact card and its expanded preview
 * stay visually consistent.
 */
@Composable
private fun rememberNoteCardMarkdownColors(linkColor: Color): MarkdownColors {
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

@Composable
private fun NoteCardSurface(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onLongPress: () -> Unit,
    showPinAction: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState
) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .roundedCombinedClickableTarget(
                shape = shape,
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = visuals.cardElevation),
        colors = CardDefaults.cardColors(containerColor = visuals.containerColor),
        shape = shape,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
        )
    ) {
        NoteCardBody(
            note = note,
            onPin = onPin,
            showPinAction = showPinAction,
            noteCardStyle = noteCardStyle,
            visuals = visuals,
            textState = textState
        )
    }
}

@Composable
private fun NoteCardBody(
    note: Note,
    onPin: () -> Unit,
    showPinAction: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState
) {
    val verticalPadding = if (noteCardStyle == NoteCardStyle.TITLE_ONLY) 10.dp else 12.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        NoteCardAccentStrip(visuals.primaryColor, visuals.accentAlpha)
        NoteCardMainColumn(
            note = note,
            onPin = onPin,
            showPinAction = showPinAction,
            noteCardStyle = noteCardStyle,
            visuals = visuals,
            textState = textState,
            verticalPadding = verticalPadding,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NoteCardAccentStrip(primaryColor: Color, accentAlpha: Float) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(primaryColor.copy(alpha = accentAlpha))
    )
}

@Composable
private fun NoteCardMainColumn(
    note: Note,
    onPin: () -> Unit,
    showPinAction: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState,
    verticalPadding: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(
            start = 13.dp,
            end = 16.dp,
            top = verticalPadding,
            bottom = verticalPadding
        )
    ) {
        NoteCardTitleRow(
            title = textState.title,
            isPinned = note.isPinned,
            primaryColor = visuals.primaryColor,
            onPin = onPin,
            showPinAction = showPinAction
        )
        if (showsContentPreview(note, noteCardStyle)) {
            Spacer(Modifier.height(4.dp))
            NoteCardPreview(textState.content)
        }
        Spacer(Modifier.height(if (noteCardStyle == NoteCardStyle.TITLE_ONLY) 4.dp else 8.dp))
        NoteCardFooter(note, noteCardStyle, visuals.primaryColor)
    }
}
