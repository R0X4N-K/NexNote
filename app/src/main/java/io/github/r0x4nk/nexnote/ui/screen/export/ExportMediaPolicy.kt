package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment

/** Keeps readable labels while removing media destinations from a text-only export. */
internal object ExportMediaPolicy {
    private val tokens = Regex(
        """(?ms)(^```[^\n]*\n.*?^```[^\n]*$|^~~~[^\n]*\n.*?^~~~[^\n]*$|`+[^`\n]*`+)|(!?)\[((?:\\.|[^\]\\])*)]\(([^)\s]+)\)"""
    )

    /**
     * Applies the user's "include media" choice to the notes before writing them.
     *
     * When the toggle is off the exporter must not need a ZIP for TXT/Markdown,
     * so image and attachment destinations are replaced by their readable labels.
     * PDF and print output are different: images are embedded directly into the
     * rendered document, so they are kept even without a ZIP. Attachments cannot
     * be embedded and are still reduced to their labels.
     */
    fun apply(
        notes: List<Note>,
        includeMedia: Boolean,
        format: ExportFormat
    ): List<Note> =
        if (includeMedia) notes else notes.map { note ->
            val keepImages = format.embedsImages
            note.copy(
                content = if (note.isMarkdown) withoutMedia(note.content, keepImages) else note.content,
                imagePaths = if (keepImages) note.imagePaths else emptyList()
            )
        }

    private fun withoutMedia(content: String, keepImages: Boolean): String = tokens.replace(content) { match ->
        when {
            match.groups[1] != null -> match.value // Code samples are literal text.
            match.groupValues[2] == "!" -> if (keepImages) match.value else match.groupValues[3]
            NoteAttachment.isAttachmentPath(match.groupValues[4]) -> match.groupValues[3]
            else -> match.value
        }
    }
}

/** Whether a format renders media inside the document instead of shipping a ZIP. */
internal val ExportFormat.embedsImages: Boolean
    get() = this == ExportFormat.PDF || this == ExportFormat.PRINT
