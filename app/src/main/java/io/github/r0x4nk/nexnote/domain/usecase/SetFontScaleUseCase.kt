package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetFontScaleUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(scale: FontScale) {
        repository.setFontScale(scale)
    }
}
