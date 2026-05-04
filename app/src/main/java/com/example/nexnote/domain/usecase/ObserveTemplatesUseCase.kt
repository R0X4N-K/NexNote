package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.Template
import com.example.nexnote.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class ObserveTemplatesUseCase(
    private val repository: TemplateRepository
) {
    operator fun invoke(): Flow<List<Template>> {
        return repository.allTemplates
    }
}
