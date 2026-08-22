package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.di.requireAppDependencies
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.usecase.DeleteVaultNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveVaultNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultTrashedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreVaultNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleVaultNotePinUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.common.nextIn
import io.github.r0x4nk.nexnote.util.SearchUtils
import io.github.r0x4nk.nexnote.util.TagParser
import io.github.r0x4nk.nexnote.util.VaultTagAggregator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the unlocked Vault notes surface.
 *
 * When the Vault is not in the [VaultState.UNLOCKED] state the [notes] list is
 * always empty and the search state is reset: no Vault content must reach the
 * UI while the Vault is locked or not configured. The decryption itself is
 * performed by the data layer, which
 * already returns an empty flow when the in-memory Vault key is missing; this
 * ViewModel is an additional UI-level boundary on top of that contract.
 */
@Immutable
data class VaultNotesUiState(
    val isUnlocked: Boolean = false,
    val notes: List<Note> = emptyList(),
    val scoredResults: List<ScoredNote> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val searchSort: NoteSearchSort = NoteSearchSort.RELEVANCE,
    val searchScope: NoteSearchScope = NoteSearchScope.TITLE_AND_CONTENT,
    val pinnedFilter: NotePinnedFilter = NotePinnedFilter.ALL,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST,
    val isTrashVisible: Boolean = false,
    val notePendingPermanentDeleteId: Long? = null,
    val totalNoteCount: Int = 0,
    val selectedTagFilters: Set<String> = emptySet(),
    val topTags: List<Tag> = emptyList(),
    val availableTagNames: Set<String> = emptySet(),
    val showTemplatePicker: Boolean = false,
    val templates: List<Template> = emptyList(),
    /**
     * Mirrors Home's `HomeUiState.isLoading` semantics: true while the Vault
     * notes flow has not yet produced a first emission for the current
     * unlocked session, so the UI can show a spinner instead of a transient
     * "No Vault notes" empty state right after unlock. When the Vault is
     * locked or not configured the access surface (unlock/setup form) is in
     * charge of the visible state, so [isLoading] is reported as `false`
     * outside the unlocked branch.
     */
    val isLoading: Boolean = true
) {
    val hasActiveSearchFilters: Boolean
        get() = searchScope != NoteSearchScope.TITLE_AND_CONTENT ||
            pinnedFilter != NotePinnedFilter.ALL ||
            selectedTagFilters.isNotEmpty()
}

