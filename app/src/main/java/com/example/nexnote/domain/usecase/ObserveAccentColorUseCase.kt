package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.AccentColor
import com.example.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveAccentColorUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<AccentColor> {
        return repository.accentColor
    }
}
