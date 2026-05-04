package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetAccentColorUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(color: AccentColor) {
        repository.setAccentColor(color)
    }
}
