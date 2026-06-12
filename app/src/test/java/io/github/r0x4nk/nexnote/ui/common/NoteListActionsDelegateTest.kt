package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListActionsDelegateTest {

    @Test
    fun `list controls update shared state flows`() = runTest {
        val fixture = newFixture(this)

        fixture.delegate.toggleSortOrder()
        assertEquals(SortOrder.MODIFIED_ASC, fixture.sortOrder.value)

        fixture.delegate.toggleSortOrder()
        assertEquals(SortOrder.MODIFIED_DESC, fixture.sortOrder.value)

        fixture.delegate.toggleViewMode()
        assertEquals(NoteListViewMode.GRID, fixture.viewMode.value)

        fixture.delegate.toggleViewMode()
        assertEquals(NoteListViewMode.TAGS, fixture.viewMode.value)

        fixture.delegate.toggleViewMode()
        assertEquals(NoteListViewMode.LIST, fixture.viewMode.value)

        fixture.delegate.toggleTagFilter("kotlin")
        assertEquals(setOf("kotlin"), fixture.selectedTagFilters.value)

        fixture.delegate.toggleTagFilter("kotlin")
        assertEquals(emptySet<String>(), fixture.selectedTagFilters.value)

        fixture.delegate.toggleTagFilter("work")
        fixture.delegate.toggleTagFilter("home")
        fixture.delegate.removeTagFilter("work")
        assertEquals(setOf("home"), fixture.selectedTagFilters.value)

        fixture.delegate.clearTagFilters()
        assertEquals(emptySet<String>(), fixture.selectedTagFilters.value)
    }

    @Test
    fun `trash actions write before snackbar and undo restores note`() = runTest {
        val fixture = newFixture(this)
        val note = Note(id = 7L, title = "", content = "\n  Draft   outline  ")

        fixture.delegate.requestTrash(note)
        advanceUntilIdle()

        val trashEvent = fixture.trashEvents.receive()
        assertEquals(listOf(7L), fixture.repository.trashedIds)
        assertEquals(7L, trashEvent.noteId)
        assertEquals("Draft outline", trashEvent.noteLabel)

        fixture.delegate.undoPendingTrash(note.id)
        advanceUntilIdle()

        assertEquals(listOf(7L), fixture.repository.restoredIds)
    }

    @Test
    fun `bulk trash writes all notes and emits one undoable event`() = runTest {
        val fixture = newFixture(this)
        val notes = listOf(
            Note(id = 7L, title = "First"),
            Note(id = 8L, title = "Second")
        )

        fixture.delegate.requestTrash(notes)
        advanceUntilIdle()

        val trashEvent = fixture.trashEvents.receive()
        assertEquals(listOf(7L, 8L), fixture.repository.trashedIds)
        assertEquals(listOf(7L, 8L), trashEvent.noteIds)
        assertEquals("Moved 2 notes to trash", trashEvent.snackbarMessage())
    }

    @Test
    fun `togglePin flips note pinned state through use case`() = runTest {
        val fixture = newFixture(this)

        fixture.delegate.togglePin(Note(id = 5L, isPinned = false))
        advanceUntilIdle()

        assertEquals(listOf(5L to true), fixture.repository.pinnedChanges)
    }

    @Test
    fun `duplicateNote sends success message after duplicate completes`() = runTest {
        val fixture = newFixture(this)

        fixture.delegate.duplicateNote(Note(id = 2L, title = "Plan", content = "Ship #release"))
        advanceUntilIdle()

        assertEquals("Duplicated \"Plan\"", fixture.noteActionMessages.receive())
        assertEquals("Ship #release", fixture.repository.savedNotes.getValue(100L).content)
        assertEquals(listOf(100L to "Ship #release"), fixture.tagRepository.indexedNotes)
    }

    @Test
    fun `duplicateNote sends failure message when duplicate fails`() = runTest {
        val fixture = newFixture(this, repository = FakeNoteListRepository(failOnSave = true))

        fixture.delegate.duplicateNote(Note(id = 2L, title = "Plan"))
        advanceUntilIdle()

        assertEquals("Could not duplicate \"Plan\"", fixture.noteActionMessages.receive())
    }

    @Test
    fun `duplicateNote rejects Vault note without exposing its label`() = runTest {
        val fixture = newFixture(this)

        fixture.delegate.duplicateNote(
            Note(id = 2L, title = "Secret title", content = "Private body", isInVault = true)
        )
        advanceUntilIdle()

        assertEquals("Could not duplicate note", fixture.noteActionMessages.receive())
        assertEquals(emptyMap<Long, Note>(), fixture.repository.savedNotes)
        assertEquals(emptyList<Pair<Long, String>>(), fixture.tagRepository.indexedNotes)
    }

    private fun newFixture(
        scope: CoroutineScope,
        repository: FakeNoteListRepository = FakeNoteListRepository(),
        tagRepository: FakeNoteListTagRepository = FakeNoteListTagRepository(),
        duplicateNote: DuplicateNoteUseCase? = DuplicateNoteUseCase(
            noteRepository = repository,
            tagRepository = tagRepository,
            imageStorage = NoOpNoteImageStorage()
        )
    ): NoteListActionsFixture {
        val sortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
        val viewMode = MutableStateFlow(NoteListViewMode.LIST)
        val selectedTagFilters = MutableStateFlow(emptySet<String>())
        val trashEvents = Channel<TrashedNoteEvent>(Channel.BUFFERED)
        val noteActionMessages = Channel<String>(Channel.BUFFERED)

        return NoteListActionsFixture(
            repository = repository,
            tagRepository = tagRepository,
            sortOrder = sortOrder,
            viewMode = viewMode,
            selectedTagFilters = selectedTagFilters,
            trashEvents = trashEvents,
            noteActionMessages = noteActionMessages,
            delegate = NoteListActionsDelegate(
                scope = scope,
                moveNoteToTrash = MoveNoteToTrashUseCase(repository),
                restoreNoteFromTrash = RestoreNoteFromTrashUseCase(repository),
                toggleNotePin = ToggleNotePinUseCase(repository),
                duplicateNoteUseCase = duplicateNote,
                sortOrder = sortOrder,
                viewMode = viewMode,
                selectedTagFilters = selectedTagFilters,
                trashEvents = trashEvents,
                noteActionMessages = noteActionMessages
            )
        )
    }

    private class NoteListActionsFixture(
        val repository: FakeNoteListRepository,
        val tagRepository: FakeNoteListTagRepository,
        val sortOrder: MutableStateFlow<SortOrder>,
        val viewMode: MutableStateFlow<NoteListViewMode>,
        val selectedTagFilters: MutableStateFlow<Set<String>>,
        val trashEvents: Channel<TrashedNoteEvent>,
        val noteActionMessages: Channel<String>,
        val delegate: NoteListActionsDelegate
    )
}

