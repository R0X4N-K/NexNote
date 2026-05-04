package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository

class DeleteTemplateUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(template: Template) {
        repository.deleteTemplate(template)
    }
}