class VaultNotesViewModel(
    observeVaultState: ObserveVaultStateUseCase,
    observeVaultNotes: ObserveVaultNotesUseCase,
    observeVaultTrashedNotes: ObserveVaultTrashedNotesUseCase,
    private val moveNoteToVault: MoveNoteToVaultUseCase,
    private val moveVaultNoteToTrash: MoveVaultNoteToTrashUseCase,
    private val restoreVaultNoteFromTrash: RestoreVaultNoteFromTrashUseCase,
    private val deleteVaultNotePermanently: DeleteVaultNotePermanentlyUseCase,
    private val toggleVaultNotePin: ToggleVaultNotePinUseCase,
    private val duplicateVaultNote: DuplicateVaultNoteUseCase,
    private val removeNoteFromVault: RemoveNoteFromVaultUseCase,
    observeTemplates: ObserveTemplatesUseCase,
    observeNoteCardStyle: ObserveNoteCardStyleUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _searchSort = MutableStateFlow(NoteSearchSort.RELEVANCE)
    private val _searchScope = MutableStateFlow(NoteSearchScope.TITLE_AND_CONTENT)
    private val _pinnedFilter = MutableStateFlow(NotePinnedFilter.ALL)
    private val _viewMode = MutableStateFlow(NoteListViewMode.LIST)
    private val _isTrashVisible = MutableStateFlow(false)
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _showTemplatePicker = MutableStateFlow(false)
    private val _notePendingPermanentDeleteId = MutableStateFlow<Long?>(null)
    private val _vaultActionMessages = Channel<String>(Channel.BUFFERED)
    val vaultActionMessages = _vaultActionMessages.receiveAsFlow()

    /**
     * Emits non-sensitive Vault trash snackbar events so the Vault screen can
     * show an undo affordance for both soft-delete and manual restore.
     *
     * Only the [Note.id] and an event kind flow through this channel: no
     * decrypted title, preview or path leaves the data layer through here, so
     * a queued event cannot expose Vault content if it is observed while the
     * Vault is locked. Matching undo actions are gated by the current unlocked
     * UI state before they touch the data layer.
     */
    private val _vaultTrashEvents = Channel<VaultTrashSnackbarEvent>(Channel.BUFFERED)
    internal val vaultTrashEvents = _vaultTrashEvents.receiveAsFlow()

    val noteCardStyle: StateFlow<NoteCardStyle> = observeNoteCardStyle().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteCardStyle.TITLE_AND_PREVIEW
    )

    private val templates: StateFlow<List<Template>> = observeTemplates().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<VaultNotesUiState> =
        observeVaultState()
            .onEach { state ->
                if (state != VaultState.UNLOCKED) {
                    clearUnlockedUiState()
                }
            }
            .flatMapLatest { state ->
                if (state == VaultState.UNLOCKED) {
                    // Show a loading sentinel until the encrypted notes/trash
                    // flows have produced a first joined emission. Without this
                    // sentinel the Vault UI can render the "No Vault notes" empty
                    // state for the few frames between unlock and the first Room
                    // emission, which Home avoids via `HomeUiState.isLoading`.
                    combine(
                        observeVaultNotes(),
                        observeVaultTrashedNotes()
                    ) { activeNotes, trashedNotes ->
                        VaultNotesSource(
                            isUnlocked = true,
                            isLoading = false,
                            activeNotes = activeNotes,
                            trashedNotes = trashedNotes,
                            vaultTags = vaultTopTags(activeNotes)
                        )
                    }.onStart {
                        emit(
                            VaultNotesSource(
                                isUnlocked = true,
                                isLoading = true,
                                activeNotes = emptyList(),
                                trashedNotes = emptyList(),
                                vaultTags = emptyList()
                            )
                        )
                    }
                } else {
                    flowOf(
                        VaultNotesSource(
                            isUnlocked = false,
                            isLoading = false,
                            activeNotes = emptyList(),
                            trashedNotes = emptyList(),
                            vaultTags = emptyList()
                        )
                    )
                }
            }
            .combine(_searchQuery) { source, query -> source to query }
            .combine(_isSearchActive) { (source, query), isSearchActive ->
                VaultNotesFilterInput(
                    source = source,
                    query = query,
                    isSearchActive = isSearchActive,
                    selectedTagFilters = emptySet()
                )
            }
            .combine(_selectedTagFilters) { filterInput, selectedTagFilters ->
                filterInput.copy(selectedTagFilters = selectedTagFilters)
            }
            .combine(_searchSort) { filterInput, searchSort ->
                filterInput.copy(searchSort = searchSort)
            }
            .combine(_searchScope) { filterInput, searchScope ->
                filterInput.copy(searchScope = searchScope)
            }
            .combine(_pinnedFilter) { filterInput, pinnedFilter ->
                filterInput.copy(pinnedFilter = pinnedFilter)
            }
            .combine(_sortOrder) { filterInput, sortOrder ->
                filterInput to sortOrder
            }
            .combine(_viewMode) { (filterInput, sortOrder), viewMode ->
                Triple(filterInput, sortOrder, viewMode)
            }
            .combine(_isTrashVisible) { (filterInput, sortOrder, viewMode), isTrashVisible ->
                val source = filterInput.source
                if (!source.isUnlocked) {
                    // Locked / not configured: the unlock or setup form is in
                    // charge of the visible state, so report `isLoading = false`
                    // here. The default `true` only applies to the very initial
                    // StateFlow value before any upstream emission.
                    VaultNotesUiState(
                        sortOrder = sortOrder,
                        searchSort = filterInput.searchSort,
                        searchScope = filterInput.searchScope,
                        pinnedFilter = filterInput.pinnedFilter,
                        viewMode = viewMode,
                        isLoading = false
                    )
                } else if (source.isLoading) {
                    // Unlocked but the encrypted notes flow has not yet emitted.
                    // Surface a loading state to the UI instead of a transient
                    // empty list that would otherwise render the "No Vault
                    // notes" empty state for a few frames.
                    VaultNotesUiState(
                        isUnlocked = true,
                        isLoading = true,
                        sortOrder = sortOrder,
                        searchSort = filterInput.searchSort,
                        searchScope = filterInput.searchScope,
                        pinnedFilter = filterInput.pinnedFilter,
                        viewMode = viewMode,
                        isTrashVisible = isTrashVisible
                    )
                } else {
                    val effectiveQuery = if (filterInput.isSearchActive) filterInput.query else ""
                    // Keep selected filters even if the current Vault source no
                    // longer contains them. The selected chip must remain removable
                    // and an unmatched filter must produce an empty result instead
                    // of silently restoring every note.
                    val effectiveTagFilters = filterInput.selectedTagFilters
                    val sourceNotes = if (isTrashVisible) {
                        source.trashedNotes
                    } else {
                        source.activeNotes
                    }
                    val filteredNotes = filterVaultNotes(
                        notes = sourceNotes,
                        query = effectiveQuery,
                        selectedTagFilters = effectiveTagFilters,
                        searchScope = filterInput.searchScope,
                        pinnedFilter = filterInput.pinnedFilter,
                        searchSort = filterInput.searchSort
                    )
                    VaultNotesUiState(
                        isUnlocked = true,
                        isLoading = false,
                        notes = filteredNotes.sortedForVault(
                            browseSortOrder = sortOrder,
                            searchSort = filterInput.searchSort.takeIf {
                                effectiveQuery.isNotBlank()
                            } ?: NoteSearchSort.RELEVANCE
                        ),
                        scoredResults = filteredNotes.scoredResults,
                        searchQuery = effectiveQuery,
                        isSearchActive = filterInput.isSearchActive,
                        sortOrder = sortOrder,
                        searchSort = filterInput.searchSort,
                        searchScope = filterInput.searchScope,
                        pinnedFilter = filterInput.pinnedFilter,
                        viewMode = viewMode,
                        isTrashVisible = isTrashVisible,
                        totalNoteCount = sourceNotes.size,
                        selectedTagFilters = effectiveTagFilters,
                        topTags = if (isTrashVisible) emptyList() else source.vaultTags,
                        availableTagNames = sourceNotes.asSequence()
                            .flatMap { note -> TagParser.extractTags(note.content).asSequence() }
                            .toSet()
                    )
                }
            }
            .combine(_notePendingPermanentDeleteId) { state, noteId ->
                state.copy(
                    notePendingPermanentDeleteId = noteId.takeIf { state.isUnlocked }
                )
            }
            .combine(_showTemplatePicker) { state, showTemplatePicker ->
                val canShowTemplatePicker = state.isUnlocked && !state.isTrashVisible
                state.copy(
                    showTemplatePicker = showTemplatePicker && canShowTemplatePicker
                )
            }
            .combine(templates) { state, templates ->
                state.copy(
                    templates = if (state.showTemplatePicker) templates else emptyList()
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = VaultNotesUiState()
            )

    fun onSearchQueryChange(query: String) {
        if (uiState.value.isUnlocked) {
            _searchQuery.update { query }
        }
    }

    fun onSearchToggle(active: Boolean) {
        if (active && uiState.value.isUnlocked) {
            _isSearchActive.update { true }
        } else {
            clearSearchState()
        }
    }

    fun setSearchSort(sort: NoteSearchSort) {
        if (uiState.value.isUnlocked) _searchSort.value = sort
    }

    fun setSearchScope(scope: NoteSearchScope) {
        if (uiState.value.isUnlocked) _searchScope.value = scope
    }

    fun setPinnedFilter(filter: NotePinnedFilter) {
        if (uiState.value.isUnlocked) _pinnedFilter.value = filter
    }

    fun toggleSortOrder() {
        _sortOrder.update { current ->
            if (current == SortOrder.MODIFIED_DESC) {
                SortOrder.MODIFIED_ASC
            } else {
                SortOrder.MODIFIED_DESC
            }
        }
    }

    fun toggleViewMode() {
        _viewMode.update { current -> current.nextIn() }
    }

    fun showTemplatePicker() {
        val state = uiState.value
        if (!state.isUnlocked || state.isTrashVisible) return

        _showTemplatePicker.update { true }
    }

    fun dismissTemplatePicker() {
        _showTemplatePicker.update { false }
    }

    fun toggleTrashVisibility() {
        if (!uiState.value.isUnlocked) return

        clearSearchState()
        clearTagFilters()
        dismissTemplatePicker()
        _notePendingPermanentDeleteId.update { null }
        _isTrashVisible.update { !it }
    }

    fun toggleTagFilter(tagName: String) {
        val state = uiState.value
        if (!state.isUnlocked) return

        val normalizedTag = tagName.trim().lowercase()
        if (normalizedTag.isEmpty()) return

        _selectedTagFilters.update { current ->
            if (normalizedTag in current) current - normalizedTag else current + normalizedTag
        }
    }

    fun removeTagFilter(tagName: String) {
        if (!uiState.value.isUnlocked) return

        _selectedTagFilters.update { current -> current - tagName.trim().lowercase() }
    }

    fun clearTagFilters() {
        _selectedTagFilters.update { emptySet() }
    }

    fun removeFromVault(note: Note) {
        if (!note.isInVault || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                val removed = removeNoteFromVault(note.id)
                _vaultActionMessages.trySend(
                    if (removed) {
                        "Note removed from Vault"
                    } else {
                        "Could not remove note from Vault"
                    }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not remove note from Vault")
            }
        }
    }

    fun moveToTrash(note: Note) {
        moveToTrash(listOf(note))
    }

    fun moveToTrash(notes: Collection<Note>) {
        val state = uiState.value
        if (!state.isUnlocked || state.isTrashVisible) return

        val movableNotes = notes.filter { note ->
            note.isInVault && !note.isDeleted
        }
        if (movableNotes.isEmpty()) return

        viewModelScope.launch {
            try {
                val movedIds = mutableListOf<Long>()
                movableNotes.forEach { note ->
                    if (moveVaultNoteToTrash(note.id)) {
                        movedIds += note.id
                    }
                }
                if (movedIds.isNotEmpty()) {
                    _vaultTrashEvents.trySend(
                        VaultTrashSnackbarEvent.MovedToTrash(
                            noteId = movedIds.first(),
                            additionalNoteIds = movedIds.drop(1)
                        )
                    )
                } else {
                    _vaultActionMessages.trySend("Could not move note to trash")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not move note to trash")
            }
        }
    }

    /**
     * Restores a Vault note that was just trashed through [moveToTrash]. Used
     * by the undo affordance on the Vault trash snackbar.
     *
     * Mirrors Home's [NoteListActionsDelegate.undoPendingTrash] timing: the
     * data write already happened eagerly, so undo simply calls the restore
     * use case. The action is ignored if the Vault is not unlocked, so a
     * stale event observed after the Vault locks cannot trigger restores
     * against an in-memory key that is no longer present.
     */
    fun undoMoveToTrash(noteId: Long) {
        if (noteId <= 0L || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                val restored = restoreVaultNoteFromTrash(noteId)
                if (!restored) {
                    _vaultActionMessages.trySend("Could not restore note")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not restore note")
            }
        }
    }

    internal fun undoTrashSnackbarEvent(event: VaultTrashSnackbarEvent) {
        when (event) {
            is VaultTrashSnackbarEvent.MovedToTrash -> {
                event.noteIds.forEach(::undoMoveToTrash)
            }
            is VaultTrashSnackbarEvent.RestoredFromTrash -> {
                event.noteIds.forEach(::undoRestoreFromTrash)
            }
        }
    }

    fun undoRestoreFromTrash(noteId: Long) {
        val state = uiState.value
        if (noteId <= 0L || !state.isUnlocked || !state.isTrashVisible) return

        viewModelScope.launch {
            try {
                val moved = moveVaultNoteToTrash(noteId)
                if (!moved) {
                    _vaultActionMessages.trySend("Could not move note to trash")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not move note to trash")
            }
        }
    }

    fun togglePin(note: Note) {
        if (!note.isInVault || note.isDeleted || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                val toggled = toggleVaultNotePin(note)
                if (!toggled) {
                    _vaultActionMessages.trySend("Could not update note pin")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not update note pin")
            }
        }
    }

    fun duplicate(note: Note) {
        if (!note.isInVault || note.isDeleted || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                _vaultActionMessages.trySend(duplicateVaultNote(note.id).toMessage())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not duplicate note")
            }
        }
    }

    fun restoreFromTrash(note: Note) {
        val state = uiState.value
        if (!note.isInVault || !note.isDeleted || !state.isUnlocked || !state.isTrashVisible) {
            return
        }

        viewModelScope.launch {
            try {
                val restored = restoreVaultNoteFromTrash(note.id)
                if (restored) {
                    _vaultTrashEvents.trySend(VaultTrashSnackbarEvent.RestoredFromTrash(note.id))
                } else {
                    _vaultActionMessages.trySend("Could not restore note")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not restore note")
            }
        }
    }

    fun requestDeletePermanentlyFromTrash(note: Note) {
        val state = uiState.value
        if (!note.isInVault || !note.isDeleted || !state.isUnlocked || !state.isTrashVisible) {
            return
        }

        _notePendingPermanentDeleteId.update { note.id }
    }

    fun cancelDeletePermanentlyFromTrash() {
        _notePendingPermanentDeleteId.update { null }
    }

    fun confirmDeletePermanentlyFromTrash() {
        val noteId = _notePendingPermanentDeleteId.value ?: return
        _notePendingPermanentDeleteId.update { null }
        if (!uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                val deleted = deleteVaultNotePermanently(noteId)
                _vaultActionMessages.trySend(
                    if (deleted) {
                        "Note permanently deleted"
                    } else {
                        "Could not delete note"
                    }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not delete note")
            }
        }
    }

    fun moveNormalNoteToVault(noteId: Long) {
        if (noteId <= 0L || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                _vaultActionMessages.trySend(moveNoteToVault(noteId).toMessage())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not move note to Vault")
            }
        }
    }

    private fun clearSearchState() {
        _searchQuery.update { "" }
        _isSearchActive.update { false }
        _searchSort.value = NoteSearchSort.RELEVANCE
        _searchScope.value = NoteSearchScope.TITLE_AND_CONTENT
        _pinnedFilter.value = NotePinnedFilter.ALL
    }

    private fun clearUnlockedUiState() {
        clearSearchState()
        clearTagFilters()
        dismissTemplatePicker()
        _isTrashVisible.update { false }
        _notePendingPermanentDeleteId.update { null }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = requireAppDependencies()
                val vault = app.useCases.vault
                VaultNotesViewModel(
                    observeVaultState = vault.observeVaultState,
                    observeVaultNotes = vault.observeVaultNotes,
                    observeVaultTrashedNotes = vault.observeVaultTrashedNotes,
                    moveNoteToVault = vault.moveNoteToVault,
                    moveVaultNoteToTrash = vault.moveVaultNoteToTrash,
                    restoreVaultNoteFromTrash = vault.restoreVaultNoteFromTrash,
                    deleteVaultNotePermanently = vault.deleteVaultNotePermanently,
                    toggleVaultNotePin = vault.toggleVaultNotePin,
                    duplicateVaultNote = vault.duplicateVaultNote,
                    removeNoteFromVault = vault.removeNoteFromVault,
                    observeTemplates = app.useCases.templates.observeTemplates,
                    observeNoteCardStyle = app.useCases.preferences.observeNoteCardStyle
                )
            }
        }
    }
}

