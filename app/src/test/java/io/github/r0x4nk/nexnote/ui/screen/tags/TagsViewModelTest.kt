package io.github.r0x4nk.nexnote.ui.screen.tags

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTagUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchTagsUseCase
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var noteRepository: TagsFakeNoteRepository
    private lateinit var tagRepository: TagsFakeTagRepository
    private lateinit var viewModel: TagsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        noteRepository = TagsFakeNoteRepository()
        tagRepository = TagsFakeTagRepository(noteRepository)
        viewModel = TagsViewModel(
            observeTagsByUsageDesc = ObserveTagsByUsageDescUseCase(tagRepository),
            observeTagsByUsageAsc = ObserveTagsByUsageAscUseCase(tagRepository),
            observeTagsByDateDesc = ObserveTagsByDateDescUseCase(tagRepository),
            observeTagsByDateAsc = ObserveTagsByDateAscUseCase(tagRepository),
            searchTags = SearchTagsUseCase(tagRepository),
            observeFilteredNoteIds = ObserveFilteredNoteIdsUseCase(tagRepository),
            observeAllNotes = ObserveAllNotesUseCase(noteRepository),
            deleteTag = DeleteTagUseCase(tagRepository),
            moveNoteToTrash = MoveNoteToTrashUseCase(noteRepository),
            restoreNoteFromTrash = RestoreNoteFromTrashUseCase(noteRepository),
            duplicateNoteUseCase = DuplicateNoteUseCase(
                noteRepository = noteRepository,
                tagRepository = tagRepository,
                imageStorage = NoOpNoteImageStorage()
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        block()
    }

    @Test
    fun `requestTrash followed by undo restores note for selected tag`() = runViewModelTest {
        noteRepository.put(Note(id = 5L, title = "Tagged note", content = "#work"))
        tagRepository.setTags(noteId = 5L, tags = setOf("work"))
        advanceInitialTagDebounce()

        viewModel.toggleTagSelection("work")
        advanceUntilIdle()
        assertEquals(listOf(5L), viewModel.uiState.value.notesForSelectedTag.map { it.id })

        val note = viewModel.uiState.value.notesForSelectedTag.single()
        viewModel.requestTrash(note)
        advanceUntilIdle()

        assertEquals(listOf(5L), noteRepository.trashedIds)
        assertTrue(viewModel.uiState.value.notesForSelectedTag.isEmpty())
        assertEquals(listOf(5L), noteRepository.deletedNotesSnapshot().map { it.id })

        viewModel.undoPendingTrash(note.id)
        advanceUntilIdle()

        assertEquals(listOf(5L), noteRepository.restoredIds)
        assertEquals(listOf(5L), viewModel.uiState.value.notesForSelectedTag.map { it.id })
        assertTrue(noteRepository.deletedNotesSnapshot().isEmpty())
    }

    private suspend fun TestScope.advanceInitialTagDebounce() {
        advanceTimeBy(350)
        advanceUntilIdle()
    }
}

