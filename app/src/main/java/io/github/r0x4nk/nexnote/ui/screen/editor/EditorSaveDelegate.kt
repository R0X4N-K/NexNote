package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
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
    private val indexNoteTags: IndexNoteTagsUseCase?,
    private val scope: CoroutineScope,
    private val autosaveDelayMs: Long,
    private val tagIndexDedup: TagIndexDedupPolicy = TagIndexDedupPolicy()
) {
    private val saveMutex = Mutex()
    private var autosaveJob: Job? = null

    fun scheduleAutosave() {
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

    suspend fun flushPendingChanges() {
        NexNoteDebugLog.persistence(
            event = "flushPendingChanges",
            details = uiState.value.debugSaveSummary()
        )
        autosaveJob?.cancel()
        autosaveJob = null
        performSave()
    }

    suspend fun performSave(): Boolean = saveMutex.withLock {
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
            if (snapshot.isTemplateMode) saveAsTemplate(snapshot) else saveAsNote(snapshot)
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

    private fun hasContent(state: EditorUiState): Boolean {
        return state.title.isNotBlank() || state.content.isNotBlank()
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
        "contentVersion=$contentVersion selection=$contentSelectionOffset " +
        "${NexNoteDebugLog.textSummary("title", title)} " +
        NexNoteDebugLog.textSummary("content", content)
}
