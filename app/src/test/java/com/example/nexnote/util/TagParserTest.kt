package com.example.nexnote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TagParser.extractTags].
 *
 * All tests are pure JVM — no Android context required.
 */
class TagParserTest {

    private fun extract(content: String): Set<String> = TagParser.extractTags(content)

    // ── Basic extraction ──────────────────────────────────────────────────────

    @Test
    fun `single tag in text`() {
        assertEquals(setOf("kotlin"), extract("Learning #kotlin today"))
    }

    @Test
    fun `multiple distinct tags`() {
        val result = extract("Notes on #kotlin and #android development")
        assertEquals(setOf("kotlin", "android"), result)
    }

    @Test
    fun `tag at start of line`() {
        val result = extract("#projects\nsome content")
        assertTrue("projects" in result)
    }

    @Test
    fun `tag at start of string`() {
        assertEquals(setOf("work"), extract("#work item"))
    }

    @Test
    fun `tag after newline`() {
        val result = extract("line one\n#secondline")
        assertTrue("secondline" in result)
    }

    @Test
    fun `tags deduplicated`() {
        val result = extract("#kotlin is great. I love #kotlin")
        assertEquals(setOf("kotlin"), result)
    }

    // ── Case normalisation ────────────────────────────────────────────────────

    @Test
    fun `case insensitive normalisation`() {
        val result = extract("#Kotlin and #KOTLIN and #kotlin")
        assertEquals(setOf("kotlin"), result)
    }

    @Test
    fun `mixed case normalised to lowercase`() {
        val result = extract("#MyTag")
        assertEquals(setOf("mytag"), result)
    }

    // ── Tag name rules ────────────────────────────────────────────────────────

    @Test
    fun `tag with digits after initial letter`() {
        val result = extract("#android14 release")
        assertTrue("android14" in result)
    }

    @Test
    fun `tag with underscores`() {
        val result = extract("#my_project plan")
        assertTrue("my_project" in result)
    }

    @Test
    fun `numeric-only tag not extracted`() {
        val result = extract("#123 is not a tag")
        assertFalse("123" in result)
    }

    @Test
    fun `heading style double-hash not extracted`() {
        val result = extract("## Heading text")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `triple hash not extracted`() {
        val result = extract("### Subheading")
        assertTrue(result.isEmpty())
    }

    // ── Empty and whitespace ──────────────────────────────────────────────────

    @Test
    fun `empty string returns empty set`() {
        assertTrue(extract("").isEmpty())
    }

    @Test
    fun `content with no tags returns empty set`() {
        assertTrue(extract("Just regular text without any hashtags.").isEmpty())
    }

    // ── Fenced code block exclusion ───────────────────────────────────────────

    @Test
    fun `tag inside fenced code block excluded`() {
        val content = """
            |Here is code:
            |```kotlin
            |val x = #notATag
            |```
            |End
        """.trimMargin()
        assertTrue(extract(content).isEmpty())
    }

    @Test
    fun `tag outside fenced code block included`() {
        val content = """
            |#realTag here
            |```kotlin
            |val x = #notATag
            |```
            |Done
        """.trimMargin()
        assertEquals(setOf("realtag"), extract(content))
    }

    @Test
    fun `tags in multiple fenced blocks all excluded`() {
        val content = """
            |```
            |#block1
            |```
            |plain text
            |```python
            |#block2
            |```
        """.trimMargin()
        assertTrue(extract(content).isEmpty())
    }

    @Test
    fun `tags between two fenced blocks extracted`() {
        val content = """
            |```
            |#excluded
            |```
            |#included between blocks
            |```
            |#alsoExcluded
            |```
        """.trimMargin()
        assertEquals(setOf("included"), extract(content))
    }

    // ── Inline code inclusion ─────────────────────────────────────────────────

    @Test
    fun `tag inside inline code is included`() {
        val result = extract("Use `#inline` syntax")
        assertTrue("inline" in result)
    }

    // ── Adjacent punctuation ──────────────────────────────────────────────────

    @Test
    fun `tag followed by comma stops at comma`() {
        val result = extract("Try #kotlin, it's great")
        assertTrue("kotlin" in result)
        assertFalse("kotlin," in result)
    }

    @Test
    fun `tag followed by period stops at period`() {
        val result = extract("I love #android.")
        assertTrue("android" in result)
    }

    @Test
    fun `hash preceded by non-whitespace is not a tag`() {
        val result = extract("foo#bar is not a tag")
        assertFalse("bar" in result)
    }
}
