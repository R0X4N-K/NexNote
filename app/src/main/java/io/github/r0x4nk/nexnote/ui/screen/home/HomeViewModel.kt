package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesSortedAscUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveMostUsedTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SearchNotesScoredUseCase
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val searchNotesScored: SearchNotesScoredUseCase,
    private val observeAllNotesSortedAsc: ObserveAllNotesSortedAscUseCase,
    private val observeAllNotes: ObserveAllNotesUseCase,
    moveNoteToTrash: MoveNoteToTrashUseCase,
    restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    toggleNotePin: ToggleNotePinUseCase,
    duplicateNoteUseCase: DuplicateNoteUseCase? = null,
    private val observeTemplates: ObserveTemplatesUseCase? = null,
    private val observeMostUsedTags: ObserveMostUsedTagsUseCase? = null,
    private val observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase? = null,
    observeNoteCardStyle: ObserveNoteCardStyleUseCase? = null
) : ViewModel() {

    private val _searchQuery       = MutableStateFlow("")
    private val _isSearchActive    = MutableStateFlow(false)
    private val _sortOrder         = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _viewMode          = MutableStateFlow(NoteListViewMode.LIST)
    private val _showTemplatePicker = MutableStateFlow(false)
    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())

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
     * When no TemplateRepository is provided (e.g., in unit tests), emits an empty list.
     */
    private val templatesFlow = buildHomeTemplatesFlow(observeTemplates, viewModelScope)

    /**
     * Most-used tags for the AutoScrollingTagRow.
     *
     * Bug B fix: shared as a hot [StateFlow] (Lazily started) so the Room query
     * stays alive in [viewModelScope] once started, regardless of whether the UI
     * is currently collecting [uiState]. Without this, the query stops after the
     * [WhileSubscribed] timeout while the Editor is open (>5 s), and the tag row
     * on the notes list does not reflect changes made in the Editor until the
     * [uiState] combine fully restarts — which itself incurs the debounce delay.
     * With [SharingStarted.Lazily] the query never stops once started, so the
     * [StateFlow] always replays the latest tags to [uiState] combine
     * immediately on restart.
     */
    private val topTagsFlow = buildHomeTopTagsFlow(observeMostUsedTags, viewModelScope)

    val noteCardStyle: StateFlow<NoteCardStyle> = (
        observeNoteCardStyle?.invoke() ?: flowOf(NoteCardStyle.TITLE_AND_PREVIEW)
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteCardStyle.TITLE_AND_PREVIEW
    )

    /**
     * Note IDs matching the active tag filters (intersection semantics).
     * Emits an empty set when no filters are active — the UI treats an empty
     * set as "show all".
     */
    private val filteredNoteIds: Flow<Set<Long>> =
        buildFilteredNoteIdsFlow(_selectedTagFilters, observeFilteredNoteIds)

    /**
     * Combines a debounce-aware search query and sort order as a single
     * [flatMapLatest] key, so a sort-order change immediately re-subscribes to
     * the correct DAO query.
     *
     * Bug A fix: [transformLatest] replaces the previous [debounce] call so that
     * an empty query (initial load, search cleared) emits instantly rather than
     * after [SEARCH_DEBOUNCE_MS]. Without this, the first subscription to [notesData]
     * waits 300 ms before emitting, keeping [uiState] in its [isLoading] state and
     * leaving [topTags] blank for that entire window. The debounce is still applied
     * for non-empty queries (live search typing) to avoid hammering the database.
     *
     * During search the sort order is ignored — results are ranked by score.
     */
    private val notesData =
        buildHomeNotesQueryFlow(
            searchQuery = _searchQuery,
            sortOrder = _sortOrder,
            searchNotesScored = searchNotesScored,
            observeAllNotesSortedAsc = observeAllNotesSortedAsc,
            observeAllNotes = observeAllNotes,
            searchDebounceMs = SEARCH_DEBOUNCE_MS
        )

    /**
     * Room flows are the single source of truth for the note list. No in-memory
     * pending-trash filter is applied here — [requestTrash] writes to the database
     * immediately, so Room's [allNotes] flow (which queries WHERE isDeleted = 0)
     * automatically excludes trashed notes without any extra in-memory layer.
     *
     * Tag filtering is applied in-memory after the Room query: [filteredNoteIds]
     * provides the set of matching IDs reactively, and the notes list is filtered
     * against it. This approach avoids a complex JOIN query in NoteDao and keeps
     * the tag filter logic separate from the core note queries.
     */
    val uiState: StateFlow<HomeUiState> = buildHomeUiStateFlow(
        flows = HomeUiStateFlows(
            notesData = notesData,
            filteredNoteIds = filteredNoteIds,
            searchQuery = _searchQuery,
            isSearchActive = _isSearchActive,
            selectedTagFilters = _selectedTagFilters,
            sortOrder = _sortOrder,
            viewMode = _viewMode,
            templates = templatesFlow,
            showTemplatePicker = _showTemplatePicker,
            topTags = topTagsFlow
        ),
        scope = viewModelScope
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onSearchToggle(active: Boolean) {
        _isSearchActive.update { active }
        if (!active) _searchQuery.update { "" }
    }

    fun toggleSortOrder() {
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
        noteListActions.toggleTagFilter(tagName)
    }

    /** Removes a single tag from the active filters. */
    fun removeTagFilter(tagName: String) {
        noteListActions.removeTagFilter(tagName)
    }

    /** Clears all active tag filters, restoring the full note list. */
    fun clearTagFilters() {
        noteListActions.clearTagFilters()
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

    fun togglePin(note: Note) {
        noteListActions.togglePin(note)
    }

    fun duplicateNote(note: Note) {
        noteListActions.duplicateNote(note)
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory = homeViewModelFactory()
    }
}
