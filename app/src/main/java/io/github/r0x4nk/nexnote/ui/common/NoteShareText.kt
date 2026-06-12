package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note

private const val NEXNOTE_SHARE_SUBJECT = "NexNote note"

internal fun Collection<Note>.shareAsText(): String =
    copyAsMarkdown()

internal fun Collection<Note>.shareSubject(): String =
    when (size) {
        0 -> NEXNOTE_SHARE_SUBJECT
        1 -> first().title.trim().ifBlank { NEXNOTE_SHARE_SUBJECT }
        else -> "$size NexNote notes"
    }