private fun MoveNoteToVaultResult.toMessage(): String =
    when (this) {
        MoveNoteToVaultResult.Success -> "Note moved to Vault"
        MoveNoteToVaultResult.NotFound -> "Could not move note to Vault"
    }

private fun DuplicateVaultNoteResult.toMessage(): String =
    when (this) {
        is DuplicateVaultNoteResult.Success -> "Vault note duplicated"
        DuplicateVaultNoteResult.NotFound,
        DuplicateVaultNoteResult.Failed -> "Could not duplicate note"
    }

private data class VaultNotesSource(
    val isUnlocked: Boolean,
    val isLoading: Boolean,
    val activeNotes: List<Note>,
    val trashedNotes: List<Note>,
    val vaultTags: List<Tag>
)

private data class VaultNotesFilterInput(
    val source: VaultNotesSource,
    val query: String,
    val isSearchActive: Boolean,
    val selectedTagFilters: Set<String>,
    val searchSort: NoteSearchSort = NoteSearchSort.RELEVANCE,
    val searchScope: NoteSearchScope = NoteSearchScope.TITLE_AND_CONTENT,
    val pinnedFilter: NotePinnedFilter = NotePinnedFilter.ALL
)

private data class VaultFilteredNotes(
    val notes: List<Note>,
    val scoredResults: List<ScoredNote>
)

