package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.FontScale
import com.example.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFontScaleUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<FontScale> {
        return repository.fontScale
    }
}
