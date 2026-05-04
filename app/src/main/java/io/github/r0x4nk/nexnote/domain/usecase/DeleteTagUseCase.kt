package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.TagRepository

class DeleteTagUseCase(
    private val repository: TagRepository
) {
    suspend operator fun invoke(tagName: String) {
        repository.deleteTag(tagName)
    }
}
