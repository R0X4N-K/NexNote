package com.example.nexnote.ui.screen.editor

import androidx.compose.runtime.Immutable
import com.example.nexnote.util.SearchUtils

/**
 * Keeps in-note search deterministic and independent from Compose rendering.
 */
@Immutable
internal data class NoteSearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val matches: List<IntRange> = emptyList(),
    val currentIndex: Int = 0
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val hasMatches: Boolean get() = matches.isNotEmpty()
    val currentMatch: IntRange? get() = matches.getOrNull(currentIndex)
    val resultLabel: String
        get() = when {
            !hasQuery -> ""
            !hasMatches -> "0/0"
            else -> "${currentIndex + 1}/${matches.size}"
        }

    fun open(content: String): NoteSearchState =
        copy(isActive = true).refresh(content)

    fun close(): NoteSearchState = Empty

    fun updateQuery(newQuery: String, content: String): NoteSearchState =
        copy(query = newQuery).refresh(content)

    fun refresh(content: String): NoteSearchState {
        if (!isActive) return this

        val refreshedMatches = NoteSearchMatcher.findMatches(content, query)
        return copy(
            matches = refreshedMatches,
            currentIndex = currentIndexFor(refreshedMatches)
        )
    }

    fun next(): NoteSearchState {
        if (!hasMatches) return this
        return copy(currentIndex = (currentIndex + 1) % matches.size)
    }

    fun previous(): NoteSearchState {
        if (!hasMatches) return this
        return copy(currentIndex = (currentIndex - 1 + matches.size) % matches.size)
    }

    private fun currentIndexFor(refreshedMatches: List<IntRange>): Int {
        if (refreshedMatches.isEmpty()) return 0

        val preservedRangeIndex = currentMatch?.let(refreshedMatches::indexOf) ?: -1
        return when {
            preservedRangeIndex >= 0 -> preservedRangeIndex
            currentIndex in refreshedMatches.indices -> currentIndex
            else -> refreshedMatches.lastIndex
        }
    }

    companion object {
        val Empty = NoteSearchState()
    }
}

private object NoteSearchMatcher {
    fun findMatches(content: String, query: String): List<IntRange> {
        if (query.isBlank()) return emptyList()
        return SearchUtils.findRanges(content, query)
    }
}
