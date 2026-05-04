package io.github.r0x4nk.nexnote.util

internal object MarkdownPatterns {
    val STANDALONE_IMAGE_LINE = Regex("""^\s*!\[([^\]]*?)]\(([^)]+?)\)\s*$""")
    val HORIZONTAL_RULE = Regex("""^\s*([-*_])\s*(?:\1\s*){2,}$""")
    val CODE_FENCE_OPEN = Regex("""^(\s*)(```+|~~~+)(.*)$""")
    val CODE_FENCE_CLOSE = Regex("""^(\s*)(```+|~~~+)\s*$""")
    val TABLE_LINE = Regex("""^\s*\|.+\|\s*$""")
    val TABLE_SEPARATOR_LINE = Regex("""^\s*\|(\s*:?-+:?\s*\|)+\s*$""")
    val CHECKBOX_UNCHECKED = Regex("""^(\s*)[*\-]\s+\[ ]\s*""")
    val CHECKBOX_CHECKED = Regex("""^(\s*)[*\-]\s+\[[xX]]\s*""")
    val BULLET_LINE = Regex("""^(\s*)([*\-])\s+""")
    val ORDERED_LIST = Regex("""^(\s*)(\d+)\.\s+""")
}
