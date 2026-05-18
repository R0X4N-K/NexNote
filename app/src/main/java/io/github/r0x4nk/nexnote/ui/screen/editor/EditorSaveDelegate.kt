package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EditorSaveDelegate(
    private val uiState: MutableStateFlow<EditorUiState>,
    private val saveNote: SaveNoteUseCase,
    private val saveTemplate: SaveTemplateUseCase,
    private val saveVaultNote: SaveVaultNoteUseCase?,
    private val indexNoteTags: IndexNoteTagsUseCase?,
    private val scope: CoroutineScope,
    private val autosaveDelayMs: Long,
    private val savesEnabled: Boolean = true,
    private val tagIndexDedup: TagIndexDedupPolicy = TagIndexDedupPolicy()
) {
    private val saveMutex = Mutex()
    private var autosaveJob: Job? = null

    fun cancelPendingAutosave() {
        autosaveJob?.cancel()
        autosaveJob = null
    }

    fun scheduleAutosave() {
        if (!savesEnabled) return
        NexNoteDebugLog.persistence(
            event = "scheduleAutosave",
            details = uiState.value.debugSaveSummary()
        )
        autosaveJob?.cancel()
        autosaveJob = scope.launch {
            delay(autosaveDelayMs)
            performSave()
        }
    }

    suspend fun flushPendingChanges(): Boolean {
        NexNoteDebugLog.persistence(
            event = "flushPendingChanges",
            details = uiState.value.debugSaveSummary()
        )
        cancelPendingAutosave()
        return performSave()
    }

    /**
     * Eagerly persists the current editor state so callers can obtain a real
     * database id, even when the note has no textual content yet.
     *
     * Whereas [performSave] is conservative — it refuses to persist a brand-new
     * note that has neither title, body text, nor images, so transient empty
     * editors do not pollute the database — there are legitimate flows that
     * MUST have a stable id before any user content exists. The canonical
     * example is image insertion: the storage layer needs a [Note.id] to
     * namespace the image file before it can be copied into internal storage.
     *
     * This method is intentionally narrow: it only forces creation of the row
     * when one does not yet exist. If the note is already persisted, or the
     * editor is in template mode, the call is a no-op and the regular autosave
     * pipeline keeps owning subsequent writes.
     *
     * @return `true` when the editor state references a real, persisted row
     *         after the call (either it already did, or one was just created),
     *         `false` if the underlying save failed.
     */
    suspend fun ensurePersisted(): Boolean = saveMutex.withLock {
        if (!savesEnabled) return@withLock false
        val snapshot = uiState.value
        NexNoteDebugLog.persistence(
            event = "ensurePersistedStart",
            details = snapshot.debugSaveSummary()
        )

        // Already has an id (note) or is editing a template — nothing to do.
        // Templates intentionally do not participate in the image flow.
        if (snapshot.isTemplateMode || snapshot.noteId != EditorViewModel.NO_ID) {
            return@withLock true
        }

        uiState.update { it.copy(isSaving = true, errorMessage = null, isDirty = true) }
        return@withLock try {
            if (snapshot.isVaultNote) {
                saveAsVaultNote(snapshot)
            } else {
                saveAsNote(snapshot)
            }
            NexNoteDebugLog.persistence(
                event = "ensurePersistedSuccess",
                details = uiState.value.debugSaveSummary()
            )
            true
        } catch (e: Exception) {
            NexNoteDebugLog.persistence(
                event = "ensurePersistedFailed",
                details = "${NexNoteDebugLog.throwableSummary(e)} ${snapshot.debugSaveSummary()}"
            )
            uiState.update { it.copy(isSaving = false, errorMessage = "Save failed") }
            false
        }
    }

    suspend fun performSave(): Boolean = saveMutex.withLock {
        if (!savesEnabled) {
            autosaveJob?.cancel()
            autosaveJob = null
            uiState.update { it.copy(isSaving = false, isDirty = false) }
            NexNoteDebugLog.persistence(
                event = "performSaveSkipped",
                details = "reason=readOnly ${uiState.value.debugSaveSummary()}"
            )
            return@withLock true
        }
        val snapshot = uiState.value
        val shouldSaveSnapshot = shouldSave(snapshot)
        NexNoteDebugLog.persistence(
            event = "performSaveStart",
            details = "shouldSave=$shouldSaveSnapshot ${snapshot.debugSaveSummary()}"
        )
        if (!snapshot.isDirty || !shouldSaveSnapshot) {
            NexNoteDebugLog.persistence(
                event = "performSaveSkipped",
                details = "dirty=${snapshot.isDirty} shouldSave=$shouldSaveSnapshot " +
                    snapshot.debugSaveSummary()
            )
            return@withLock true
        }

        uiState.update { it.copy(isSaving = true, errorMessage = null) }
        return@withLock try {
            when {
                snapshot.isTemplateMode -> saveAsTemplate(snapshot)
                snapshot.isVaultNote -> saveAsVaultNote(snapshot)
                else -> saveAsNote(snapshot)
            }
            NexNoteDebugLog.persistence(
                event = "performSaveSuccess",
                details = uiState.value.debugSaveSummary()
            )
            if (uiState.value.isDirty) scheduleAutosave()
            true
        } catch (e: Exception) {
            NexNoteDebugLog.persistence(
                event = "performSaveFailed",
                details = "${NexNoteDebugLog.throwableSummary(e)} ${snapshot.debugSaveSummary()}"
            )
            uiState.update { it.copy(isSaving = false, errorMessage = "Save failed") }
            false
        }
    }

    fun flushOnCleared() {
        if (!savesEnabled) return
        val state = uiState.value
        NexNoteDebugLog.persistence(
            event = "flushOnCleared",
            details = "willFlush=${state.isDirty && shouldSave(state)} ${state.debugSaveSummary()}"
        )
        if (state.isDirty && shouldSave(state)) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                runCatching { performSave() }
            }
        }
    }

    private suspend fun saveAsNote(snapshot: EditorUiState) {
        val savedAt = System.currentTimeMillis()
        val note = buildNote(snapshot)
        NexNoteDebugLog.persistence(
            event = "saveAsNoteBeforeRepository",
            details = NexNoteDebugLog.noteSummary("note", note)
        )
        val savedId = saveNote(note)
        maybeIndexNoteTags(savedId, snapshot.content)

        uiState.update { current ->
            val changedDuringSave = EditorSaveChangePolicy.hasUnsavedNoteChanges(
                savedSnapshot = snapshot,
                currentState = current
            )
            NexNoteDebugLog.persistence(
                event = "saveAsNoteAfterRepository",
                details = "savedId=$savedId changedDuringSave=$changedDuringSave " +
                    "current=${current.debugSaveSummary()}"
            )
            current.copy(
                noteId = if (snapshot.noteId == EditorViewModel.NO_ID) savedId else current.noteId,
                lastModifiedDate = savedAt,
                isSaving = false,
                isDirty = changedDuringSave
            )
        }
    }

    private suspend fun saveAsVaultNote(snapshot: EditorUiState) {
        val saveVault = saveVaultNote ?: error("Vault note save use case is not available")
        val savedAt = System.currentTimeMillis()
        val note = buildNote(snapshot).copy(isInVault = true)
        NexNoteDebugLog.persistence(
            event = "saveAsVaultNoteBeforeRepository",
            details = NexNoteDebugLog.noteSummary("note", note)
        )
        val savedId = saveVault(note)

        uiState.update { current ->
            val changedDuringSave = EditorSaveChangePolicy.hasUnsavedNoteChanges(
                savedSnapshot = snapshot,
                currentState = current
            )
            NexNoteDebugLog.persistence(
                event = "saveAsVaultNoteAfterRepository",
                details = "savedId=$savedId changedDuringSave=$changedDuringSave " +
                    "current=${current.debugSaveSummary()}"
            )
            current.copy(
                noteId = if (snapshot.noteId == EditorViewModel.NO_ID) savedId else current.noteId,
                lastModifiedDate = savedAt,
                isSaving = false,
                isDirty = changedDuringSave
            )
        }
    }

    private suspend fun saveAsTemplate(snapshot: EditorUiState) {
        val template = Template(
            id = snapshot.templateId,
            name = snapshot.title.trim().ifBlank { "Template" },
            content = snapshot.content,
            isMarkdown = EDITOR_MARKDOWN_ENABLED,
            isPredefined = false
        )
        NexNoteDebugLog.persistence(
            event = "saveAsTemplateBeforeRepository",
            details = "templateId=${template.id} nameLen=${template.name.length} " +
                NexNoteDebugLog.textSummary("content", template.content)
        )
        val savedId = saveTemplate(template)

        uiState.update { current ->
            val changedDuringSave = EditorSaveChangePolicy.hasUnsavedTemplateChanges(
                savedSnapshot = snapshot,
                currentState = current
            )
            NexNoteDebugLog.persistence(
                event = "saveAsTemplateAfterRepository",
                details = "savedId=$savedId changedDuringSave=$changedDuringSave " +
                    "current=${current.debugSaveSummary()}"
            )
            current.copy(
                templateId = savedId,
                isSaving = false,
                isDirty = changedDuringSave
            )
        }
    }

    /**
     * Re-indexes the note's hashtags only when content has actually changed
     * since the last successful indexing. Saves triggered by title, colour,
     * markdown-mode or preview-mode changes therefore do not re-walk the full
     * content nor touch every existing tag row in the database — a meaningful
     * win on long notes where the autosave runs every [autosaveDelayMs].
     *
     * On failure the dedup hash is forgotten so the next save retries the
     * index, preventing transient errors from leaving the tag table stale.
     */
    private suspend fun maybeIndexNoteTags(savedId: Long, content: String) {
        val indexer = indexNoteTags ?: return
        if (!tagIndexDedup.shouldIndexAndRemember(savedId, content)) {
            NexNoteDebugLog.persistence(
                event = "indexNoteTagsSkipped",
                details = "reason=unchanged savedId=$savedId contentLen=${content.length}"
            )
            return
        }

        try {
            indexer(savedId, content)
        } catch (e: Throwable) {
            tagIndexDedup.forgetLastIndex()
            throw e
        }
    }

    private fun shouldSave(state: EditorUiState): Boolean {
        val isExistingRecord = if (state.isTemplateMode) {
            state.templateId != EditorViewModel.NO_ID
        } else {
            state.noteId != EditorViewModel.NO_ID
        }
        return isExistingRecord || hasContent(state)
    }

    /**
     * A note is considered "non-empty" for persistence purposes if it carries
     * any user-authored content. Images count: a note that only contains
     * inserted pictures is still a meaningful artifact, and once it has been
     * persisted via [ensurePersisted] subsequent autosaves must continue to
     * flush changes (e.g. additional images, colour, pin state) instead of
     * skipping the save because the textual fields are still blank.
     */
    private fun hasContent(state: EditorUiState): Boolean {
        return state.title.isNotBlank() ||
            state.content.isNotBlank() ||
            state.imagePaths.isNotEmpty()
    }

    private fun buildNote(state: EditorUiState): Note = Note(
        id = state.noteId,
        title = state.title,
        content = state.content,
        isMarkdown = EDITOR_MARKDOWN_ENABLED,
        creationDate = state.creationDate,
        lastModifiedDate = state.lastModifiedDate ?: System.currentTimeMillis(),
        timezone = state.timezone,
        isPinned = state.isPinned,
        imagePaths = state.imagePaths,
        backgroundColor = state.backgroundColor,
        isPreviewMode = state.showPreview
    )
}

private fun EditorUiState.debugSaveSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "dirty=$isDirty saving=$isSaving loading=$isLoading preview=$showPreview " +
        "vault=$isVaultNote readOnly=$isReadOnly " +
        "contentVersion=$contentVersion selection=$contentSelectionOffset " +
        "${NexNoteDebugLog.textSummary("title", title, redact = redactContentForLogs)} " +
        NexNoteDebugLog.textSummary("content", content, redact = redactContentForLogs)
}
