package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.util.TagParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GenerateDebugNotesUseCaseTest {

    @Test
    fun `generation creates varied realistic notes and indexes every note`() = runTest {
        val savedNotes = mutableListOf<Note>()
        val indexedNotes = mutableListOf<Pair<Long, String>>()
        val progressUpdates = mutableListOf<Int>()
        val now = 1_800_000_000_000L
        val useCase = GenerateDebugNotesUseCase(
            saveNote = { note ->
                savedNotes += note
                savedNotes.size.toLong()
            },
            indexNoteTags = { noteId, content -> indexedNotes += noteId to content },
            currentTimeMillis = { now },
            random = Random(42)
        )

        val generatedCount = useCase(count = 40, onProgress = progressUpdates::add)

        assertEquals(40, generatedCount)
        assertEquals(40, savedNotes.size)
        assertEquals(40, indexedNotes.size)
        assertEquals(40, savedNotes.map(Note::title).distinct().size)
        assertTrue(savedNotes.all(Note::isMarkdown))
        assertTrue(savedNotes.all { it.creationDate <= now })
        assertTrue(savedNotes.map(Note::creationDate).distinct().size > 30)
        assertTrue(savedNotes.map { it.content.length }.distinct().size > 10)
        assertTrue(savedNotes.all { TagParser.extractTags(it.content).isNotEmpty() })
        assertTrue(savedNotes.all { "https://developer.android.com/" in it.content })
        assertTrue(savedNotes.drop(1).all { "[[note:" in it.content })
        assertEquals((1L..40L).toList(), indexedNotes.map { it.first })
        assertEquals((1..40).toList(), progressUpdates)
    }

    @Test
    fun `generation rejects quantities outside the supported range`() = runTest {
        var saveCalls = 0
        val useCase = GenerateDebugNotesUseCase(
            saveNote = {
                saveCalls += 1
                saveCalls.toLong()
            },
            indexNoteTags = { _, _ -> },
            random = Random(7)
        )

        val result = runCatching {
            useCase(GenerateDebugNotesUseCase.MAX_NOTE_COUNT + 1)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, saveCalls)
    }
}
