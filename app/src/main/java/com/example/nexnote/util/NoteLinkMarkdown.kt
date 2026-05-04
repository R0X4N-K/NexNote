package com.example.nexnote.util

/**
 * Markdown-like syntax used to link one note to another.
 *
 * The id keeps the link stable when the target note is renamed, while the title
 * keeps the raw markdown readable for people editing the note.
 */
object NoteLinkMarkdown {
    const val ANNOTATION_TAG = "NOTE_LINK"

    private const val OPEN = "[[note:"
    private const val CLOSE = "]]"
    private const val TITLE_SEPARATOR = '|'

    data class ParsedLink(
        val noteId: Long,
        val title: String,
        val endIndexExclusive: Int
    )

    fun create(noteId: Long, title: String): String {
        require(noteId > 0) { "Note links require a persisted note id." }
        return "$OPEN$noteId$TITLE_SEPARATOR${title.toSafeLabel()}$CLOSE"
    }

    fun parseAt(text: String, startIndex: Int): ParsedLink? {
        if (!text.startsWith(OPEN, startIndex)) return null

        val closeIndex = text.indexOf(CLOSE, startIndex + OPEN.length)
        if (closeIndex == -1) return null

        val body = text.substring(startIndex + OPEN.length, closeIndex)
        val separatorIndex = body.indexOf(TITLE_SEPARATOR)
        val noteIdText = if (separatorIndex == -1) body else body.substring(0, separatorIndex)
        val noteId = noteIdText.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val title = if (separatorIndex == -1) "" else body.substring(separatorIndex + 1)

        return ParsedLink(
            noteId = noteId,
            title = title.trim().ifBlank { "Note $noteId" },
            endIndexExclusive = closeIndex + CLOSE.length
        )
    }

    private fun String.toSafeLabel(): String =
        replace(Regex("""[\[\]\|\r\n]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { "Untitled note" }
}
