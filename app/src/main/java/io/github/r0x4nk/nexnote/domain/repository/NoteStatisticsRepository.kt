package io.github.r0x4nk.nexnote.domain.repository

import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import kotlinx.coroutines.flow.Flow

/** Persistent, incrementally maintained source for note statistics. */
interface NoteStatisticsRepository {
    val indexedNotes: Flow<List<IndexedNoteStatistics>>
    val indexState: Flow<NoteStatisticsIndexState>

    /** Discards derived values so the background indexer rebuilds them from notes. */
    suspend fun rebuildIndex()
}