private class TagsFakeNoteRepository : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    val trashedIds = mutableListOf<Long>()
    val restoredIds = mutableListOf<Long>()

    override val allNotes: Flow<List<Note>> =
        notes.map { list -> list.activeSorted() }

    override val allNotesSortedAsc: Flow<List<Note>> =
        notes.map { list ->
            list.filter { !it.isDeleted }
                .sortedWith(
                    compareByDescending<Note> { it.isPinned }
                        .thenBy { it.lastModifiedDate }
                        .thenBy { it.id }
                )
        }

    override val deletedNotes: Flow<List<Note>> =
        notes.map { list -> list.filter { it.isDeleted } }

    override val noteLinkCandidates: Flow<List<NoteLinkCandidate>> =
        notes.map { list ->
            list.filter { !it.isDeleted }
                .map { NoteLinkCandidate(id = it.id, title = it.title) }
        }

    override val distinctActiveDays: Flow<Set<Long>> = flowOf(emptySet())
    override val distinctLocalDays: Flow<Set<Long>> = flowOf(emptySet())

    fun put(note: Note) {
        notes.value = notes.value.filterNot { it.id == note.id } + note
    }

    fun allNotesSnapshot(): List<Note> = notes.value

    fun deletedNotesSnapshot(): List<Note> =
        notes.value.filter { it.isDeleted }

    override fun searchNotes(query: String): Flow<List<Note>> =
        allNotes.map { list -> list.filter { it.matches(query) } }

    override fun searchNotesScored(query: String): Flow<List<ScoredNote>> =
        searchNotes(query).map { list ->
            list.map { ScoredNote(note = it, score = 0, titleRanges = emptyList(), contentRanges = emptyList()) }
        }

    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>> =
        allNotes.map { list -> list.filter { it.creationDate in startMs until endMs } }

    override suspend fun getNoteById(id: Long): Note? =
        notes.value.find { it.id == id }

    override suspend fun saveNote(note: Note): Long {
        val id = if (note.id == 0L) {
            (notes.value.maxOfOrNull { it.id } ?: 0L) + 1L
        } else {
            note.id
        }
        put(note.copy(id = id))
        return id
    }

    override suspend fun moveToTrash(id: Long) {
        trashedIds += id
        notes.value = notes.value.map { note ->
            if (note.id == id) note.copy(isDeleted = true, deletedDate = 1L) else note
        }
    }

    override suspend fun restoreFromTrash(id: Long) {
        restoredIds += id
        notes.value = notes.value.map { note ->
            if (note.id == id) note.copy(isDeleted = false, deletedDate = null) else note
        }
    }

    override suspend fun deleteNotePermanently(id: Long) {
        notes.value = notes.value.filterNot { it.id == id && it.isDeleted }
    }

    override suspend fun emptyTrash() {
        notes.value = notes.value.filterNot { it.isDeleted }
    }

    override suspend fun setPinned(id: Long, isPinned: Boolean) {
        notes.value = notes.value.map { note ->
            if (note.id == id) note.copy(isPinned = isPinned) else note
        }
    }

    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) {
        notes.value = notes.value.map { note ->
            if (note.id == id) note.copy(isPreviewMode = isPreviewMode) else note
        }
    }

    private fun List<Note>.activeSorted(): List<Note> =
        filter { !it.isDeleted }
            .sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.lastModifiedDate }
                    .thenBy { it.id }
            )

    private fun Note.matches(query: String): Boolean =
        title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
}

private class TagsFakeTagRepository(
    private val noteRepository: TagsFakeNoteRepository
) : TagRepository {
    private val refs = MutableStateFlow<Map<Long, Set<String>>>(emptyMap())

    fun setTags(noteId: Long, tags: Set<String>) {
        refs.value = refs.value + (noteId to tags)
    }

    override fun getAllTagsByUsageDesc(): Flow<List<Tag>> =
        tagsFlow().map { tags ->
            tags.sortedWith(compareByDescending<Tag> { it.noteCount }.thenBy { it.name })
        }

    override fun getAllTagsByUsageAsc(): Flow<List<Tag>> =
        tagsFlow().map { tags ->
            tags.sortedWith(compareBy<Tag> { it.noteCount }.thenBy { it.name })
        }

    override fun getAllTagsByDateDesc(): Flow<List<Tag>> =
        tagsFlow().map { tags -> tags.sortedByDescending { it.lastUpdatedDate } }

    override fun getAllTagsByDateAsc(): Flow<List<Tag>> =
        tagsFlow().map { tags -> tags.sortedBy { it.lastUpdatedDate } }

    override fun searchTags(query: String): Flow<List<Tag>> =
        getAllTagsByUsageDesc().map { tags ->
            tags.filter { it.name.contains(query, ignoreCase = true) }
        }

    override fun getTagsForNote(noteId: Long): Flow<List<Tag>> =
        tagsFlow().map { tags ->
            val names = refs.value[noteId].orEmpty()
            tags.filter { it.name in names }
        }

    override fun getMostUsedTags(limit: Int): Flow<List<Tag>> =
        getAllTagsByUsageDesc().map { it.take(limit) }

    override fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>> {
        if (tagNames.isEmpty()) return flowOf(emptySet())
        return refs.map { current ->
            current.filterValues { tags -> tags.containsAll(tagNames) }.keys
        }
    }

    override suspend fun indexNoteTags(noteId: Long, content: String) = Unit

    override suspend fun deleteTag(tagName: String) {
        refs.value = refs.value.mapValues { (_, tags) -> tags - tagName }
    }

    private fun tagsFlow(): Flow<List<Tag>> =
        combine(refs, noteRepository.allNotesSnapshotFlow()) { currentRefs, notes ->
            val activeIds = notes.filter { !it.isDeleted }.map { it.id }.toSet()
            currentRefs.values.flatten().distinct().map { tagName ->
                Tag(
                    name = tagName,
                    noteCount = currentRefs.count { (noteId, tags) ->
                        noteId in activeIds && tagName in tags
                    },
                    createdDate = 1L,
                    lastUpdatedDate = 1L
                )
            }
        }

    private fun TagsFakeNoteRepository.allNotesSnapshotFlow(): Flow<List<Note>> =
        combine(allNotes, deletedNotes) { active, deleted -> active + deleted }
}
