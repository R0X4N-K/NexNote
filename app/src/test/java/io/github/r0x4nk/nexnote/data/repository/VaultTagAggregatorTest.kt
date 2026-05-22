package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.VaultTagAggregator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [VaultTagAggregator]. The helper is pure (no Room, no
 * Android, no I/O), so we can drive it directly with synthetic [Note] inputs.
 */
class VaultTagAggregatorTest {

    @Test
    fun `aggregate on empty list returns empty`() {
        val tags = VaultTagAggregator.aggregate(emptyList())
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `aggregate parses tags from content only and counts unique notes`() {
        val notes = listOf(
            note(
                id = 1L,
                title = "Buying #titleOnly soon",
                content = "remember to pick up #groceries #milk and #bread",
                created = 100L,
                modified = 200L
            ),
            note(
                id = 2L,
                title = "Random note",
                content = "more #groceries planning #bread #milk",
                created = 150L,
                modified = 300L
            )
        )

        val tags = VaultTagAggregator.aggregate(notes)
        val byName = tags.associateBy { it.name }

        assertEquals(setOf("bread", "groceries", "milk"), byName.keys)
        assertEquals(2, byName.getValue("groceries").noteCount)
        assertEquals(2, byName.getValue("milk").noteCount)
        assertEquals(2, byName.getValue("bread").noteCount)
    }

    @Test
    fun `aggregate dedupes tag occurrences within the same note`() {
        val notes = listOf(
            note(id = 1L, content = "#todo #todo and #todo again")
        )

        val tags = VaultTagAggregator.aggregate(notes)
        assertEquals(1, tags.size)
        assertEquals(1, tags.single().noteCount)
        assertEquals("todo", tags.single().name)
    }

    @Test
    fun `aggregate ignores trashed notes even if present in the input`() {
        val notes = listOf(
            note(id = 1L, content = "#alpha", deleted = false),
            note(id = 2L, content = "#alpha #beta", deleted = true)
        )

        val tags = VaultTagAggregator.aggregate(notes)
        val byName = tags.associateBy { it.name }

        assertEquals(setOf("alpha"), byName.keys)
        assertEquals(1, byName.getValue("alpha").noteCount)
    }

    @Test
    fun `aggregate uses min created and max modified across notes`() {
        val notes = listOf(
            note(id = 1L, content = "#shared", created = 500L, modified = 600L),
            note(id = 2L, content = "#shared", created = 100L, modified = 700L),
            note(id = 3L, content = "#shared", created = 300L, modified = 200L)
        )

        val tag = VaultTagAggregator.aggregate(notes).single()
        assertEquals("shared", tag.name)
        assertEquals(3, tag.noteCount)
        assertEquals(100L, tag.createdDate)
        assertEquals(700L, tag.lastUpdatedDate)
    }

    @Test
    fun `aggregate orders by descending count then ascending name`() {
        val notes = listOf(
            note(id = 1L, content = "#zebra #alpha"),
            note(id = 2L, content = "#alpha"),
            note(id = 3L, content = "#alpha"),
            note(id = 4L, content = "#beta #zebra")
        )

        val tags = VaultTagAggregator.aggregate(notes)
        assertEquals(listOf("alpha", "zebra", "beta"), tags.map { it.name })
        assertEquals(listOf(3, 2, 1), tags.map { it.noteCount })
    }

    @Test
    fun `aggregate ignores tags inside fenced code blocks`() {
        val notes = listOf(
            note(
                id = 1L,
                content = """
                    real tag #visible
                    ```
                    code block #hidden
                    ```
                """.trimIndent()
            )
        )

        val tags = VaultTagAggregator.aggregate(notes)
        val names = tags.map { it.name }.toSet()
        assertEquals(setOf("visible"), names)
    }

    private fun note(
        id: Long,
        title: String = "",
        content: String = "",
        created: Long = 1_000L,
        modified: Long = 1_000L,
        deleted: Boolean = false
    ): Note = Note(
        id = id,
        title = title,
        content = content,
        isMarkdown = true,
        creationDate = created,
        lastModifiedDate = modified,
        timezone = "UTC",
        isDeleted = deleted,
        deletedDate = if (deleted) modified else null,
        isInVault = true,
        isPinned = false,
        imagePaths = emptyList(),
        backgroundColor = null,
        isPreviewMode = false
    )
}
