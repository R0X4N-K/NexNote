package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Template
import com.example.nexnote.domain.repository.TemplateRepository

class GetTemplateByIdUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(templateId: Long): Template? {
        return repository.getTemplateById(templateId)
    }
}
