package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.MarkdownPlainText

internal fun Note.copyAsPlainText(): String {
    val body = if (isMarkdown) MarkdownPlainText.fromMarkdown(content) else content
    return joinTitleAndBody(body)
}

internal fun Note.copyAsMarkdown(): String =
    joinTitleAndBody(content)

private fun Note.joinTitleAndBody(body: String): String {
    val cleanTitle = title.trim()
    return when {
        cleanTitle.isBlank() -> body
        body.isBlank() -> cleanTitle
        else -> "$cleanTitle\n\n$body"
    }
}
