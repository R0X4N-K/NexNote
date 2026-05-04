package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveLeftHandedUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.isLeftHanded
    }
}
