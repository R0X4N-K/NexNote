package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.model.Note

internal fun Collection<Note>.shareAsText(): String =
    copyAsMarkdown()

internal fun Collection<Note>.shareSubject(strings: StringProvider): String =
    when (size) {
        0 -> strings.get(R.string.share_subject)
        1 -> first().title.trim().ifBlank { strings.get(R.string.share_subject) }
        else -> strings.get(R.string.share_subject_many, size)
    }
