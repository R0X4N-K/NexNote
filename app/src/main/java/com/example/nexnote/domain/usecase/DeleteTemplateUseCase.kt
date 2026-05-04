package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Template
import com.example.nexnote.domain.repository.TemplateRepository

class DeleteTemplateUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(template: Template) {
        repository.deleteTemplate(template)
    }
}
