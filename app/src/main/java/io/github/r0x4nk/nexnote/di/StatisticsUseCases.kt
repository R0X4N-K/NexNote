package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.NoteStatisticsRepository
import io.github.r0x4nk.nexnote.domain.usecase.ObserveIndexedNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteStatisticsIndexStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RebuildNoteStatisticsIndexUseCase

internal class StatisticsUseCases internal constructor(
    repository: NoteStatisticsRepository
) {
    val observeIndexedNotes = ObserveIndexedNoteStatisticsUseCase(repository)
    val observeIndexState = ObserveNoteStatisticsIndexStateUseCase(repository)
    val rebuildIndex = RebuildNoteStatisticsIndexUseCase(repository)
}
