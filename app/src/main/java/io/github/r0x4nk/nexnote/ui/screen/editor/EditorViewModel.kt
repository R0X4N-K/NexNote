package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DecryptVaultImageBytesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteImageFileUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNotePreviewModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetThemeModeUseCase
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

// ── ViewModel ─────────────────────────────────────────────────────────────────

class EditorViewModel(
    private val copyNoteImageToInternal: CopyNoteImageToInternalUseCase,
    private val deleteNoteImage: DeleteNoteImageUseCase,
    private val getNoteImageFile: GetNoteImageFileUseCase,
    private val getNoteById: GetNoteByIdUseCase,
    private val getVaultNoteById: GetVaultNoteByIdUseCase? = null,
    private val getTemplateById: GetTemplateByIdUseCase,
    private val saveNote: SaveNoteUseCase,
    private val saveTemplate: SaveTemplateUseCase,
    private val saveVaultNote: SaveVaultNoteUseCase? = null,
    private val setNotePreviewMode: SetNotePreviewModeUseCase,
    observeNoteLinkCandidates: ObserveNoteLinkCandidatesUseCase? = null,
    observeVaultNoteLinkCandidates: ObserveVaultNoteLinkCandidatesUseCase? = null,
    private val observeTagsForNote: ObserveTagsForNoteUseCase? = null,
    private val indexNoteTags: IndexNoteTagsUseCase? = null,
    observeVaultState: ObserveVaultStateUseCase? = null,
    observeThemeMode: ObserveThemeModeUseCase? = null,
    private val setThemeMode: SetThemeModeUseCase? = null,
    private val decryptVaultImageBytesUseCase: DecryptVaultImageBytesUseCase? = null,
    private val initialMode: EditorMode,
    undoHistoryDebounceMs: Long = DEFAULT_UNDO_HISTORY_DEBOUNCE_MS,
    undoHistoryMaxSnapshots: Int = DEFAULT_UNDO_HISTORY_MAX_SNAPSHOTS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditorUiState(
            isLoading = initialMode.startsWithLoading,
            isVaultNote = initialMode.isVaultNote,
            isReadOnly = initialMode.isReadOnly
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val contentHistory = EditorUndoHistory(
        scope = viewModelScope,
        debounceMs = undoHistoryDebounceMs,
        maxStackSize = undoHistoryMaxSnapshots
    )
    internal val undoRedoState: StateFlow<EditorUndoRedoState> = contentHistory.state

    val themeMode: StateFlow<ThemeMode> = (
        observeThemeMode?.invoke() ?: flowOf(ThemeMode.SYSTEM)
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )

    private val tagNavigator = EditorTagNavigator(
        uiState = uiState,
        onError = { message -> _uiState.update { it.copy(errorMessage = message) } }
    )
    val selectedTagsInEditor: StateFlow<String?> = tagNavigator.selectedTagsInEditor
    val tagSearchEvent: SharedFlow<TagSearchState> = tagNavigator.tagSearchEvent

    /**
     * Tags associated with the current note, updated reactively via Room.
     * Emits an empty list until the note is saved for the first time (noteId = 0)
     * or when [tagRepository] is null (unit-test environments).
     */
    val tagsForCurrentNote: StateFlow<List<Tag>> = buildTagsForCurrentNoteFlow(
        uiState = _uiState,
        observeTagsForNote = observeTagsForNote,
        scope = viewModelScope
    )
    internal val noteLinkTargets: StateFlow<List<NoteLinkTarget>> = buildNoteLinkTargetsFlow(
        uiState = _uiState,
        observeNoteLinkCandidates = observeNoteLinkCandidates,
        observeVaultNoteLinkCandidates = observeVaultNoteLinkCandidates,
        scope = viewModelScope
    )

    private val saveDelegate = EditorSaveDelegate(
        uiState = _uiState,
        saveNote = saveNote,
        saveTemplate = saveTemplate,
        saveVaultNote = saveVaultNote,
        indexNoteTags = indexNoteTags,
        scope = viewModelScope,
        autosaveDelayMs = AUTOSAVE_DELAY_MS,
        savesEnabled = !initialMode.isReadOnly
    )
    private val imageActions = EditorImageActions(
        uiState = _uiState,
        copyNoteImageToInternal = copyNoteImageToInternal,
        deleteNoteImage = deleteNoteImage,
        saveDelegate = saveDelegate,
        recordContentHistoryChange = ::recordImmediateContentHistoryChange,
        scope = viewModelScope
    )
    private val loadDelegate = EditorLoadDelegate(
        uiState = _uiState,
        getNoteById = getNoteById,
        getVaultNoteById = getVaultNoteById,
        getTemplateById = getTemplateById,
        scheduleAutosave = { scheduleAutosave() },
        resetContentHistory = ::resetContentHistory
    )

    init {
        NexNoteDebugLog.viewModel(
            event = "init",
            details = initialMode.debugRouteSummary()
        )
        viewModelScope.launch {
            loadDelegate.loadInitial(initialMode)
        }
        if (initialMode.isVaultNote && observeVaultState != null) {
            viewModelScope.launch {
                observeVaultState().collect { state ->
                    if (state != VaultState.UNLOCKED) {
                        lockVaultEditor(state)
                    }
                }
            }
        }
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onTitleChange(value: String) {
        if (ignoreReadOnlyChange("onTitleChange")) return
        val redact = _uiState.value.redactContentForLogs
        NexNoteDebugLog.viewModel(
            event = "onTitleChange",
            details = "${NexNoteDebugLog.textSummary("newTitle", value, redact = redact)} " +
                uiState.value.debugViewModelSummary()
        )
        _uiState.update { it.copy(title = value, isDirty = true, errorMessage = null) }
        scheduleAutosave()
    }

    fun onContentChange(value: String, selectionOffset: Int? = null) {
        if (ignoreReadOnlyChange("onContentChange")) return
        val redact = _uiState.value.redactContentForLogs
        NexNoteDebugLog.viewModel(
            event = "onContentChangeReceived",
            details = "selection=$selectionOffset " +
                "${NexNoteDebugLog.textSummary("newContent", value, redact = redact)} " +
                uiState.value.debugViewModelSummary()
        )
        // Reject pastes or inputs that would exceed the safe layout limit.
        if (value.length > MAX_CONTENT_LENGTH) {
            NexNoteDebugLog.viewModel(
                event = "onContentChangeRejectedTooLong",
                details = "selection=$selectionOffset " +
                    NexNoteDebugLog.textSummary("newContent", value, redact = redact)
            )
            _uiState.update {
                it.copy(errorMessage = "Text too long (max ${MAX_CONTENT_LENGTH / 1_000}k characters)")
            }
            return
        }

        val safeSelectionOffset = selectionOffset?.coerceIn(0, value.length)
        if (value == _uiState.value.content) {
            NexNoteDebugLog.viewModel(
                event = "onContentChangeSelectionOnly",
                details = "safeSelection=$safeSelectionOffset ${uiState.value.debugViewModelSummary()}"
            )
            contentHistory.updateCurrentSelection(safeSelectionOffset)
            return
        }

        contentHistory.recordUserChange(
            EditorContentSnapshot(text = value, selectionOffset = safeSelectionOffset)
        )
        _uiState.update {
            it.copy(
                content = value,
                isDirty = true,
                errorMessage = null,
                contentSelectionOffset = safeSelectionOffset
            )
        }
        NexNoteDebugLog.viewModel(
            event = "onContentChangeApplied",
            details = "safeSelection=$safeSelectionOffset ${uiState.value.debugViewModelSummary()}"
        )
        scheduleAutosave()
    }

    fun onContentSelectionChange(selectionOffset: Int?) {
        val safeSelectionOffset = selectionOffset?.coerceIn(0, _uiState.value.content.length)
        contentHistory.updateCurrentSelection(safeSelectionOffset)
    }

    fun undoContentChange() {
        if (ignoreReadOnlyChange("undoContentChange")) return
        NexNoteDebugLog.viewModel(event = "undoContentChange", details = uiState.value.debugViewModelSummary())
        contentHistory.undo()?.let(::applyHistorySnapshot)
    }

    fun redoContentChange() {
        if (ignoreReadOnlyChange("redoContentChange")) return
        NexNoteDebugLog.viewModel(event = "redoContentChange", details = uiState.value.debugViewModelSummary())
        contentHistory.redo()?.let(::applyHistorySnapshot)
    }

    fun clearContentHistory() {
        NexNoteDebugLog.viewModel(event = "clearContentHistory", details = uiState.value.debugViewModelSummary())
        contentHistory.clear()
    }

    fun onCreationDateChange(newTimestamp: Long) {
        if (ignoreReadOnlyChange("onCreationDateChange")) return
        NexNoteDebugLog.viewModel(
            event = "onCreationDateChange",
            details = "newTimestamp=$newTimestamp ${uiState.value.debugViewModelSummary()}"
        )
        _uiState.update { it.copy(creationDate = newTimestamp, isDirty = true) }
        scheduleAutosave()
    }

    fun onBackgroundColorChange(color: Int?) {
        if (ignoreReadOnlyChange("onBackgroundColorChange")) return
        NexNoteDebugLog.viewModel(
            event = "onBackgroundColorChange",
            details = "color=$color ${uiState.value.debugViewModelSummary()}"
        )
        _uiState.update { it.copy(backgroundColor = color, isDirty = true) }
        scheduleAutosave()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun lockVaultEditor(vaultState: VaultState) {
        val current = _uiState.value
        if (!current.isVaultNote || current.isVaultLocked) return

        NexNoteDebugLog.viewModel(
            event = "lockVaultEditor",
            details = "vaultState=$vaultState noteId=${current.noteId} dirty=${current.isDirty}"
        )
        saveDelegate.cancelPendingAutosave()
        contentHistory.clear()
        _uiState.update {
            it.copy(
                title = "",
                content = "",
                imagePaths = emptyList(),
                isVaultLocked = true,
                isReadOnly = true,
                isDirty = false,
                isSaving = false,
                errorMessage = "Vault locked",
                showPreview = false,
                openedDirectlyInPreview = false,
                openedDirectlyInEdit = false,
                contentVersion = it.contentVersion + 1,
                contentSelectionOffset = null
            )
        }
    }

    private fun ignoreReadOnlyChange(event: String): Boolean {
        if (!_uiState.value.isReadOnly) return false
        NexNoteDebugLog.viewModel(
            event = event,
            details = "ignored=readOnlyVaultNote noteId=${_uiState.value.noteId}"
        )
        return true
    }

    fun onTagChipClick(tagName: String) {
        tagNavigator.onTagChipClick(tagName)
    }

    fun clearTagSelectionInEditor() {
        tagNavigator.clearSelection()
    }

    // ── Images ────────────────────────────────────────────────────────────────

    /**
     * Handles an image selected via the Photo Picker.
     *
     * 1. If the note has no id yet, saves it eagerly to obtain one.
     * 2. Copies the image to internal storage.
     * 3. Inserts `![image](relativePath)` at [insertionOffset].
     * 4. Updates [EditorUiState.imagePaths] and schedules autosave.
     *
     * Not available in template-editing mode (templates store no images).
     */
    fun onImagePicked(
        openImageInputStream: () -> InputStream?,
        insertionOffset: Int? = null
    ) {
        if (ignoreReadOnlyChange("onImagePicked")) return
        imageActions.onImagePicked(openImageInputStream, insertionOffset)
    }

    /**
     * Removes an image from the note: deletes the physical file and strips its
     * Markdown tag from the content.
     */
    fun onRemoveImage(relativePath: String) {
        if (ignoreReadOnlyChange("onRemoveImage")) return
        imageActions.onRemoveImage(relativePath)
    }

    fun getImageFile(relativePath: String): File {
        return getNoteImageFile(relativePath)
    }

    /**
     * Returns the decrypted bytes of a Vault image referenced by the current
     * note, or `null` when the note is not a Vault note, the Vault is locked,
     * the relative path is blank or not referenced by the current note, the
     * decryption use case is not wired, the file is missing on disk, or any
     * other recoverable failure occurs.
     *
     * The bytes are never logged, persisted in plaintext or returned outside
     * the unlocked Vault scope: this method is only invoked by the preview
     * pipeline while the editor is in an unlocked Vault state and is dropped
     * from the consumer as soon as the bitmap decode completes.
     */
    suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? {
        val normalizedPath = relativePath.trim()
        if (normalizedPath.isBlank()) return null
        val current = _uiState.value
        if (!current.isVaultNote || current.isVaultLocked) return null
        if (normalizedPath !in current.imagePaths) return null
        val useCase = decryptVaultImageBytesUseCase ?: return null
        return runCatching { useCase(normalizedPath) }.getOrNull()
    }

    // ── Preview toggle ────────────────────────────────────────────────────────

    /**
     * Switches between edit mode and Markdown preview.
     *
     * The new mode is persisted immediately via a lightweight targeted update so
     * that the note reopens in the same view the user left it in. [lastModifiedDate]
     * is intentionally NOT touched — toggling the view is not a content edit.
     * For unsaved notes ([noteId] == [NO_ID]) the mode lives in memory only; it
     * will be persisted the first time the note is saved.
     */
    fun togglePreview() {
        val current = _uiState.value
        val newValue = !current.showPreview
        NexNoteDebugLog.viewModel(
            event = "togglePreview",
            details = "newValue=$newValue ${current.debugViewModelSummary()}"
        )
        _uiState.update {
            it.copy(
                showPreview = newValue,
                openedDirectlyInPreview = false,
                openedDirectlyInEdit = false,
                isDirty = if (current.isVaultNote) true else it.isDirty
            )
        }
        if (current.isReadOnly) return
        if (current.isVaultNote) {
            scheduleAutosave()
            return
        }
        if (!current.isTemplateMode && current.noteId != NO_ID) {
            viewModelScope.launch { setNotePreviewMode(current.noteId, newValue) }
        }
    }

    fun toggleTheme(isDarkTheme: Boolean) {
        val nextThemeMode = if (isDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK
        viewModelScope.launch { setThemeMode?.invoke(nextThemeMode) }
    }

    // ── Saving ────────────────────────────────────────────────────────────────

    /**
     * Explicit save triggered on back navigation.
     * Cancels any pending autosave before performing an immediate save.
     */
    suspend fun flushPendingChanges() {
        NexNoteDebugLog.viewModel(event = "flushPendingChanges", details = uiState.value.debugViewModelSummary())
        saveDelegate.flushPendingChanges()
    }

    private fun scheduleAutosave() {
        if (_uiState.value.isReadOnly) return
        saveDelegate.scheduleAutosave()
    }

    private fun resetContentHistory(content: String, selectionOffset: Int?) {
        NexNoteDebugLog.viewModel(
            event = "resetContentHistory",
            details = "selection=$selectionOffset " +
                NexNoteDebugLog.textSummary(
                    "content",
                    content,
                    redact = _uiState.value.redactContentForLogs
                )
        )
        contentHistory.reset(
            EditorContentSnapshot(text = content, selectionOffset = selectionOffset)
        )
    }

    private fun recordImmediateContentHistoryChange(
        previous: EditorContentSnapshot,
        next: EditorContentSnapshot
    ) {
        if (_uiState.value.isReadOnly) return
        contentHistory.recordImmediateChange(previous, next)
    }

    private fun applyHistorySnapshot(snapshot: EditorContentSnapshot) {
        if (ignoreReadOnlyChange("applyHistorySnapshot")) return
        NexNoteDebugLog.viewModel(
            event = "applyHistorySnapshot",
            details = "selection=${snapshot.selectionOffset} " +
                NexNoteDebugLog.textSummary(
                    "snapshot",
                    snapshot.text,
                    redact = _uiState.value.redactContentForLogs
                )
        )
        _uiState.update { current ->
            current.copy(
                content = snapshot.text,
                isDirty = true,
                errorMessage = null,
                contentVersion = current.contentVersion + 1,
                contentSelectionOffset = snapshot.selectionOffset?.coerceIn(0, snapshot.text.length)
            )
        }
        scheduleAutosave()
    }

    /**
     * Defensive fallback: only runs if flushPendingChanges() was never called.
     * Uses a separate scope because viewModelScope is already cancelled here.
     */
    override fun onCleared() {
        NexNoteDebugLog.viewModel(event = "onCleared", details = uiState.value.debugViewModelSummary())
        saveDelegate.flushOnCleared()
        contentHistory.clear()
        super.onCleared()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        /** Sentinel value: note/template not yet persisted in the database. */
        const val NO_ID = EditorMode.NO_ID

        /**
         * editTemplateId sentinel: open the editor to create a brand-new
         * template (title = name, content = body, saved to templateRepository).
         */
        const val NEW_TEMPLATE_ID = EditorMode.NEW_TEMPLATE_ID

        private const val AUTOSAVE_DELAY_MS = 1_500L

        fun factory(
            mode: EditorMode
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val useCases = app.useCases
                EditorViewModel(
                    copyNoteImageToInternal = useCases.images.copyNoteImageToInternal,
                    deleteNoteImage      = useCases.images.deleteNoteImage,
                    getNoteImageFile     = useCases.images.getNoteImageFile,
                    getNoteById           = useCases.notes.getNoteById,
                    getVaultNoteById      = useCases.vault.getVaultNoteById,
                    getTemplateById       = useCases.templates.getTemplateById,
                    saveNote              = useCases.notes.saveNote,
                    saveTemplate          = useCases.templates.saveTemplate,
                    saveVaultNote         = useCases.vault.saveVaultNote,
                    setNotePreviewMode    = useCases.notes.setNotePreviewMode,
                    observeNoteLinkCandidates = useCases.notes.observeNoteLinkCandidates,
                    observeVaultNoteLinkCandidates =
                        useCases.vault.observeVaultNoteLinkCandidates,
                    observeTagsForNote    = useCases.tags.observeTagsForNote,
                    indexNoteTags         = useCases.tags.indexNoteTags,
                    observeVaultState     = useCases.vault.observeVaultState,
                    observeThemeMode      = useCases.preferences.observeThemeMode,
                    setThemeMode          = useCases.preferences.setThemeMode,
                    decryptVaultImageBytesUseCase = useCases.vault.decryptVaultImageBytes,
                    initialMode           = mode
                )
            }
        }
    }
}

private fun EditorUiState.debugViewModelSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "loading=$isLoading dirty=$isDirty saving=$isSaving " +
        "vault=$isVaultNote readOnly=$isReadOnly " +
        "preview=$showPreview openedDirectlyInPreview=$openedDirectlyInPreview " +
        "openedDirectlyInEdit=$openedDirectlyInEdit " +
        "contentVersion=$contentVersion selection=$contentSelectionOffset " +
        "${NexNoteDebugLog.textSummary("title", title, redact = redactContentForLogs)} " +
        NexNoteDebugLog.textSummary("content", content, redact = redactContentForLogs)
}
