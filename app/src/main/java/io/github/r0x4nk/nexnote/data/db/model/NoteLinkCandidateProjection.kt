package io.github.r0x4nk.nexnote.data.db.model

/**
 * Lightweight row used by note-link UI. It intentionally excludes note content
 * so editor startup never loads every note body just to validate internal links.
 */
data class NoteLinkCandidateProjection(
    val id: Long,
    val title: String
)
