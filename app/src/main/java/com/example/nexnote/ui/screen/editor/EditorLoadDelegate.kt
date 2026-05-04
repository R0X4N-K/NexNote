package com.example.nexnote.ui.screen.editor

import com.example.nexnote.domain.usecase.GetNoteByIdUseCase
import com.example.nexnote.domain.usecase.GetTemplateByIdUseCase
import com.example.nexnote.util.DateUtils
import com.example.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class EditorLoadDelegate(
    private val uiState: MutableStateFlow<EditorUiState>,
    private val getNoteById: GetNoteByIdUseCase,
    private val getTemplateById: GetTemplateByIdUseCase,
    private val scheduleAutosave: () -> Unit,
    private val resetContentHistory: (content: String, selectionOffset: Int?) -> Unit
) {
    suspend fun loadInitial(
        noteId: Long,
        templateId: Long,
        editTemplateId: Long
    ) {
        NexNoteDebugLog.viewModel(
            event = "loadInitial",
            details = "routeNoteId=$noteId routeTemplateId=$templateId " +
                "routeEditTemplateId=$editTemplateId"
        )
        when {
            editTemplateId != EditorViewModel.NO_ID -> loadTemplateForEdit(editTemplateId)
            noteId != EditorViewModel.NO_ID -> loadNote(noteId)
            templateId != EditorViewModel.NO_ID -> loadTemplate(templateId)
            else -> finishEmptyEditorLoad()
        }
    }

    private suspend fun loadNote(id: Long) {
        NexNoteDebugLog.viewModel(event = "loadNoteStart", details = "noteId=$id")
        val note = getNoteById(id)
        if (note == null) {
            NexNoteDebugLog.viewModel(event = "loadNoteMissing", details = "noteId=$id")
            uiState.update { it.copy(isLoading = false, errorMessage = "Note not found") }
            return
        }
        NexNoteDebugLog.viewModel(
            event = "loadNoteResult",
            details = NexNoteDebugLog.noteSummary("note", note)
        )

        val loadedState = EditorUiState(
            noteId = note.id,
            isLoading = false,
            title = note.title,
            content = note.content,
            isMarkdown = note.isMarkdown,
            showPreview = note.isPreviewMode,
            openedDirectlyInPreview = note.isPreviewMode,
            creationDate = note.creationDate,
            lastModifiedDate = note.lastModifiedDate,
            timezone = note.timezone,
            isPinned = note.isPinned,
            imagePaths = note.imagePaths,
            backgroundColor = note.backgroundColor,
            isDirty = false,
            contentVersion = 1
        )
        uiState.update { loadedState }
        NexNoteDebugLog.viewModel(
            event = "loadNoteStateApplied",
            details = loadedState.debugLoadSummary()
        )
        resetContentHistory(loadedState.content, loadedState.contentSelectionOffset)
    }

    private suspend fun loadTemplate(id: Long) {
        NexNoteDebugLog.viewModel(event = "loadTemplateStart", details = "templateId=$id")
        val template = getTemplateById(id)
        if (template == null) {
            NexNoteDebugLog.viewModel(event = "loadTemplateMissing", details = "templateId=$id")
            uiState.update { it.copy(isLoading = false) }
            return
        }
        NexNoteDebugLog.viewModel(
            event = "loadTemplateResult",
            details = "templateId=${template.id} nameLen=${template.name.length} " +
                NexNoteDebugLog.textSummary("content", template.content)
        )

        val dateStr = DateUtils.formatDate(System.currentTimeMillis())
        val loadedState = EditorUiState(
            noteId = EditorViewModel.NO_ID,
            isLoading = false,
            content = template.content.replace("{{date}}", dateStr),
            isMarkdown = template.isMarkdown,
            isDirty = true,
            contentVersion = 1
        )
        uiState.update { loadedState }
        NexNoteDebugLog.viewModel(
            event = "loadTemplateStateApplied",
            details = loadedState.debugLoadSummary()
        )
        resetContentHistory(loadedState.content, loadedState.contentSelectionOffset)
        scheduleAutosave()
    }

    private suspend fun loadTemplateForEdit(editTemplateId: Long) {
        NexNoteDebugLog.viewModel(
            event = "loadTemplateForEditStart",
            details = "editTemplateId=$editTemplateId"
        )
        if (editTemplateId == EditorViewModel.NEW_TEMPLATE_ID) {
            uiState.update { it.copy(isTemplateMode = true, isLoading = false) }
            resetContentHistory("", null)
            return
        }

        val template = getTemplateById(editTemplateId)
        if (template == null) {
            NexNoteDebugLog.viewModel(
                event = "loadTemplateForEditMissing",
                details = "editTemplateId=$editTemplateId"
            )
            uiState.update { it.copy(isLoading = false, errorMessage = "Template not found") }
            return
        }
        NexNoteDebugLog.viewModel(
            event = "loadTemplateForEditResult",
            details = "templateId=${template.id} nameLen=${template.name.length} " +
                NexNoteDebugLog.textSummary("content", template.content)
        )

        val loadedState = EditorUiState(
            isTemplateMode = true,
            isLoading = false,
            templateId = template.id,
            title = template.name,
            content = template.content,
            isMarkdown = template.isMarkdown,
            isDirty = false,
            contentVersion = 1
        )
        uiState.update { loadedState }
        NexNoteDebugLog.viewModel(
            event = "loadTemplateForEditStateApplied",
            details = loadedState.debugLoadSummary()
        )
        resetContentHistory(loadedState.content, loadedState.contentSelectionOffset)
    }

    private fun finishEmptyEditorLoad() {
        NexNoteDebugLog.viewModel(event = "finishEmptyEditorLoad")
        uiState.update { it.copy(isLoading = false) }
    }
}

private fun EditorUiState.debugLoadSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "loading=$isLoading dirty=$isDirty preview=$showPreview " +
        "contentVersion=$contentVersion " +
        NexNoteDebugLog.textSummary("content", content)
}
