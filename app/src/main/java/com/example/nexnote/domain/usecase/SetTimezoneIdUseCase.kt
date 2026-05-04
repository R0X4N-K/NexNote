package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.repository.IUserPreferencesRepository

class SetTimezoneIdUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(id: String) {
        repository.setTimezoneId(id)
    }
}
