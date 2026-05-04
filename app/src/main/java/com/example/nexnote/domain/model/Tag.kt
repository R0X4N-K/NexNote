package com.example.nexnote.domain.model

/**
 * Domain model for a tag extracted from note content.
 *
 * Role: domain layer — pure Kotlin data class with no Android dependencies.
 *
 * [name] is the canonical lowercase tag identifier without the leading '#'.
 * [noteCount] is the number of *active* (non-deleted) notes containing this tag.
 *   Tags in trashed notes are preserved for restore, but do not count here.
 * [createdDate] is set once when the tag is first seen in any note.
 * [lastUpdatedDate] is updated every time any note containing this tag is saved.
 */
data class Tag(
    val name: String,
    val noteCount: Int,
    val createdDate: Long,
    val lastUpdatedDate: Long
)
