package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.TagRepository

class IndexNoteTagsUseCase(
    private val repository: TagRepository
) {
    suspend operator fun invoke(noteId: Long, content: String) {
        repository.indexNoteTags(noteId, content)
    }
}
