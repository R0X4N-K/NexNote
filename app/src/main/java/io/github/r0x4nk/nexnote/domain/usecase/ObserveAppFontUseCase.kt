package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.AppFont
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveAppFontUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<AppFont> {
        return repository.appFont
    }
}
