package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.FontScale
import com.example.nexnote.domain.repository.IUserPreferencesRepository

class SetFontScaleUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(scale: FontScale) {
        repository.setFontScale(scale)
    }
}
