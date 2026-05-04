package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Template
import com.example.nexnote.domain.repository.TemplateRepository

class SaveTemplateUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(template: Template): Long {
        return repository.saveTemplate(template)
    }
}
