package com.example.nexnote.ui.screen.agenda

import com.example.nexnote.data.db.NoteDao
import com.example.nexnote.data.db.entity.NoteEntity
import com.example.nexnote.data.db.model.NoteLinkCandidateProjection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class AgendaFakeNoteDao : NoteDao {

    private val notes = MutableStateFlow<List<NoteEntity>>(emptyList())

    fun addNote(note: NoteEntity) {
        notes.value = notes.value + note
    }

    override fun getAllNotes(): Flow<List<NoteEntity>> =
        notes.map { list -> list.filter { !it.isDeleted } }

    override fun getAllNotesSortedAsc(): Flow<List<NoteEntity>> =
        notes.map { list -> list.filter { !it.isDeleted } }

    override fun getDeletedNotes(): Flow<List<NoteEntity>> =
        notes.map { list -> list.filter { it.isDeleted } }

    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        notes.map { list ->
            list
                .filter { !it.isDeleted }
                .map { NoteLinkCandidateProjection(id = it.id, title = it.title) }
        }

    override fun getAllCreationDates(): Flow<List<Long>> =
        notes.map { list -> list.filter { !it.isDeleted }.map { it.creationDate } }

    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<NoteEntity>> =
        notes.map { list ->
            list.filter { !it.isDeleted && it.creationDate in startMs until endMs }
        }

    override fun searchNotes(query: String): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override suspend fun getNoteById(id: Long): NoteEntity? =
        notes.value.find { it.id == id }

    override suspend fun insertNote(note: NoteEntity): Long = 0L
    override suspend fun updateNote(note: NoteEntity) = Unit
    override suspend fun moveToTrash(id: Long, deletedDate: Long) = Unit
    override suspend fun restoreFromTrash(id: Long) = Unit
    override suspend fun deleteNotePermanently(id: Long) = Unit
    override suspend fun emptyTrash() = Unit
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit
    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) = Unit
    override suspend fun updateNoteContent(id: Long, content: String, lastModifiedDate: Long) = Unit
}
