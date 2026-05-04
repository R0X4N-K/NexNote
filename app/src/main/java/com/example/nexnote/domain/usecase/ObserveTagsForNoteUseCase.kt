package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class ObserveTagsForNoteUseCase(
    private val repository: TagRepository
) {
    operator fun invoke(noteId: Long): Flow<List<Tag>> {
        return repository.getTagsForNote(noteId)
    }
}
