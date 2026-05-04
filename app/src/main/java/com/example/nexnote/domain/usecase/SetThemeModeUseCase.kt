package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.ThemeMode
import com.example.nexnote.domain.repository.IUserPreferencesRepository

class SetThemeModeUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }
}
