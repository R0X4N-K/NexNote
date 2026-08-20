package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveTableLayoutModeUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<TableLayoutMode> = repository.tableLayoutMode
}
