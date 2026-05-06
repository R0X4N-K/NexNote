package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagIndexDedupPolicyTest {

    @Test
    fun `first call for a valid note id returns true`() {
        val policy = TagIndexDedupPolicy()

        assertTrue(policy.shouldIndexAndRemember(noteId = 7L, content = "#alpha body"))
    }

    @Test
    fun `repeated call with same content and same id is deduplicated`() {
        val policy = TagIndexDedupPolicy()
        val content = "Long body with #alpha and #beta tags"

        assertTrue(policy.shouldIndexAndRemember(noteId = 7L, content = content))
        assertFalse(policy.shouldIndexAndRemember(noteId = 7L, content = content))
        assertFalse(policy.shouldIndexAndRemember(noteId = 7L, content = content))
    }

    @Test
    fun `content change re-enables indexing for the same note id`() {
        val policy = TagIndexDedupPolicy()

        policy.shouldIndexAndRemember(noteId = 7L, content = "#alpha")
        assertTrue(policy.shouldIndexAndRemember(noteId = 7L, content = "#alpha #beta"))
    }

    @Test
    fun `note id change re-enables indexing even when content matches`() {
        val policy = TagIndexDedupPolicy()
        val content = "#alpha"

        policy.shouldIndexAndRemember(noteId = 7L, content = content)
        // Same content for a different note must still index, otherwise the
        // second note would never get its tag cross-refs persisted.
        assertTrue(policy.shouldIndexAndRemember(noteId = 11L, content = content))
    }

    @Test
    fun `invalid note ids are always skipped`() {
        val policy = TagIndexDedupPolicy()

        assertFalse(policy.shouldIndexAndRemember(noteId = 0L, content = "#alpha"))
        assertFalse(policy.shouldIndexAndRemember(noteId = -1L, content = "#alpha"))
    }

    @Test
    fun `invalid note id call does not poison subsequent valid calls`() {
        val policy = TagIndexDedupPolicy()
        val content = "#alpha"

        policy.shouldIndexAndRemember(noteId = 0L, content = content)
        // The earlier no-op for an unsaved note must not be remembered as if
        // indexing had succeeded — the first valid save still has to index.
        assertTrue(policy.shouldIndexAndRemember(noteId = 7L, content = content))
    }

    @Test
    fun `forgetLastIndex retries the next save after a failure`() {
        val policy = TagIndexDedupPolicy()
        val content = "#alpha"

        policy.shouldIndexAndRemember(noteId = 7L, content = content)
        policy.forgetLastIndex()

        assertTrue(policy.shouldIndexAndRemember(noteId = 7L, content = content))
    }
}
