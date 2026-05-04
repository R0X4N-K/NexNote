package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFontScaleUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<FontScale> {
        return repository.fontScale
    }
}
