package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.repository.NoteStatisticsRepository
import kotlinx.coroutines.flow.Flow

class ObserveIndexedNoteStatisticsUseCase(
    private val repository: NoteStatisticsRepository
) {
    operator fun invoke(): Flow<List<IndexedNoteStatistics>> = repository.indexedNotes
}

class ObserveNoteStatisticsIndexStateUseCase(
    private val repository: NoteStatisticsRepository
) {
    operator fun invoke(): Flow<NoteStatisticsIndexState> = repository.indexState
}

class RebuildNoteStatisticsIndexUseCase(
    private val repository: NoteStatisticsRepository
) {
    suspend operator fun invoke() = repository.rebuildIndex()
}
