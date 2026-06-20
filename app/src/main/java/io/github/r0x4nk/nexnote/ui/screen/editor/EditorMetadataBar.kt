package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import io.github.r0x4nk.nexnote.util.DateUtils

internal const val EDITOR_METADATA_BAR_TAG = "editor_metadata_bar"

@Immutable
internal data class EditorNoteMetadata(
    val characterCount: Int,
    val lastModifiedDate: Long?,
    val creationDate: Long
)

/**
 * Compact, single-line note summary used as the editor top-bar subtitle.
 * Keeping the three values in one [Text] gives ellipsis ownership to a single
 * layout node, so narrow devices degrade predictably instead of clipping one of
 * several adjacent labels.
 */
@Composable
internal fun EditorMetadataBar(
    metadata: EditorNoteMetadata,
    modifier: Modifier = Modifier
) {
    val charCount = metadata.characterCount
    val secondaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    val charCountColor = when {
        charCount >= 400_000 -> MaterialTheme.colorScheme.error
        charCount >= 50_000 -> MaterialTheme.colorScheme.tertiary
        else -> secondaryColor
    }
    val summary = buildAnnotatedString {
        withStyle(SpanStyle(color = charCountColor)) {
            append("$charCount chars")
        }
        metadata.lastModifiedDate?.let { timestamp ->
            withStyle(SpanStyle(color = secondaryColor)) {
                append(" · ${DateUtils.formatRelative(timestamp)}")
            }
        }
        withStyle(SpanStyle(color = secondaryColor)) {
            append(" · ${DateUtils.formatDate(metadata.creationDate)}")
        }
    }

    Text(
        text = summary,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.testTag(EDITOR_METADATA_BAR_TAG)
    )
}