private fun filterVaultNotes(
    notes: List<Note>,
    query: String,
    selectedTagFilters: Set<String>,
    searchScope: NoteSearchScope,
    pinnedFilter: NotePinnedFilter,
    searchSort: NoteSearchSort
): VaultFilteredNotes {
    val normalizedQuery = query.trim()
    val tagFilteredNotes = if (selectedTagFilters.isEmpty()) {
        notes
    } else {
        notes.filter { note -> noteContainsAllVaultTags(note, selectedTagFilters) }
    }
    val pinnedFilteredNotes = tagFilteredNotes.filter { note ->
        when (pinnedFilter) {
            NotePinnedFilter.ALL -> true
            NotePinnedFilter.PINNED -> note.isPinned
            NotePinnedFilter.UNPINNED -> !note.isPinned
        }
    }
    if (normalizedQuery.isEmpty()) {
        return VaultFilteredNotes(notes = pinnedFilteredNotes, scoredResults = emptyList())
    }

    val scoredResults = SearchUtils.searchAndSort(
        notes = pinnedFilteredNotes,
        query = normalizedQuery,
        scope = searchScope,
        sort = searchSort
    )
    return VaultFilteredNotes(
        notes = scoredResults.map { it.note },
        scoredResults = scoredResults
    )
}

