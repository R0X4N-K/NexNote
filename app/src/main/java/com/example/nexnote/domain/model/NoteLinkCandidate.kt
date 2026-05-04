package com.example.nexnote.domain.model

/**
 * Minimal note data needed to build internal note-link targets.
 */
data class NoteLinkCandidate(
    val id: Long,
    val title: String
)
