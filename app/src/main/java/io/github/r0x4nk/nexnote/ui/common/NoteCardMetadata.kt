package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.runtime.Immutable
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.util.AttachmentMarkdown
import io.github.r0x4nk.nexnote.util.TagParser

internal const val NOTE_CARD_MAX_TAGS = 3
internal const val NOTE_CARD_MAX_ATTACHMENTS = 2

/**
 * Compact metadata shown in a note card footer.
 *
 * Replaces the former "MD" marker, which carried no information because every
 * note is Markdown. [tags] are normalised, [attachmentNames] are the original
 * file labels when resolvable, [attachmentCount] is the real number of files
 * (which can exceed the resolved names), and [imageCount] counts embedded
 * images only.
 */
@Immutable
internal data class NoteCardMetadata(
    val tags: List<String> = emptyList(),
    val attachmentNames: List<String> = emptyList(),
    val attachmentCount: Int = 0,
    val imageCount: Int = 0
) {
    val hasAttachments: Boolean get() = attachmentCount > 0
    val hasImages: Boolean get() = imageCount > 0
}

/**
 * Builds the footer metadata for [note] without touching storage.
 *
 * Tag and attachment extraction are bounded and short-circuited: notes without
 * a '#' or without an attachment path never scan their body, which keeps long
 * notes cheap to render in list surfaces.
 */
internal fun buildNoteCardMetadata(
    note: Note,
    maxTags: Int = NOTE_CARD_MAX_TAGS,
    maxAttachments: Int = NOTE_CARD_MAX_ATTACHMENTS
): NoteCardMetadata {
    val tags = if ('#' in note.content) {
        TagParser.extractTags(note.content).sorted().take(maxTags)
    } else {
        emptyList()
    }

    val attachmentPaths = note.imagePaths.filter(NoteAttachment::isAttachmentPath)
    val imageCount = note.imagePaths.size - attachmentPaths.size
    val attachmentNames = if (attachmentPaths.isEmpty()) {
        emptyList()
    } else {
        val fromContent = if (NoteAttachment.DIRECTORY in note.content) {
            extractAttachmentNames(note.content, maxAttachments)
        } else {
            emptyList()
        }
        fromContent.ifEmpty {
            attachmentPaths.take(maxAttachments).map { path -> path.substringAfterLast('/') }
        }
    }

    return NoteCardMetadata(
        tags = tags,
        attachmentNames = attachmentNames,
        attachmentCount = attachmentPaths.size,
        imageCount = imageCount
    )
}

private fun extractAttachmentNames(content: String, limit: Int): List<String> {
    val names = LinkedHashSet<String>(limit)
    for (line in content.lineSequence()) {
        if (names.size >= limit) break
        val attachment = AttachmentMarkdown.parse(line) ?: continue
        names += attachment.displayName
    }
    return names.toList()
}
