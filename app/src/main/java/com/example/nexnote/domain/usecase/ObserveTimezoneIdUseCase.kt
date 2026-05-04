package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveTimezoneIdUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<String> {
        return repository.timezoneId
    }
}
