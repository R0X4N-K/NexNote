package io.github.r0x4nk.nexnote.util

import io.github.r0x4nk.nexnote.domain.model.NoteAttachment

/** Standard Markdown links remain usable after extracting an exported bundle. */
object AttachmentMarkdown {
    private val link = Regex("""^\s*\[((?:\\.|[^\]\\])*)]\(([^)\s]+)\)\s*$""")
    private val escaped = Regex("""\\([\\\[\]#*_`~|])""")
    private val labelPunctuation = Regex("""[\\\[\]#*_`~|]""")

    fun parse(line: String): NoteAttachment? {
        val match = link.matchEntire(line) ?: return null
        val path = match.groupValues[2]
        if (!NoteAttachment.isAttachmentPath(path)) return null
        return NoteAttachment(path, escaped.replace(match.groupValues[1]) { it.groupValues[1] })
    }

    fun format(attachment: NoteAttachment): String {
        val label = attachment.displayName.replace(Regex("[\\p{Cc}\\p{Cf}]"), " ")
            .trim().take(200).ifBlank { "Attachment" }
        val name = labelPunctuation.replace(label) { "\\${it.value}" }
        require(NoteAttachment.isAttachmentPath(attachment.path))
        return "[$name](${attachment.path})"
    }
}
