package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.IUserPreferencesRepository

class SetLeftHandedUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(value: Boolean) {
        repository.setLeftHanded(value)
    }
}