private class FakeNoteListRepository(
    private val failOnSave: Boolean = false
) : NoteRepository {
    val trashedIds = mutableListOf<Long>()
    val restoredIds = mutableListOf<Long>()
    val pinnedChanges = mutableListOf<Pair<Long, Boolean>>()
    val savedNotes = linkedMapOf<Long, Note>()
    private var nextId = 100L

    override val allNotes: Flow<List<Note>> = flowOf(emptyList())
    override val allNotesSortedAsc: Flow<List<Note>> = flowOf(emptyList())
    override val deletedNotes: Flow<List<Note>> = flowOf(emptyList())
    override val noteLinkCandidates: Flow<List<NoteLinkCandidate>> = flowOf(emptyList())
    override val distinctActiveDays: Flow<Set<Long>> = flowOf(emptySet())
    override val distinctLocalDays: Flow<Set<Long>> = flowOf(emptySet())

    override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
    override fun searchNotesScored(query: String): Flow<List<ScoredNote>> = flowOf(emptyList())
    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>> =
        flowOf(emptyList())

    override suspend fun getNoteById(id: Long): Note? = savedNotes[id]

    override suspend fun saveNote(note: Note): Long {
        if (failOnSave) error("Duplicate failed")
        val id = if (note.id == 0L) nextId++ else note.id
        savedNotes[id] = note.copy(id = id)
        return id
    }

    override suspend fun moveToTrash(id: Long) {
        trashedIds += id
    }

    override suspend fun restoreFromTrash(id: Long) {
        restoredIds += id
    }

    override suspend fun deleteNotePermanently(id: Long) = Unit
    override suspend fun emptyTrash() = Unit

    override suspend fun setPinned(id: Long, isPinned: Boolean) {
        pinnedChanges += id to isPinned
    }

    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
}

private class FakeNoteListTagRepository : TagRepository {
    val indexedNotes = mutableListOf<Pair<Long, String>>()

    override fun getAllTagsByUsageDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByUsageAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateDesc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun getAllTagsByDateAsc(): Flow<List<Tag>> = flowOf(emptyList())
    override fun searchTags(query: String): Flow<List<Tag>> = flowOf(emptyList())
    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> = flowOf(emptyList())
    override fun getMostUsedTags(limit: Int): Flow<List<Tag>> = flowOf(emptyList())
    override fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>> = flowOf(emptySet())

    override suspend fun indexNoteTags(noteId: Long, content: String) {
        indexedNotes += noteId to content
    }

    override suspend fun deleteTag(tagName: String) = Unit
}
