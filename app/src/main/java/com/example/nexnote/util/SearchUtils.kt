package com.example.nexnote.util

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.ScoredNote
import com.example.nexnote.util.SearchUtils.CONTENT_WEIGHT
import com.example.nexnote.util.SearchUtils.TITLE_PREFIX_BONUS
import com.example.nexnote.util.SearchUtils.TITLE_WEIGHT
import com.example.nexnote.util.SearchUtils.scoreAndRank

/**
 * Utilities for optimised note search.
 *
 * Strategy: "SQL filters, Kotlin scores":
 * - Room/DAO returns notes matching via LIKE (coarse filter).
 * - [scoreAndRank] computes a weighted score and returns notes sorted by relevance.
 *
 * Weights:
 * - Each title match contributes [TITLE_WEIGHT] (3 points).
 * - Each content match contributes [CONTENT_WEIGHT] (1 point).
 * - If the title starts with the query (prefix match), add [TITLE_PREFIX_BONUS] (5 points).
 *
 * Final sort order: pinned notes first, then descending score.
 */
object SearchUtils {

    /**
     * Computes the relevance score for each note and returns the sorted list.
     *
     * - If [query] is blank, returns all notes with score 0 and empty ranges.
     * - Notes with no matches are excluded.
     * - Sort order: isPinned DESC, score DESC.
     */
    fun scoreAndRank(notes: List<Note>, query: String): List<ScoredNote> {
        if (query.isBlank()) {
            return notes.map { ScoredNote(it, 0, emptyList(), emptyList()) }
        }

        val lowerQuery = query.lowercase()

        return notes.mapNotNull { note ->
            val titleRanges   = findRanges(note.title, lowerQuery)
            val contentRanges = findRanges(note.content, lowerQuery)

            if (titleRanges.isEmpty() && contentRanges.isEmpty()) return@mapNotNull null

            val score = (titleRanges.size * TITLE_WEIGHT) +
                    (contentRanges.size * CONTENT_WEIGHT) +
                    (if (note.title.lowercase().startsWith(lowerQuery)) TITLE_PREFIX_BONUS else 0)

            ScoredNote(note, score, titleRanges, contentRanges)
        }.sortedWith(
            compareByDescending<ScoredNote> { it.note.isPinned }
                .thenByDescending { it.score }
        )
    }

    /**
     * Finds all case-insensitive occurrences of [query] in [text].
     *
     * Returns a list of [IntRange] values indicating start..end (inclusive)
     * of each occurrence in the original text.
     *
     * Uses a sliding search: after each match advances by 1 character to also
     * find overlapping matches (e.g. "aa" in "aaa" → 2 ranges).
     */
    internal fun findRanges(text: String, query: String): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) return emptyList()

        val lowerText  = text.lowercase()
        val lowerQuery = query.lowercase()
        val ranges     = mutableListOf<IntRange>()
        var startIndex = 0

        while (startIndex <= lowerText.length - lowerQuery.length) {
            val foundIndex = lowerText.indexOf(lowerQuery, startIndex)
            if (foundIndex == -1) break
            ranges.add(foundIndex..<foundIndex + lowerQuery.length)
            startIndex = foundIndex + 1
        }

        return ranges
    }

    private const val TITLE_WEIGHT      = 3
    private const val CONTENT_WEIGHT    = 1
    private const val TITLE_PREFIX_BONUS = 5
}
