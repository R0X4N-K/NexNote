package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.NoteRepository

class EmptyTrashUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke() {
        repository.emptyTrash()
    }
}
