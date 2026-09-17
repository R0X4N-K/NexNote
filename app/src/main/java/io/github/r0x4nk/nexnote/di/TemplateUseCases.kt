package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase

internal class TemplateUseCases internal constructor(
    templateRepository: TemplateRepository
) {
    val observeTemplates = ObserveTemplatesUseCase(templateRepository)
    val getTemplateById = GetTemplateByIdUseCase(templateRepository)
    val saveTemplate = SaveTemplateUseCase(templateRepository)
    val deleteTemplate = DeleteTemplateUseCase(templateRepository)
}
