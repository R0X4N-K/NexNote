package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<ThemeMode> {
        return repository.themeMode
    }
}
