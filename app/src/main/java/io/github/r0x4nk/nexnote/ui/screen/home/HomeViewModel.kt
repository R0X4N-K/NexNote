package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveActiveNoteCountUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveHomeNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveHomeNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListActionsDelegate
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.common.TrashedNoteEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val observeHomeNotes: ObserveHomeNotesUseCase,
    observeHomeNoteIds: ObserveHomeNoteIdsUseCase,
    observeActiveNoteCount: ObserveActiveNoteCountUseCase,
    moveNoteToTrash: MoveNoteToTrashUseCase,
    restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    toggleNotePin: ToggleNotePinUseCase,
    duplicateNoteUseCase: DuplicateNoteUseCase,
    private val observeTemplates: ObserveTemplatesUseCase,
    private val observeMostUsedTags: ObserveMostUsedTagsUseCase,
    observeNoteCardStyle: ObserveNoteCardStyleUseCase
) : ViewModel() {

    private val _searchQuery       = MutableStateFlow("")
    private val _isSearchActive    = MutableStateFlow(false)
    private val _sortOrder         = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _searchSort        = MutableStateFlow(HomeSearchSort.RELEVANCE)
    private val _searchScope       = MutableStateFlow(HomeSearchScope.TITLE_AND_CONTENT)
    private val _pinnedFilter      = MutableStateFlow(HomePinnedFilter.ALL)
    private val _viewMode          = MutableStateFlow(NoteListViewMode.LIST)
    private val _showTemplatePicker = MutableStateFlow(false)
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    private val _noteLimit = MutableStateFlow(INITIAL_NOTE_LIMIT)

    /**
     * One-shot event channel for trash actions. Each emission triggers a
     * single snackbar in the UI. Using a [Channel] (not a StateFlow) ensures
     * the event is consumed exactly once and is never re-emitted on
     * recomposition or configuration change.
     */
    private val _trashEvents = Channel<TrashedNoteEvent>(Channel.BUFFERED)
    val trashEvents: Flow<TrashedNoteEvent> = _trashEvents.receiveAsFlow()

    private val _noteActionMessages = Channel<String>(Channel.BUFFERED)
    val noteActionMessages: Flow<String> = _noteActionMessages.receiveAsFlow()

    private val noteListActions = NoteListActionsDelegate(
        scope = viewModelScope,
        moveNoteToTrash = moveNoteToTrash,
        restoreNoteFromTrash = restoreNoteFromTrash,
        toggleNotePin = toggleNotePin,
        duplicateNoteUseCase = duplicateNoteUseCase,
        sortOrder = _sortOrder,
        viewMode = _viewMode,
        selectedTagFilters = _selectedTagFilters,
        trashEvents = _trashEvents,
        noteActionMessages = _noteActionMessages
    )

    /**
     * Templates are loaded once and kept hot for the duration of the ViewModel.
     * This avoids a cold-flow re-subscription every time the picker is shown.
     * The dependency is required so a wiring error cannot silently hide templates.
     */
    private val templatesFlow = buildHomeTemplatesFlow(observeTemplates, viewModelScope)

    /** Keeps the compact tag filter source current while Home is off screen. */
    private val topTagsFlow = buildHomeTopTagsFlow(observeMostUsedTags, viewModelScope)

    val noteCardStyle: StateFlow<NoteCardStyle> = observeNoteCardStyle().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteCardStyle.TITLE_AND_PREVIEW
    )

    /** Builds the bounded DAO query from debounced text and immediate controls. */
    private val notesData =
        buildHomeNotesQueryFlow(
            searchQuery = _searchQuery,
            isSearchActive = _isSearchActive,
            sortOrder = _sortOrder,
            searchSort = _searchSort,
            searchScope = _searchScope,
            pinnedFilter = _pinnedFilter,
            selectedTagFilters = _selectedTagFilters,
            noteLimit = _noteLimit,
            observeHomeNotes = observeHomeNotes,
            searchDebounceMs = SEARCH_DEBOUNCE_MS
        )

    val selectionCandidateIds: StateFlow<Set<Long>?> =
        buildHomeSelectionCandidateIdsFlow(
            searchQuery = _searchQuery,
            isSearchActive = _isSearchActive,
            searchScope = _searchScope,
            pinnedFilter = _pinnedFilter,
            selectedTagFilters = _selectedTagFilters,
            observeHomeNoteIds = observeHomeNoteIds,
            searchDebounceMs = SEARCH_DEBOUNCE_MS
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * Home observes a bounded Room query. Search, sorting and tag intersection
     * are executed by SQLite before note bodies enter application memory.
     */
    val uiState: StateFlow<HomeUiState> = buildHomeUiStateFlow(
        flows = HomeUiStateFlows(
            notesData = notesData,
            activeNoteCount = observeActiveNoteCount(),
            searchQuery = _searchQuery,
            isSearchActive = _isSearchActive,
            selectedTagFilters = _selectedTagFilters,
            searchSort = _searchSort,
            searchScope = _searchScope,
            pinnedFilter = _pinnedFilter,
            sortOrder = _sortOrder,
            viewMode = _viewMode,
            templates = templatesFlow,
            showTemplatePicker = _showTemplatePicker,
            topTags = topTagsFlow
        ),
        scope = viewModelScope
    )

    fun onSearchQueryChange(query: String) {
        if (query != _searchQuery.value) resetNoteWindow()
        _searchQuery.update { query }
    }

    fun onSearchToggle(active: Boolean) {
        if (active != _isSearchActive.value) resetNoteWindow()
        _isSearchActive.update { active }
        if (!active) {
            _searchQuery.value = ""
            _searchSort.value = HomeSearchSort.RELEVANCE
            _searchScope.value = HomeSearchScope.TITLE_AND_CONTENT
            _pinnedFilter.value = HomePinnedFilter.ALL
        }
    }

    fun setSearchSort(sort: HomeSearchSort) {
        resetNoteWindow()
        _searchSort.value = sort
    }

    fun setSearchScope(scope: HomeSearchScope) {
        resetNoteWindow()
        _searchScope.value = scope
    }

    fun setPinnedFilter(filter: HomePinnedFilter) {
        resetNoteWindow()
        _pinnedFilter.value = filter
    }

    fun toggleSortOrder() {
        resetNoteWindow()
        noteListActions.toggleSortOrder()
    }

    fun toggleViewMode() {
        noteListActions.toggleViewMode()
    }

    fun showTemplatePicker() {
        _showTemplatePicker.update { true }
    }

    fun dismissTemplatePicker() {
        _showTemplatePicker.update { false }
    }

    // ── Tag filter actions ────────────────────────────────────────────────────

    /**
     * Toggles a tag filter: if [tagName] is already active it is removed;
     * otherwise it is added. An empty filter set means "show all notes".
     */
    fun toggleTagFilter(tagName: String) {
        resetNoteWindow()
        noteListActions.toggleTagFilter(tagName)
    }

    /** Removes a single tag from the active filters. */
    fun removeTagFilter(tagName: String) {
        resetNoteWindow()
        noteListActions.removeTagFilter(tagName)
    }

    /** Clears all active tag filters, restoring the full note list. */
    fun clearTagFilters() {
        resetNoteWindow()
        noteListActions.clearTagFilters()
    }

    /** Extends the bounded Home query when the user approaches the loaded edge. */
    fun loadMoreNotes() {
        _noteLimit.update { current ->
            (current + NOTE_LOAD_BATCH_SIZE).coerceAtMost(
                uiState.value.totalNoteCount.coerceAtLeast(INITIAL_NOTE_LIMIT)
            )
        }
    }

    private fun resetNoteWindow() {
        _noteLimit.value = INITIAL_NOTE_LIMIT
    }

    // ── Trash actions ─────────────────────────────────────────────────────────

    /**
     * Moves the note to trash (database write) and signals the UI to show an
     * undo snackbar. The write runs in [viewModelScope] — ViewModel-scoped, not
     * composable-scoped — so it completes even if the user switches tabs before
     * the snackbar appears or is dismissed.
     *
     * The snackbar event is sent only AFTER the write completes so that
     * [undoPendingTrash] can safely call [restoreFromTrash] without racing
     * against an in-flight [moveToTrash].
     *
     * Visual feedback (instant disappearance) is handled by NoteCard's local
     * [collapsed] state, which triggers its shrink animation independently of
     * the database write. By the time the 280 ms animation delay elapses and
     * this method is called, the DB write is complete and Room has already
     * emitted the updated list — no in-memory filter is needed.
     */
    fun requestTrash(note: Note) {
        noteListActions.requestTrash(note)
    }

    fun requestTrash(notes: Collection<Note>) {
        noteListActions.requestTrash(notes)
    }

    fun requestTrashByIds(noteIds: Collection<Long>) {
        noteListActions.requestTrashByIds(noteIds)
    }

    /**
     * Called when the undo snackbar is dismissed without pressing Undo.
     * The note is already in the database trash (written in [requestTrash]);
     * nothing else needs to happen here.
     */
    fun confirmTrash() {
        noteListActions.confirmTrash()
    }

    /**
     * Restores the note from trash. Called when the user presses Undo.
     * Because [requestTrash] performed the database write eagerly, undo must
     * issue a matching [restoreFromTrash] call to bring the note back.
     */
    fun undoPendingTrash(noteId: Long) {
        noteListActions.undoPendingTrash(noteId)
    }

    fun undoPendingTrash(noteIds: Collection<Long>) {
        noteListActions.undoPendingTrash(noteIds)
    }

    fun togglePin(note: Note) {
        noteListActions.togglePin(note)
    }

    fun duplicateNote(note: Note) {
        noteListActions.duplicateNote(note)
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val INITIAL_NOTE_LIMIT = 64
        private const val NOTE_LOAD_BATCH_SIZE = 64

        val Factory = homeViewModelFactory()
    }
}
