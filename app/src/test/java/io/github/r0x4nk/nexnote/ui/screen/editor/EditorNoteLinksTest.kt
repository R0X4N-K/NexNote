package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.r0x4nk.nexnote.ui.navigation.Screen
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
    fun findNoteLinkAutocompleteMatch_ignoresCursorInsideOpeningTrigger() {
        val value = TextFieldValue(
            text = "See [[note:1|Project]]",
            selection = TextRange("See [".length)
        )

        assertNull(findNoteLinkAutocompleteMatch(value))
    }

    @Test
    fun findNoteLinkAutocompleteMatch_ignoresTriggerOutsideQueryWindow() {
        val text = "[[" + "a".repeat(81)
        val value = TextFieldValue(
            text = text,
            selection = TextRange(text.length)
        )

        assertNull(findNoteLinkAutocompleteMatch(value))
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
    fun findNoteLinkAutocompleteMatch_readsOnlyTheCursorWindowFromCharSequence() {
        val text = WindowOnlyCharSequence("Prefix ".repeat(20) + "See [[proj")

        val match = findNoteLinkAutocompleteMatch(text, TextRange(text.length))

        assertEquals(text.length - "[[proj".length, match?.start)
        assertEquals(text.length, match?.endExclusive)
        assertEquals("proj", match?.query)
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

    @Test
    fun previewNoteLinkEditorRoute_keepsVaultLinksInsideVaultEditorPath() {
        assertEquals(
            Screen.Editor.vaultNoteRoute(7L),
            previewNoteLinkEditorRoute(isVaultNote = true, targetNoteId = 7L)
        )
        assertEquals(
            Screen.Editor.existingNoteRoute(7L),
            previewNoteLinkEditorRoute(isVaultNote = false, targetNoteId = 7L)
        )
    }

    private class WindowOnlyCharSequence(
        private val value: String
    ) : CharSequence {
        override val length: Int get() = value.length

        override fun get(index: Int): Char = value[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            error("Autocomplete must not request a full subsequence")
        }

        override fun toString(): String {
            error("Autocomplete must not copy the full editor text")
        }
    }
}
