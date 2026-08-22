package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.HomeNotesQuery
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface NoteRepository {
    val allNotes: Flow<List<Note>>
    val allNotesSortedAsc: Flow<List<Note>>
    val deletedNotes: Flow<List<Note>>
    val noteLinkCandidates: Flow<List<NoteLinkCandidate>>
    val distinctActiveDays: Flow<Set<Long>>
    val distinctLocalDays: Flow<Set<Long>>
    val activeNoteCount: Flow<Int>
        get() = allNotes.map { notes -> notes.size }
    val allNormalNoteCount: Flow<Int>
        get() = combine(allNotes, deletedNotes) { active, deleted ->
            active.size + deleted.size
        }

    /** Bounded Home result; production repositories should push filtering into storage. */
    fun observeHomeNotes(query: HomeNotesQuery): Flow<List<Note>> {
        val source = if (query.sortAscending) allNotesSortedAsc else allNotes
        return source.map { notes ->
            notes.asSequence()
                .filter { note -> when (query.pinnedFilter) {
                    HomePinnedFilter.ALL -> true
                    HomePinnedFilter.PINNED -> note.isPinned
                    HomePinnedFilter.UNPINNED -> !note.isPinned
                } }
                .filter { note ->
                    query.text.isBlank() || when (query.searchScope) {
                        HomeSearchScope.TITLE_AND_CONTENT ->
                            note.title.contains(query.text, ignoreCase = true) ||
                                note.content.contains(query.text, ignoreCase = true)
                        HomeSearchScope.TITLE ->
                            note.title.contains(query.text, ignoreCase = true)
                        HomeSearchScope.CONTENT ->
                            note.content.contains(query.text, ignoreCase = true)
                    }
                }
                .let { filtered ->
                    if (query.text.isBlank()) filtered else when (query.searchSort) {
                        HomeSearchSort.RELEVANCE -> filtered
                        HomeSearchSort.MODIFIED_DESC -> filtered.sortedWith(
                            compareByDescending<Note> { it.isPinned }
                                .thenByDescending { it.lastModifiedDate }
                        )
                        HomeSearchSort.MODIFIED_ASC -> filtered.sortedWith(
                            compareByDescending<Note> { it.isPinned }
                                .thenBy { it.lastModifiedDate }
                        )
                        HomeSearchSort.TITLE_ASC -> filtered.sortedWith(
                            compareByDescending<Note> { it.isPinned }.thenBy {
                                it.title.lowercase(java.util.Locale.ROOT)
                            }
                        )
                        HomeSearchSort.TITLE_DESC -> filtered.sortedWith(
                            compareByDescending<Note> { it.isPinned }.thenByDescending {
                                it.title.lowercase(java.util.Locale.ROOT)
                            }
                        )
                    }
                }
                .take(query.limit)
                .toList()
        }
    }

    /** All ids matching the Home query, independent of the materialisation window. */
    fun observeHomeNoteIds(query: HomeNotesQuery): Flow<List<Long>> =
        observeHomeNotes(query.copy(limit = Int.MAX_VALUE)).map { notes ->
            notes.map(Note::id)
        }

    fun searchNotes(query: String): Flow<List<Note>>
    fun searchNotesScored(query: String): Flow<List<ScoredNote>>
    fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>>

    suspend fun getNoteById(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun moveToTrash(id: Long)
    suspend fun moveToTrash(ids: Collection<Long>) {
        ids.distinct().forEach { id -> moveToTrash(id) }
    }
    suspend fun restoreFromTrash(id: Long)
    suspend fun restoreFromTrash(ids: Collection<Long>) {
        ids.distinct().forEach { id -> restoreFromTrash(id) }
    }
    suspend fun deleteNotePermanently(id: Long)
    suspend fun emptyTrash()
    suspend fun deleteAllNormalNotesPermanently(): Int {
        val activeIds = allNotes.first().map(Note::id)
        val deletedIds = deletedNotes.first().map(Note::id)
        moveToTrash(activeIds)
        (activeIds + deletedIds).distinct().forEach { id -> deleteNotePermanently(id) }
        return (activeIds + deletedIds).distinct().size
    }
    suspend fun setPinned(id: Long, isPinned: Boolean)
    suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean)
}
