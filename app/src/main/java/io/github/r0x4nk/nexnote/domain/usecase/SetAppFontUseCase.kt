package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.AppFont
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetAppFontUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(font: AppFont) {
        repository.setAppFont(font)
    }
}
