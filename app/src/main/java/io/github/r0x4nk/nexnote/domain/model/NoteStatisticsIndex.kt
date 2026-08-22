package io.github.r0x4nk.nexnote.domain.model

/** Text-derived values persisted for one normal note. */
data class IndexedNoteStatistics(
    val noteId: Long,
    val creationDate: Long,
    val sourceLastModifiedDate: Long,
    val characterCount: Int,
    val wordCount: Int,
    val tagNames: Set<String>
)

/** Progress of the persistent statistics index for active normal notes. */
data class NoteStatisticsIndexState(
    val indexedNotes: Int = 0,
    val totalNotes: Int = 0,
    val isRetryingAfterError: Boolean = false
) {
    val isIndexing: Boolean
        get() = indexedNotes < totalNotes
}
