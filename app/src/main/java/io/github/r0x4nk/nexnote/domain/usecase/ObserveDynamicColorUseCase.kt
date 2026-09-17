package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveDynamicColorUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.dynamicColor
    }
}
