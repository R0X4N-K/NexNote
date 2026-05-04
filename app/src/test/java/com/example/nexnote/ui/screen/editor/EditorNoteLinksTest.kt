package com.example.nexnote.ui.screen.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorNoteLinksTest {

    @Test
    fun findNoteLinkAutocompleteMatch_detectsOpenTriggerBeforeCursor() {
        val value = TextFieldValue(
            text = "See [[proj",
            selection = TextRange("See [[proj".length)
        )

        val match = findNoteLinkAutocompleteMatch(value)

        assertEquals(4, match?.start)
        assertEquals("See [[proj".length, match?.endExclusive)
        assertEquals("proj", match?.query)
    }

    @Test
    fun findNoteLinkAutocompleteMatch_ignoresClosedLink() {
        val value = TextFieldValue(
            text = "See [[note:1|Project]]",
            selection = TextRange("See [[note:1|Project]]".length)
        )

        assertNull(findNoteLinkAutocompleteMatch(value))
    }

    @Test
    fun filterNoteLinkTargets_matchesTitleCaseInsensitively() {
        val targets = listOf(
            NoteLinkTarget(id = 1, title = "Project Plan"),
            NoteLinkTarget(id = 2, title = "Daily notes")
        )

        val result = filterNoteLinkTargets(targets, query = "project", limit = 6)

        assertEquals(listOf(targets.first()), result)
    }

    @Test
    fun noteLinkMarkdownFor_usesStableIdAndSanitizedTitle() {
        val markdown = noteLinkMarkdownFor(NoteLinkTarget(id = 7, title = "A [risky] | title"))

        assertEquals("[[note:7|A risky title]]", markdown)
        assertTrue(markdown.contains("note:7"))
    }
}
