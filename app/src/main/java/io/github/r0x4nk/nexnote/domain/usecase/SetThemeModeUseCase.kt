package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetThemeModeUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }
}
