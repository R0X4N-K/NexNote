package io.github.r0x4nk.nexnote.ui.screen.templates

private const val DATE_PLACEHOLDER = "{{date}}"
private val MARKER_ONLY_LINE = Regex("""^\s*(?:[-*+]\s*|\d+\.\s*)(?:\[[ xX]\]\s*)?$""")
private val BLANK_LINE_RUN = Regex("""\n{2,}""")

/**
 * Normalises stored template content for the compact card preview.
 *
 * Templates keep authoring placeholders and empty checklist slots so an applied
 * note starts with the intended structure; the card only needs a readable
 * digest. Resolving the date placeholder, dropping marker-only lines and
 * collapsing blank runs keeps the markdown preview dense without ever mutating
 * the template itself.
 */
internal fun templatePreviewSource(content: String, dateLabel: String): String =
    content.replace(DATE_PLACEHOLDER, dateLabel)
        .lineSequence()
        .filterNot { MARKER_ONLY_LINE.matches(it) }
        .joinToString("\n")
        .replace(BLANK_LINE_RUN, "\n")
        .trim('\n')
