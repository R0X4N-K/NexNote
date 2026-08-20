package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetTableLayoutModeUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(mode: TableLayoutMode) {
        repository.setTableLayoutMode(mode)
    }
}
