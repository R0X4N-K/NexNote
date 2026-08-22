package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTagsByUsageDesc(): Flow<List<Tag>>
    fun getAllTagsByUsageAsc(): Flow<List<Tag>>
    fun getAllTagsByDateDesc(): Flow<List<Tag>>
    fun getAllTagsByDateAsc(): Flow<List<Tag>>
    fun searchTags(query: String): Flow<List<Tag>>
    fun getTagsForNote(noteId: Long): Flow<List<Tag>>
    fun getMostUsedTags(limit: Int = DEFAULT_TOP_TAGS_LIMIT): Flow<List<Tag>>
    fun getFilteredNoteIds(tagNames: Set<String>): Flow<Set<Long>>
    fun observeNotesForTag(tagName: String): Flow<List<Note>>

    suspend fun indexNoteTags(noteId: Long, content: String)
    suspend fun deleteTag(tagName: String)

    companion object {
        const val DEFAULT_TOP_TAGS_LIMIT = 15
    }
}
