package com.example.nexnote.data.db.model

/**
 * Lightweight row used by note-link UI. It intentionally excludes note content
 * so editor startup never loads every note body just to validate internal links.
 */
data class NoteLinkCandidateProjection(
    val id: Long,
    val title: String
)
