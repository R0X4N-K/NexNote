package io.github.r0x4nk.nexnote.util

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.util.SearchUtils.CONTENT_WEIGHT
import io.github.r0x4nk.nexnote.util.SearchUtils.TITLE_PREFIX_BONUS
import io.github.r0x4nk.nexnote.util.SearchUtils.TITLE_WEIGHT
import io.github.r0x4nk.nexnote.util.SearchUtils.scoreAndRank

/**
 * Utilities for optimised note search.
 *
 * Home supplies candidates narrowed by Room FTS, while Agenda and Vault supply
 * their already-bounded note collections. These functions apply the same
 * matching, highlighting, relevance, and ordering contract to each surface.
 *
 * Weights:
 * - Each title match contributes [TITLE_WEIGHT] (3 points).
 * - Each content match contributes [CONTENT_WEIGHT] (1 point).
 * - If the title starts with the query (prefix match), add [TITLE_PREFIX_BONUS] (5 points).
 *
 * Final sort order: pinned notes first, then descending score.
 */
object SearchUtils {

    /** Applies the shared scope, highlighting, relevance, and result ordering contract. */
    fun searchAndSort(
        notes: List<Note>,
        query: String,
        scope: HomeSearchScope,
        sort: NoteSearchSort
    ): List<ScoredNote> {
        if (sort == NoteSearchSort.RELEVANCE) return scoreAndRank(notes, query, scope)
        return scoreInOrder(notes, query, scope).sortedWith(sort.scoredNoteComparator)
    }

    /**
     * Computes the relevance score for each note and returns the sorted list.
     *
     * - If [query] is blank, returns all notes with score 0 and empty ranges.
     * - Notes with no matches are excluded.
     * - Sort order: isPinned DESC, score DESC.
     */
    fun scoreAndRank(
        notes: List<Note>,
        query: String,
        scope: HomeSearchScope = HomeSearchScope.TITLE_AND_CONTENT
    ): List<ScoredNote> {
        return scoreInOrder(notes, query, scope).sortedWith(
            compareByDescending<ScoredNote> { it.note.isPinned }
                .thenByDescending { it.score }
        )
    }

    /** Scores and highlights matches while preserving the DAO result order. */
    fun scoreInOrder(
        notes: List<Note>,
        query: String,
        scope: HomeSearchScope = HomeSearchScope.TITLE_AND_CONTENT
    ): List<ScoredNote> {
        if (query.isBlank()) {
            return notes.map { ScoredNote(it, 0, emptyList(), emptyList()) }
        }
        val terms = query.searchTerms()
        return notes.mapNotNull { note -> note.score(terms, scope) }
    }

    private fun Note.score(
        terms: List<String>,
        scope: HomeSearchScope
    ): ScoredNote? {
        val titleRangesByTerm = terms.map { term ->
            if (scope == HomeSearchScope.CONTENT) emptyList() else findRanges(title, term)
        }
        val contentRangesByTerm = terms.map { term ->
            if (scope == HomeSearchScope.TITLE) emptyList() else findRanges(content, term)
        }
        if (terms.indices.any { index ->
                titleRangesByTerm[index].isEmpty() && contentRangesByTerm[index].isEmpty()
            }) {
            return null
        }
        val titleRanges = titleRangesByTerm.flatten().distinct().sortedBy(IntRange::first)
        val contentRanges = contentRangesByTerm.flatten().distinct().sortedBy(IntRange::first)
        val prefixBonus = if (
            scope != HomeSearchScope.CONTENT &&
            terms.any { term -> title.lowercase(java.util.Locale.ROOT).startsWith(term) }
        ) {
            TITLE_PREFIX_BONUS
        } else {
            0
        }
        return ScoredNote(
            note = this,
            score = titleRanges.size * TITLE_WEIGHT +
                contentRanges.size * CONTENT_WEIGHT + prefixBonus,
            titleRanges = titleRanges,
            contentRanges = contentRanges
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

        val lowerText = text.lowercase(java.util.Locale.ROOT)
        val lowerQuery = query.lowercase(java.util.Locale.ROOT)
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

    private val NoteSearchSort.scoredNoteComparator: Comparator<ScoredNote>
        get() = when (this) {
            NoteSearchSort.RELEVANCE -> error("Relevance is sorted by scoreAndRank")
            NoteSearchSort.MODIFIED_DESC ->
                compareByDescending<ScoredNote> { result -> result.note.isPinned }
                    .thenByDescending { result -> result.note.lastModifiedDate }
                    .thenByDescending { result -> result.note.id }
            NoteSearchSort.MODIFIED_ASC ->
                compareByDescending<ScoredNote> { result -> result.note.isPinned }
                    .thenBy { result -> result.note.lastModifiedDate }
                    .thenBy { result -> result.note.id }
            NoteSearchSort.TITLE_ASC ->
                compareByDescending<ScoredNote> { result -> result.note.isPinned }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { result -> result.note.title }
                    .thenBy { result -> result.note.id }
            NoteSearchSort.TITLE_DESC ->
                compareByDescending<ScoredNote> { result -> result.note.isPinned }
                    .thenByDescending(String.CASE_INSENSITIVE_ORDER) { result -> result.note.title }
                    .thenByDescending { result -> result.note.id }
        }

    private fun String.searchTerms(): List<String> = SEARCH_TERM_PATTERN.findAll(this)
        .map { match -> match.value.lowercase(java.util.Locale.ROOT) }
        .distinct()
        .toList()
        .ifEmpty { listOf(lowercase(java.util.Locale.ROOT)) }

    private val SEARCH_TERM_PATTERN = Regex("""[\p{L}\p{N}]+""")
}
