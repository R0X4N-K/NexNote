package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.TagRepository

class IndexNoteTagsUseCase(
    private val repository: TagRepository
) {
    suspend operator fun invoke(noteId: Long, content: String) {
        repository.indexNoteTags(noteId, content)
    }
}
