package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class ObserveTemplatesUseCase(
    private val repository: TemplateRepository
) {
    operator fun invoke(): Flow<List<Template>> {
        return repository.allTemplates
    }
}
