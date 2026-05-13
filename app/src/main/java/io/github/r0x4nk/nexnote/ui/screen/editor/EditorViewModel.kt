package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteImageFileUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
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
    private val getTemplateById: GetTemplateByIdUseCase,
    private val saveNote: SaveNoteUseCase,
    private val saveTemplate: SaveTemplateUseCase,
    private val setNotePreviewMode: SetNotePreviewModeUseCase,
    observeNoteLinkCandidates: ObserveNoteLinkCandidatesUseCase? = null,
    private val observeTagsForNote: ObserveTagsForNoteUseCase? = null,
    private val indexNoteTags: IndexNoteTagsUseCase? = null,
    observeThemeMode: ObserveThemeModeUseCase? = null,
    private val setThemeMode: SetThemeModeUseCase? = null,
    private val initialMode: EditorMode,
    undoHistoryDebounceMs: Long = DEFAULT_UNDO_HISTORY_DEBOUNCE_MS,
    undoHistoryMaxSnapshots: Int = DEFAULT_UNDO_HISTORY_MAX_SNAPSHOTS
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditorUiState(
            isLoading = initialMode.startsWithLoading
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
        scope = viewModelScope
    )

    private val saveDelegate = EditorSaveDelegate(
        uiState = _uiState,
        saveNote = saveNote,
        saveTemplate = saveTemplate,
        indexNoteTags = indexNoteTags,
        scope = viewModelScope,
        autosaveDelayMs = AUTOSAVE_DELAY_MS
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
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun onTitleChange(value: String) {
        NexNoteDebugLog.viewModel(
            event = "onTitleChange",
            details = "${NexNoteDebugLog.textSummary("newTitle", value)} " +
                uiState.value.debugViewModelSummary()
        )
        _uiState.update { it.copy(title = value, isDirty = true, errorMessage = null) }
        scheduleAutosave()
    }

    fun onContentChange(value: String, selectionOffset: Int? = null) {
        NexNoteDebugLog.viewModel(
            event = "onContentChangeReceived",
            details = "selection=$selectionOffset ${NexNoteDebugLog.textSummary("newContent", value)} " +
                uiState.value.debugViewModelSummary()
        )
        // Reject pastes or inputs that would exceed the safe layout limit.
        if (value.length > MAX_CONTENT_LENGTH) {
            NexNoteDebugLog.viewModel(
                event = "onContentChangeRejectedTooLong",
                details = "selection=$selectionOffset ${NexNoteDebugLog.textSummary("newContent", value)}"
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
        NexNoteDebugLog.viewModel(event = "undoContentChange", details = uiState.value.debugViewModelSummary())
        contentHistory.undo()?.let(::applyHistorySnapshot)
    }

    fun redoContentChange() {
        NexNoteDebugLog.viewModel(event = "redoContentChange", details = uiState.value.debugViewModelSummary())
        contentHistory.redo()?.let(::applyHistorySnapshot)
    }

    fun clearContentHistory() {
        NexNoteDebugLog.viewModel(event = "clearContentHistory", details = uiState.value.debugViewModelSummary())
        contentHistory.clear()
    }

    fun onCreationDateChange(newTimestamp: Long) {
        NexNoteDebugLog.viewModel(
            event = "onCreationDateChange",
            details = "newTimestamp=$newTimestamp ${uiState.value.debugViewModelSummary()}"
        )
        _uiState.update { it.copy(creationDate = newTimestamp, isDirty = true) }
        scheduleAutosave()
    }

    fun onBackgroundColorChange(color: Int?) {
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
        imageActions.onImagePicked(openImageInputStream, insertionOffset)
    }

    /**
     * Removes an image from the note: deletes the physical file and strips its
     * Markdown tag from the content.
     */
    fun onRemoveImage(relativePath: String) {
        imageActions.onRemoveImage(relativePath)
    }

    fun getImageFile(relativePath: String): File {
        return getNoteImageFile(relativePath)
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
                openedDirectlyInEdit = false
            )
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
        saveDelegate.scheduleAutosave()
    }

    private fun resetContentHistory(content: String, selectionOffset: Int?) {
        NexNoteDebugLog.viewModel(
            event = "resetContentHistory",
            details = "selection=$selectionOffset ${NexNoteDebugLog.textSummary("content", content)}"
        )
        contentHistory.reset(
            EditorContentSnapshot(text = content, selectionOffset = selectionOffset)
        )
    }

    private fun recordImmediateContentHistoryChange(
        previous: EditorContentSnapshot,
        next: EditorContentSnapshot
    ) {
        contentHistory.recordImmediateChange(previous, next)
    }

    private fun applyHistorySnapshot(snapshot: EditorContentSnapshot) {
        NexNoteDebugLog.viewModel(
            event = "applyHistorySnapshot",
            details = "selection=${snapshot.selectionOffset} " +
                NexNoteDebugLog.textSummary("snapshot", snapshot.text)
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
                    getTemplateById       = useCases.templates.getTemplateById,
                    saveNote              = useCases.notes.saveNote,
                    saveTemplate          = useCases.templates.saveTemplate,
                    setNotePreviewMode    = useCases.notes.setNotePreviewMode,
                    observeNoteLinkCandidates = useCases.notes.observeNoteLinkCandidates,
                    observeTagsForNote    = useCases.tags.observeTagsForNote,
                    indexNoteTags         = useCases.tags.indexNoteTags,
                    observeThemeMode      = useCases.preferences.observeThemeMode,
                    setThemeMode          = useCases.preferences.setThemeMode,
                    initialMode           = mode
                )
            }
        }
    }
}

private fun EditorUiState.debugViewModelSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "loading=$isLoading dirty=$isDirty saving=$isSaving " +
        "preview=$showPreview openedDirectlyInPreview=$openedDirectlyInPreview " +
        "openedDirectlyInEdit=$openedDirectlyInEdit " +
        "contentVersion=$contentVersion selection=$contentSelectionOffset " +
        "${NexNoteDebugLog.textSummary("title", title)} " +
        NexNoteDebugLog.textSummary("content", content)
}
