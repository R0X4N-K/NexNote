package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.Tag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTagEffectsTest {

    @Test
    fun `selected tag is cleared only when it is no longer available`() {
        val tags = listOf(tag("alpha"), tag("beta"))

        assertFalse(shouldClearUnavailableSelectedTag(null, tags))
        assertFalse(shouldClearUnavailableSelectedTag("alpha", tags))
        assertTrue(shouldClearUnavailableSelectedTag("gamma", tags))
        assertTrue(shouldClearUnavailableSelectedTag("alpha", emptyList()))
    }

    private fun tag(name: String): Tag =
        Tag(
            name = name,
            noteCount = 1,
            createdDate = 1_000L,
            lastUpdatedDate = 1_000L
        )
}
