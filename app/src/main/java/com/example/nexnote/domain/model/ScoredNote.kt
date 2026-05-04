package com.example.nexnote.domain.model

/**
 * Search result carrying score and match positions.
 *
 * [titleRanges] and [contentRanges] are the [IntRange] indices of each match
 * in the original text, used by the UI to highlight occurrences.
 */
data class ScoredNote(
    val note: Note,
    val score: Int,
    val titleRanges: List<IntRange>,
    val contentRanges: List<IntRange>
)
