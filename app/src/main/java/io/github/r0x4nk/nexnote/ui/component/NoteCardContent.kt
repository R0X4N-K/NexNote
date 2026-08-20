package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
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
    val cardElevation: Dp
)

private data class NoteCardTextState(
    val title: AnnotatedString,
    val content: AnnotatedString
)

/**
 * Renders the visual body of [NoteCard] after swipe and collapse handling.
 *
 * The public card owns gestures and dismissal; this component owns the stable
 * card surface, note colors, markdown-aware compact text, and search highlights.
 * Keeping that split lets the note body evolve without coupling it to Material
 * swipe state.
 */
@Composable
internal fun NoteCardContent(
    note: Note,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onActions: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    titleHighlightRanges: List<IntRange>,
    contentHighlightRanges: List<IntRange>
) {
    val visuals = rememberNoteCardVisuals(note, selected)
    val textState = rememberNoteCardTextState(
        note = note,
        titleHighlightRanges = titleHighlightRanges,
        contentHighlightRanges = contentHighlightRanges,
        primaryColor = visuals.primaryColor
    )

    NoteCardSurface(
        note = note,
        onClick = onClick,
        onLongPress = onLongPress,
        onActions = onActions,
        selectionMode = selectionMode,
        selected = selected,
        noteCardStyle = noteCardStyle,
        visuals = visuals,
        textState = textState
    )
}

@Composable
private fun rememberNoteCardVisuals(note: Note, selected: Boolean): NoteCardVisuals {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
    val isDark = remember(surfaceColor) { surfaceColor.luminance() < 0.5f }
    val baseContainerColor = note.backgroundColor?.let { adaptNoteColor(it, isDark) }
        ?: NoteCollectionCardDefaults.containerColor()

    return NoteCardVisuals(
        primaryColor = primaryColor,
        containerColor = if (selected) {
            lerp(baseContainerColor, selectedContainerColor, 0.46f)
        } else {
            baseContainerColor
        },
        cardElevation = if (selected || note.isPinned) {
            NoteCollectionCardDefaults.pinnedElevation
        } else {
            NoteCollectionCardDefaults.defaultElevation
        }
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
    onLongPress: () -> Unit,
    onActions: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState
) {
    val shape = NoteCollectionCardDefaults.shape
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
        border = when {
            selected -> NoteCollectionCardDefaults.border(
                color = visuals.primaryColor,
                alpha = 1f
            )
            note.isPinned -> NoteCollectionCardDefaults.border(
                color = visuals.primaryColor,
                alpha = 0.34f
            )
            else -> NoteCollectionCardDefaults.border(alpha = 0.42f)
        }
    ) {
        NoteCardBody(
            note = note,
            onActions = onActions,
            selectionMode = selectionMode,
            selected = selected,
            noteCardStyle = noteCardStyle,
            visuals = visuals,
            textState = textState
        )
    }
}

@Composable
private fun NoteCardBody(
    note: Note,
    onActions: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState
) {
    val verticalPadding = if (noteCardStyle == NoteCardStyle.TITLE_ONLY) 10.dp else 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = verticalPadding
            )
    ) {
        NoteCardMainColumn(
            note = note,
            onActions = onActions,
            selectionMode = selectionMode,
            selected = selected,
            noteCardStyle = noteCardStyle,
            visuals = visuals,
            textState = textState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoteCardMainColumn(
    note: Note,
    onActions: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        NoteCardTitleRow(
            title = textState.title,
            isPinned = note.isPinned,
            onActions = onActions,
            selectionMode = selectionMode,
            selected = selected
        )
        if (showsContentPreview(note, noteCardStyle)) {
            Spacer(Modifier.height(4.dp))
            NoteCardPreview(textState.content)
        }
        Spacer(Modifier.height(if (noteCardStyle == NoteCardStyle.TITLE_ONLY) 4.dp else 8.dp))
        NoteCardFooter(note, noteCardStyle, visuals.primaryColor)
    }
}
