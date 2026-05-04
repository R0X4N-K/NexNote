package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.AccentColor
import com.example.nexnote.domain.repository.IUserPreferencesRepository

class SetAccentColorUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(color: AccentColor) {
        repository.setAccentColor(color)
    }
}
