package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.data.db.NoteStatisticsDao
import io.github.r0x4nk.nexnote.data.db.NoteStatisticsSource
import io.github.r0x4nk.nexnote.data.db.entity.NoteStatisticsIndexEntity
import io.github.r0x4nk.nexnote.domain.model.IndexedNoteStatistics
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.repository.NoteStatisticsRepository
import io.github.r0x4nk.nexnote.domain.usecase.NoteStatisticsTextAnalyzer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Room-backed statistics index that processes stale note bodies in bounded batches. */
class NoteStatisticsRepositoryImpl(
    private val dao: NoteStatisticsDao,
    private val appScope: CoroutineScope,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : NoteStatisticsRepository {

    private val started = AtomicBoolean(false)
    private val rebuildMutex = Mutex()
    private val isRetryingAfterError = MutableStateFlow(false)

    override val indexedNotes: Flow<List<IndexedNoteStatistics>> =
        dao.observeIndexedNotes(INDEX_FORMAT_VERSION)
            .map { entries -> entries.map(NoteStatisticsIndexEntity::toDomain) }
            .distinctUntilChanged()

    override val indexState: Flow<NoteStatisticsIndexState> = combine(
        dao.observeIndexCounts(INDEX_FORMAT_VERSION),
        isRetryingAfterError
    ) { counts, isRetrying ->
        NoteStatisticsIndexState(
            indexedNotes = counts.indexedNotes,
            totalNotes = counts.totalNotes,
            isRetryingAfterError = isRetrying
        )
    }

    /** Starts one process-lifetime observer that drains pending index work. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            dao.observeIndexCounts(INDEX_FORMAT_VERSION)
                .map { counts -> counts.indexedNotes < counts.totalNotes }
                .distinctUntilChanged()
                .filter { hasPendingWork -> hasPendingWork }
                .collect { drainIndex() }
        }
    }

    override suspend fun rebuildIndex() {
        dao.clear()
        isRetryingAfterError.value = false
        drainIndex()
    }

    private suspend fun drainIndex() = rebuildMutex.withLock {
        var retryDelayMs = INITIAL_RETRY_DELAY_MS
        var afterNoteId = 0L
        while (true) {
            val batch = try {
                dao.getNextIndexBatch(
                    formatVersion = INDEX_FORMAT_VERSION,
                    afterNoteId = afterNoteId,
                    limit = INDEX_BATCH_SIZE
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                isRetryingAfterError.value = true
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                continue
            }
            if (batch.isEmpty()) {
                val counts = try {
                    dao.getIndexCounts(INDEX_FORMAT_VERSION)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    isRetryingAfterError.value = true
                    delay(retryDelayMs)
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                    continue
                }
                if (counts.indexedNotes < counts.totalNotes && afterNoteId != 0L) {
                    afterNoteId = 0L
                    continue
                }
                isRetryingAfterError.value = false
                return
            }

            try {
                val entries = withContext(computationDispatcher) {
                    batch.map(NoteStatisticsSource::toIndexEntity)
                }
                dao.upsert(entries)
                afterNoteId = batch.last().id
                isRetryingAfterError.value = false
                retryDelayMs = INITIAL_RETRY_DELAY_MS
                yield()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                isRetryingAfterError.value = true
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
            }
        }
    }

    companion object {
        internal const val INDEX_FORMAT_VERSION = 1
        // Sixteen maximum-size editor notes contain about 16 MB of UTF-16 text.
        private const val INDEX_BATCH_SIZE = 16
        private const val INITIAL_RETRY_DELAY_MS = 1_000L
        private const val MAX_RETRY_DELAY_MS = 30_000L
    }
}

internal fun IndexedNoteStatistics.toEntity(): NoteStatisticsIndexEntity =
    NoteStatisticsIndexEntity(
        noteId = noteId,
        creationDate = creationDate,
        sourceLastModifiedDate = sourceLastModifiedDate,
        characterCount = characterCount,
        wordCount = wordCount,
        tagNamesRaw = tagNames.sorted().joinToString("\n"),
        formatVersion = NoteStatisticsRepositoryImpl.INDEX_FORMAT_VERSION
    )

private fun NoteStatisticsSource.toIndexEntity(): NoteStatisticsIndexEntity =
    NoteStatisticsTextAnalyzer.analyze(
        noteId = id,
        content = content,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate
    ).toEntity()

private fun NoteStatisticsIndexEntity.toDomain(): IndexedNoteStatistics =
    IndexedNoteStatistics(
        noteId = noteId,
        creationDate = creationDate,
        sourceLastModifiedDate = sourceLastModifiedDate,
        characterCount = characterCount,
        wordCount = wordCount,
        tagNames = tagNamesRaw.lineSequence().filter(String::isNotBlank).toSet()
    )
