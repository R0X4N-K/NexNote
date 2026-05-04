package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.TagRepository

class DeleteTagUseCase(
    private val repository: TagRepository
) {
    suspend operator fun invoke(tagName: String) {
        repository.deleteTag(tagName)
    }
}
