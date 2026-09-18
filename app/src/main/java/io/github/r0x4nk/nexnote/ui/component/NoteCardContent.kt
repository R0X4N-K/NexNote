package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.NoteMotion
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.theme.NoteContentTheme
import io.github.r0x4nk.nexnote.ui.theme.rememberContentMarkdownColors
import io.github.r0x4nk.nexnote.ui.theme.rememberNoteColors

private data class NoteCardVisuals(
    val primaryColor: Color,
    val containerColor: Color
)

private data class NoteCardTextState(
    val title: AnnotatedString,
    val preview: AnnotatedString?
)

private const val NOTE_CARD_TITLE_MAX_LENGTH = 160
private const val NOTE_CARD_PREVIEW_MAX_LENGTH = 160

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
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    titleHighlightRanges: List<IntRange>,
    contentHighlightRanges: List<IntRange>
) {
    val noteColors = rememberNoteColors(note.backgroundColor)
    NoteContentTheme(noteColors) {
        val visuals = rememberNoteCardVisuals(noteColors.container)
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
            selectionMode = selectionMode,
            selected = selected,
            noteCardStyle = noteCardStyle,
            visuals = visuals,
            textState = textState
        )
    }
}

@Composable
private fun rememberNoteCardVisuals(container: Color): NoteCardVisuals =
    NoteCardVisuals(
        primaryColor = MaterialTheme.colorScheme.primary,
        containerColor = container
    )

@Composable
private fun rememberNoteCardTextState(
    note: Note,
    titleHighlightRanges: List<IntRange>,
    contentHighlightRanges: List<IntRange>,
    primaryColor: Color
): NoteCardTextState {
    val markdownColors = rememberContentMarkdownColors()
    val untitledLabel = stringResource(R.string.untitled_note)
    val imagePlaceholder = stringResource(R.string.markdown_image_alt_fallback)
    val hasTitle = note.title.isNotBlank()
    // A note without a title keeps its whole body in the preview: the first
    // content line must never be promoted to the title (and rendered bold).
    val displayTitle = remember(note.title, hasTitle, untitledLabel) {
        if (hasTitle) note.title.take(NOTE_CARD_TITLE_MAX_LENGTH) else untitledLabel
    }
    val effectiveTitleRanges = if (hasTitle) titleHighlightRanges else emptyList()
    // Match the Markdown-only editor, including notes imported with the legacy false flag.
    val titleAnnotated = remember(displayTitle, effectiveTitleRanges, markdownColors, imagePlaceholder) {
        buildNoteCardDisplayText(
            sourceText = displayTitle,
            ranges = effectiveTitleRanges,
            colors = markdownColors,
            highlightColor = primaryColor,
            renderMarkdown = true,
            imagePlaceholder = imagePlaceholder
        )
    }

    val previewText = remember(note.id, note.content) {
        note.content.take(NOTE_CARD_PREVIEW_MAX_LENGTH)
    }
    val clampedContentRanges = remember(contentHighlightRanges, previewText.length) {
        contentHighlightRanges.mapNotNull { range ->
            val safeStart = range.first.coerceIn(0, previewText.length)
            val safeEnd = (range.last + 1).coerceIn(safeStart, previewText.length)
            if (safeStart < safeEnd) safeStart..<safeEnd else null
        }
    }
    val previewAnnotated = remember(previewText, clampedContentRanges, markdownColors, imagePlaceholder) {
        buildNoteCardDisplayText(
            sourceText = previewText,
            ranges = clampedContentRanges,
            colors = markdownColors,
            highlightColor = primaryColor,
            renderMarkdown = true,
            imagePlaceholder = imagePlaceholder
        )
    }.takeIf { previewText.isNotBlank() }

    return NoteCardTextState(title = titleAnnotated, preview = previewAnnotated)
}

@Composable
private fun NoteCardSurface(
    note: Note,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState
) {
    val shape = NoteCollectionCardDefaults.shape
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> visuals.primaryColor
            note.isPinned -> visuals.primaryColor.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        },
        animationSpec = tween(
            durationMillis = NoteMotion.CARD_STATE_MS,
            easing = NoteMotion.cardStateEasing
        ),
        label = "noteCardBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else NoteCollectionCardDefaults.borderWidth,
        animationSpec = tween(
            durationMillis = NoteMotion.CARD_STATE_MS,
            easing = NoteMotion.cardStateEasing
        ),
        label = "noteCardBorderWidth"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = NoteMotion.CARD_STATE_MS,
                    easing = NoteMotion.cardStateEasing
                )
            )
            .roundedCombinedClickableTarget(
                shape = shape,
                onClick = onClick,
                onLongClick = onLongPress
            ),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        color = visuals.containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        NoteCardBody(
            note = note,
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
    selectionMode: Boolean,
    selected: Boolean,
    noteCardStyle: NoteCardStyle,
    visuals: NoteCardVisuals,
    textState: NoteCardTextState
) {
    val verticalPadding = if (noteCardStyle == NoteCardStyle.TITLE_ONLY) 10.dp else 18.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = verticalPadding
            )
    ) {
        NoteCardMainColumn(
            note = note,
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
            selectionMode = selectionMode,
            selected = selected
        )
        if (noteCardStyle != NoteCardStyle.TITLE_ONLY) {
            textState.preview?.let { preview ->
                Spacer(Modifier.height(6.dp))
                NoteCardPreview(preview)
            }
        }
        if (noteCardStyle == NoteCardStyle.TITLE_INFORMATION) {
            Spacer(Modifier.height(10.dp))
            NoteCardFooter(note, visuals.primaryColor)
        }
    }
}
