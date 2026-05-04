package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.ThemeMode
import com.example.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<ThemeMode> {
        return repository.themeMode
    }
}
