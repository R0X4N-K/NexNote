package io.github.r0x4nk.nexnote.data.repository

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.util.DateUtils
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.util.SearchUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn

/**
 * Room-backed implementation of the domain [NoteRepository] contract.
 *
 * The `Impl` suffix keeps the data-layer class distinct from the domain
 * interface at call sites while preserving the repository role and API shape.
 */
class NoteRepositoryImpl(
    private val dao: NoteDao,
    private val imageStorage: NoteImageStorage,
    appScope: CoroutineScope? = null
) : NoteRepository {

    private val creationDateSnapshots: Flow<List<Long>> =
        dao.getAllCreationDates()
            .distinctUntilChanged()
            .let { source ->
                appScope?.let {
                    source.shareIn(
                        scope = it,
                        started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MS),
                        replay = 1
                    )
                } ?: source
            }

    /** Active notes: pinned first, then newest-modified first. */
    override val allNotes: Flow<List<Note>> =
        dao.getAllNotes()
            .distinctUntilChanged()
            .onEach { list ->
                NexNoteDebugLog.repository(event = "allNotesDaoEmission") {
                    list.debugEntityListSummary()
                }
            }
            .map { list -> list.map { it.toDomain() } }

    /** Active notes: pinned first, then oldest-modified first. */
    override val allNotesSortedAsc: Flow<List<Note>> =
        dao.getAllNotesSortedAsc()
            .distinctUntilChanged()
            .onEach { list ->
                NexNoteDebugLog.repository(event = "allNotesSortedAscDaoEmission") {
                    list.debugEntityListSummary()
                }
            }
            .map { list -> list.map { it.toDomain() } }

    /** Notes in the trash, ordered by deletion date newest first. */
    override val deletedNotes: Flow<List<Note>> =
        dao.getDeletedNotes()
            .distinctUntilChanged()
            .map { list -> list.map { it.toDomain() } }

    /** Lightweight active notes used by note-link autocomplete and validation. */
    override val noteLinkCandidates: Flow<List<NoteLinkCandidate>> =
        dao.getNoteLinkCandidates()
            .distinctUntilChanged()
            .map { list -> list.map { it.toDomain() } }

    /**
     * Set of UTC-midnight timestamps (ms) for each day that has at least one
     * active note. Used by the agenda screen to highlight days with notes.
     */
    override val distinctActiveDays: Flow<Set<Long>> =
        creationDateSnapshots
            .map { dates -> dates.map { startOfDayUtc(it) }.toSet() }

    /**
     * Set of device-local start-of-day timestamps (ms) for each day that has
     * at least one active note. Used by AgendaViewModel to place calendar dots
     * in the device's local timezone rather than UTC.
     */
    override val distinctLocalDays: Flow<Set<Long>> =
        creationDateSnapshots
            .map { dates -> dates.map { DateUtils.startOfDay(it) }.toSet() }

    /** Text search; advanced ranking is delegated to SearchUtils after the query. */
    override fun searchNotes(query: String): Flow<List<Note>> =
        dao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    /**
     * Weighted search: the DAO filters with SQL LIKE, then SearchUtils ranks by
     * score (title match ×3, content match ×1, prefix bonus +5).
     */
    override fun searchNotesScored(query: String): Flow<List<ScoredNote>> =
        dao.searchNotes(query).map { list ->
            SearchUtils.scoreAndRank(list.map { it.toDomain() }, query)
        }

    /** Active notes whose creationDate falls in [startMs, endMs). */
    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<Note>> =
        dao.getNotesByDateRange(startMs, endMs).map { list -> list.map { it.toDomain() } }

    override suspend fun getNoteById(id: Long): Note? {
        val note = dao.getNoteById(id)?.toDomain()
        NexNoteDebugLog.repository(event = "getNoteById") {
            "requestedId=$id ${NexNoteDebugLog.noteSummary("note", note)}"
        }
        return note
    }

    /**
     * Saves a note. Inserts when id == 0 (returns the new id); otherwise updates
     * and returns the existing id. The repository always stamps lastModifiedDate
     * with the current time — the caller must not set it.
     */
    override suspend fun saveNote(note: Note): Long {
        val now = System.currentTimeMillis()
        NexNoteDebugLog.repository(event = "saveNoteStart") {
            "mode=${if (note.id == 0L) "insert" else "update"} " +
                NexNoteDebugLog.noteSummary("note", note)
        }
        val savedId = if (note.id == 0L) {
            dao.insertNote(note.toEntity(lastModifiedDate = now))
        } else {
            dao.updateNote(note.toEntity(lastModifiedDate = now))
            note.id
        }
        val storedNote = dao.getNoteById(savedId)?.toDomain()
        NexNoteDebugLog.repository(event = "saveNoteStored") {
            "savedId=$savedId ${NexNoteDebugLog.noteSummary("stored", storedNote)}"
        }
        return savedId
    }

    override suspend fun moveToTrash(id: Long) =
        dao.moveToTrash(id, System.currentTimeMillis())

    override suspend fun restoreFromTrash(id: Long) =
        dao.restoreFromTrash(id)

    /**
     * Permanently deletes a trashed note and cleans up its internal image files.
     * The DAO still enforces `isDeleted = 1`, so active notes remain protected.
     */
    override suspend fun deleteNotePermanently(id: Long) {
        val imagePaths = dao.getNoteById(id)
            ?.takeIf { it.isDeleted }
            ?.imagePaths()
            .orEmpty()
        val deletedRows = dao.deleteNotePermanently(id)
        if (deletedRows > 0) deleteImages(imagePaths)
    }

    override suspend fun emptyTrash() {
        val imagePaths = dao.getDeletedImagePathsRaw()
            .flatMap(::parseImagePaths)
        val deletedRows = dao.emptyTrash()
        if (deletedRows > 0) deleteImages(imagePaths)
    }

    override suspend fun setPinned(id: Long, isPinned: Boolean) =
        dao.setPinned(id, isPinned)

    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) =
        dao.setPreviewMode(id, isPreviewMode)

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun NoteEntity.toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        isMarkdown = isMarkdown,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate,
        timezone = timezone,
        isDeleted = isDeleted,
        deletedDate = deletedDate,
        isInVault = isInVault,
        isPinned = isPinned,
        imagePaths = imagePaths(),
        backgroundColor = backgroundColor,
        isPreviewMode = isPreviewMode
    )

    private fun NoteLinkCandidateProjection.toDomain(): NoteLinkCandidate =
        NoteLinkCandidate(
            id = id,
            title = title
        )

    private fun Note.toEntity(lastModifiedDate: Long): NoteEntity = NoteEntity(
        id = id,
        title = title,
        content = content,
        isMarkdown = isMarkdown,
        creationDate = creationDate,
        lastModifiedDate = lastModifiedDate,
        timezone = timezone,
        isDeleted = isDeleted,
        deletedDate = deletedDate,
        isInVault = isInVault,
        isPinned = isPinned,
        imagePathsRaw = imagePaths.filter { it.isNotBlank() }.joinToString("\n"),
        backgroundColor = backgroundColor,
        isPreviewMode = isPreviewMode
    )

    private suspend fun deleteImages(imagePaths: List<String>) {
        imagePaths
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { relativePath ->
                runCatching { imageStorage.deleteImage(relativePath) }
                    .onFailure { error ->
                        NexNoteDebugLog.repositoryWarning(event = "imageCleanupFailed") {
                            "path=$relativePath ${NexNoteDebugLog.throwableSummary(error)}"
                        }
                    }
            }
    }

    private fun NoteEntity.imagePaths(): List<String> =
        parseImagePaths(imagePathsRaw)

    private fun parseImagePaths(raw: String): List<String> =
        raw.split('\n').filter { it.isNotBlank() }

    // ── Utility ──────────────────────────────────────────────────────────────

    /** Returns the UTC midnight timestamp for the day that contains [millis]. */
    private fun startOfDayUtc(millis: Long): Long =
        (millis / MS_PER_DAY) * MS_PER_DAY

    private fun List<NoteEntity>.debugEntityListSummary(): String {
        return buildString {
            append("count=").append(size)
            this@debugEntityListSummary.take(8).forEachIndexed { index, note ->
                append(" item").append(index)
                    .append(".id=").append(note.id)
                    .append(" item").append(index)
                    .append(".vault=").append(note.isInVault)
                if (note.isInVault) {
                    append(" item").append(index)
                        .append(".content=redacted")
                    return@forEachIndexed
                }
                append(" item").append(index)
                    .append(".contentLen=").append(note.content.length)
                    .append(" item").append(index)
                    .append(".contentHash=").append(note.content.hashCode())
            }
        }
    }

    companion object {
        private const val MS_PER_DAY = 86_400_000L
        private const val FLOW_STOP_TIMEOUT_MS = 5_000L
    }
}
