package com.example.nexnote.util

import com.example.nexnote.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {

    // ── scoreAndRank: blank query ────────────────────────────────────────────

    @Test
    fun `emptyQuery - returns all notes with zero score`() {
        val notes = listOf(note(1, "Alpha"), note(2, "Beta"))
        val results = SearchUtils.scoreAndRank(notes, "")
        assertEquals(2, results.size)
        assertTrue(results.all { it.score == 0 })
        assertTrue(results.all { it.titleRanges.isEmpty() && it.contentRanges.isEmpty() })
    }

    @Test
    fun `blankQuery - returns all notes with zero score`() {
        val notes = listOf(note(1, "Alpha"))
        val results = SearchUtils.scoreAndRank(notes, "   ")
        assertEquals(1, results.size)
        assertEquals(0, results.first().score)
    }

    // ── scoreAndRank: scoring ────────────────────────────────────────────────

    @Test
    fun `titleMatch weighs more than contentMatch`() {
        val titleMatch = note(1, title = "Kotlin guide", content = "Hello world")
        val contentMatch = note(2, title = "Hello world", content = "Kotlin basics")
        val results = SearchUtils.scoreAndRank(listOf(titleMatch, contentMatch), "Kotlin")
        // titleMatch: match in title (3 pts), contentMatch: match in content (1 pt)
        assertEquals(1L, results.first().note.id)
        assertTrue(results[0].score > results[1].score)
    }

    @Test
    fun `titlePrefixBonus is applied when title starts with query`() {
        val prefixed = note(1, title = "Kotlin tips", content = "nope")
        val midTitle = note(2, title = "Learn Kotlin", content = "nope")
        val results = SearchUtils.scoreAndRank(listOf(prefixed, midTitle), "kotlin")
        // prefixed: 3 (title match) + 5 (prefix bonus) = 8
        // midTitle: 3 (title match) + 0 = 3
        assertEquals(1L, results.first().note.id)
        assertEquals(8, results.first().score)
        assertEquals(3, results[1].score)
    }

    @Test
    fun `multipleMatches yield higher score`() {
        val oneMatch = note(1, title = "test", content = "hello")
        val threeMatches = note(2, title = "test", content = "test test test")
        val results = SearchUtils.scoreAndRank(listOf(oneMatch, threeMatches), "test")
        // oneMatch: title(3) + prefix(5) + content(0) = 8
        // threeMatches: title(3) + prefix(5) + content(3*1 = 3) = 11
        assertEquals(2L, results.first().note.id)
        assertTrue(results[0].score > results[1].score)
    }

    // ── scoreAndRank: filtering ──────────────────────────────────────────────

    @Test
    fun `noMatch note is excluded from results`() {
        val matching = note(1, title = "Hello", content = "world")
        val noMatch = note(2, title = "Goodbye", content = "moon")
        val results = SearchUtils.scoreAndRank(listOf(matching, noMatch), "hello")
        assertEquals(1, results.size)
        assertEquals(1L, results.first().note.id)
    }

    // ── scoreAndRank: case-insensitive ───────────────────────────────────────

    @Test
    fun `caseInsensitive - matches regardless of case`() {
        val results = SearchUtils.scoreAndRank(
            listOf(note(1, title = "HELLO WORLD")),
            "hello"
        )
        assertEquals(1, results.size)
        assertEquals(1, results.first().titleRanges.size)
    }

    // ── scoreAndRank: pinned priority ────────────────────────────────────────

    @Test
    fun `pinnedNotes always appear first even with lower score`() {
        val highScore = note(1, title = "test test test", content = "test", isPinned = false)
        val lowScorePinned = note(2, title = "nope", content = "test", isPinned = true)
        val results = SearchUtils.scoreAndRank(listOf(highScore, lowScorePinned), "test")
        assertEquals(2L, results.first().note.id) // pinned first
    }

    // ── scoreAndRank: empty title ────────────────────────────────────────────

    @Test
    fun `emptyTitle - matches only on content`() {
        val results = SearchUtils.scoreAndRank(
            listOf(note(1, title = "", content = "hello world")),
            "hello"
        )
        assertEquals(1, results.size)
        assertTrue(results.first().titleRanges.isEmpty())
        assertEquals(1, results.first().contentRanges.size)
        // score = content match only: 1
        assertEquals(1, results.first().score)
    }

    // ── findRanges: correct indices ──────────────────────────────────────────

    @Test
    fun `findRanges returns correct indices`() {
        val ranges = SearchUtils.findRanges("Hello World Hello", "hello")
        assertEquals(2, ranges.size)
        assertEquals(0..<5, ranges[0])
        assertEquals(12..<17, ranges[1])
    }

    @Test
    fun `findRanges with overlapping matches`() {
        val ranges = SearchUtils.findRanges("aaaa", "aa")
        // "aa" in "aaaa": index 0, 1, 2 => 3 overlapping matches
        assertEquals(3, ranges.size)
        assertEquals(0..<2, ranges[0])
        assertEquals(1..<3, ranges[1])
        assertEquals(2..<4, ranges[2])
    }

    // ── specialChars: no crash ───────────────────────────────────────────────

    @Test
    fun `specialChars in query do not crash`() {
        val notes = listOf(note(1, title = "Hello % world _", content = "test"))
        // These should not throw
        val results1 = SearchUtils.scoreAndRank(notes, "%")
        assertEquals(1, results1.size)

        val results2 = SearchUtils.scoreAndRank(notes, "_")
        assertEquals(1, results2.size)

        val results3 = SearchUtils.scoreAndRank(notes, "🎉")
        assertEquals(0, results3.size) // no match, but no crash
    }

    // ── empty input edge cases ───────────────────────────────────────────────

    @Test
    fun `findRanges empty text returns empty`() {
        assertEquals(emptyList<IntRange>(), SearchUtils.findRanges("", "hello"))
    }

    @Test
    fun `findRanges empty query returns empty`() {
        assertEquals(emptyList<IntRange>(), SearchUtils.findRanges("hello", ""))
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun note(
        id: Long,
        title: String = "",
        content: String = "",
        isPinned: Boolean = false
    ) = Note(id = id, title = title, content = content, isPinned = isPinned)
}
