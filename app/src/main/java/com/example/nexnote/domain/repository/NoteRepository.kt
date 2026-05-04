package com.example.nexnote.domain.repository

import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.NoteLinkCandidate
import com.example.nexnote.domain.model.ScoredNote
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    val allNotes: Flow<List<Note>>
    val allNotesSortedAsc: Flow<List<Note>>
    val deletedNotes: Flow<List<Note>>
    val noteLinkCandidates: Flow<List<NoteLinkCandidate>>
    val distinctActiveDays: Flow<Set<Long>>
    val distinctLocalDays: Flow<Set<Long>>

    fun searchNotes(query: String): Flow<List<Note>>
    fun searchNotesScored(query: String): Flow<List<ScoredNote>>
    fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>>

    suspend fun getNoteById(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun moveToTrash(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun deleteNotePermanently(id: Long)
    suspend fun emptyTrash()
    suspend fun setPinned(id: Long, isPinned: Boolean)
    suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean)
}
