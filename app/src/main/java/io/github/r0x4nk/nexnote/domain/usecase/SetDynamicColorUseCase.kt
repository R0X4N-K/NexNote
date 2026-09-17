package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetDynamicColorUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(color: Boolean) {
        repository.setDynamicColor(color)
    }
}