private fun noteContainsAllVaultTags(note: Note, selectedTagFilters: Set<String>): Boolean {
    val noteTags = TagParser.extractTags(note.content)
    return noteTags.containsAll(selectedTagFilters)
}

private fun vaultTopTags(notes: List<Note>): List<Tag> =
    VaultTagAggregator.aggregate(notes)
        .take(TagRepository.DEFAULT_TOP_TAGS_LIMIT)

private fun VaultFilteredNotes.sortedForVault(
    browseSortOrder: SortOrder,
    searchSort: NoteSearchSort
): List<Note> {
    if (scoredResults.isNotEmpty()) return notes
    if (searchSort == NoteSearchSort.TITLE_ASC) {
        return notes.sortedWith(
            compareByDescending<Note> { note -> note.isPinned }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { note -> note.title }
                .thenBy { note -> note.id }
        )
    }
    if (searchSort == NoteSearchSort.TITLE_DESC) {
        return notes.sortedWith(
            compareByDescending<Note> { note -> note.isPinned }
                .thenByDescending(String.CASE_INSENSITIVE_ORDER) { note -> note.title }
                .thenByDescending { note -> note.id }
        )
    }
    val effectiveSortOrder = when (searchSort) {
        NoteSearchSort.MODIFIED_ASC -> SortOrder.MODIFIED_ASC
        NoteSearchSort.MODIFIED_DESC -> SortOrder.MODIFIED_DESC
        NoteSearchSort.RELEVANCE -> browseSortOrder
        NoteSearchSort.TITLE_ASC,
        NoteSearchSort.TITLE_DESC -> error("Handled above")
    }
    return sortVaultNotes(notes = notes, sortOrder = effectiveSortOrder)
}

private fun sortVaultNotes(notes: List<Note>, sortOrder: SortOrder): List<Note> {
    val comparator = when (sortOrder) {
        SortOrder.MODIFIED_DESC ->
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.lastModifiedDate }
                .thenBy { it.id }

        SortOrder.MODIFIED_ASC ->
            compareByDescending<Note> { it.isPinned }
                .thenBy { it.lastModifiedDate }
                .thenBy { it.id }
    }

    return notes.sortedWith(comparator)
}
