package io.github.r0x4nk.nexnote.data.local

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AttachmentImportJournalTest {
    @get:Rule val folder = TemporaryFolder()
    private val path = "images/attachments/note_1_12345678-abcd.pdf"
    private fun target() = File(folder.root, path).also { it.parentFile!!.mkdirs() }

    @Test fun `restart removes interrupted staging and orphan payload`() = runTest {
        val marker = AttachmentImportJournal(folder.root, "old").begin(path)
        val file = target().also { it.writeText("orphan") }
        val partial = File(file.parentFile, "${file.name}.part").also { it.writeText("partial") }
        AttachmentImportJournal(folder.root, "new").recover { emptySet() }
        assertFalse(marker.exists())
        assertFalse(file.exists())
        assertFalse(partial.exists())
    }

    @Test fun `retains referenced files including undo ownership and clears journal`() = runTest {
        val marker = AttachmentImportJournal(folder.root, "old").begin(path)
        val file = target().also { it.writeText("keep") }
        AttachmentImportJournal(folder.root, "new").recover { setOf(path) }
        assertTrue(file.exists())
        assertFalse(marker.exists())
    }

    @Test fun `locked vault defers orphan decision until manifest available`() = runTest {
        val marker = AttachmentImportJournal(folder.root, "old").begin(path)
        val file = target().also { it.writeText("keep until unlock") }
        val recovery = AttachmentImportJournal(folder.root, "new")
        recovery.recover { null }
        assertTrue(file.exists())
        assertTrue(marker.exists())
        recovery.recover { emptySet() }
        assertFalse(file.exists())
        assertFalse(marker.exists())
    }

    @Test fun `many attachments read each owner manifest only once per recovery`() = runTest {
        val old = AttachmentImportJournal(folder.root, "old")
        val paths = (1..50).map { "images/attachments/note_1_${it.toString(16)}.pdf" }.toSet()
        paths.forEach { path ->
            old.begin(path)
            File(folder.root, path).apply { parentFile!!.mkdirs(); writeText("keep") }
        }
        var lookups = 0
        val recovery = AttachmentImportJournal(folder.root, "new")
        recovery.recover { lookups++; null }
        assertEquals(1, lookups)
        lookups = 0
        recovery.recover { lookups++; paths }
        assertEquals(1, lookups)
        assertEquals(50, File(folder.root, "images/attachments").listFiles()!!.size)
    }

    @Test fun `active imports are never collected`() = runTest {
        val journal = AttachmentImportJournal(folder.root, "same")
        val marker = journal.begin(path)
        val file = target().also { it.writeText("save pending") }
        journal.recover { error("Must not inspect an active import") }
        assertTrue(file.exists())
        assertTrue(marker.exists())
    }
}
