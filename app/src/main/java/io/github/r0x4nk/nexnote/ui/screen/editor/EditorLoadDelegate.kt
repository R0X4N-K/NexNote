package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.util.DateUtils
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class EditorLoadDelegate(
    private val uiState: MutableStateFlow<EditorUiState>,
    private val getNoteById: GetNoteByIdUseCase,
    private val getVaultNoteById: GetVaultNoteByIdUseCase?,
    private val getTemplateById: GetTemplateByIdUseCase,
    private val scheduleAutosave: () -> Unit,
    private val resetContentHistory: (content: String, selectionOffset: Int?) -> Unit
) {
    suspend fun loadInitial(mode: EditorMode) {
        NexNoteDebugLog.viewModel(
            event = "loadInitial",
            details = mode.debugRouteSummary()
        )
        when (mode) {
            is EditorMode.EditTemplate -> loadTemplateForEdit(mode.templateId)
            is EditorMode.ExistingNote -> loadNote(mode.noteId)
            EditorMode.NewVaultNote -> finishEmptyEditorLoad(null)
            is EditorMode.VaultNote -> loadVaultNote(mode.noteId)
            is EditorMode.NewFromTemplate -> loadTemplate(mode.templateId, isVaultNote = false)
            is EditorMode.NewVaultFromTemplate ->
                loadTemplate(mode.templateId, isVaultNote = true)
            is EditorMode.NewNote -> finishEmptyEditorLoad(mode.initialCreationDate)
            EditorMode.NewTemplate -> startNewTemplate()
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
            showPreview = note.isPreviewMode,
            openedDirectlyInPreview = note.isPreviewMode,
            openedDirectlyInEdit = !note.isPreviewMode,
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

    private suspend fun loadTemplate(id: Long, isVaultNote: Boolean) {
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
            isVaultNote = isVaultNote,
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

    private fun startNewTemplate() {
        NexNoteDebugLog.viewModel(event = "startNewTemplate")
        uiState.update { it.copy(isTemplateMode = true, isLoading = false) }
        resetContentHistory("", null)
    }

    private suspend fun loadVaultNote(id: Long) {
        NexNoteDebugLog.viewModel(event = "loadVaultNoteStart", details = "vaultNoteId=$id")
        val note = getVaultNoteById?.invoke(id)
        if (note == null || !note.isInVault) {
            NexNoteDebugLog.viewModel(event = "loadVaultNoteMissing", details = "vaultNoteId=$id")
            uiState.update { it.copy(isLoading = false, errorMessage = "Vault note not available") }
            return
        }
        NexNoteDebugLog.viewModel(
            event = "loadVaultNoteResult",
            details = NexNoteDebugLog.noteSummary("note", note)
        )

        val loadedState = EditorUiState(
            noteId = note.id,
            isLoading = false,
            title = note.title,
            content = note.content,
            showPreview = note.isPreviewMode,
            openedDirectlyInPreview = note.isPreviewMode,
            openedDirectlyInEdit = !note.isPreviewMode,
            creationDate = note.creationDate,
            lastModifiedDate = note.lastModifiedDate,
            timezone = note.timezone,
            isPinned = note.isPinned,
            imagePaths = note.imagePaths,
            backgroundColor = note.backgroundColor,
            isVaultNote = true,
            isDirty = false,
            contentVersion = 1
        )
        uiState.update { loadedState }
        NexNoteDebugLog.viewModel(
            event = "loadVaultNoteStateApplied",
            details = loadedState.debugLoadSummary()
        )
        resetContentHistory(loadedState.content, loadedState.contentSelectionOffset)
    }

    private suspend fun loadTemplateForEdit(editTemplateId: Long) {
        NexNoteDebugLog.viewModel(
            event = "loadTemplateForEditStart",
            details = "editTemplateId=$editTemplateId"
        )
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

    private fun finishEmptyEditorLoad(initialCreationDate: Long?) {
        NexNoteDebugLog.viewModel(
            event = "finishEmptyEditorLoad",
            details = "initialCreationDate=$initialCreationDate"
        )
        uiState.update { current ->
            if (initialCreationDate == null) {
                current.copy(isLoading = false)
            } else {
                current.copy(
                    isLoading = false,
                    creationDate = initialCreationDate
                )
            }
        }
    }
}

private fun EditorUiState.debugLoadSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "loading=$isLoading dirty=$isDirty preview=$showPreview " +
        "vault=$isVaultNote readOnly=$isReadOnly " +
        "contentVersion=$contentVersion " +
        NexNoteDebugLog.textSummary("content", content, redact = redactContentForLogs)
}
