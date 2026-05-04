package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetTimezoneIdUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(id: String) {
        repository.setTimezoneId(id)
    }
}
