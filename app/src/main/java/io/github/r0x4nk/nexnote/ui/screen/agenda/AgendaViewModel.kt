package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDistinctLocalDaysUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.common.TrashedNoteEvent
import io.github.r0x4nk.nexnote.ui.common.displayLabel
import io.github.r0x4nk.nexnote.ui.common.toTrashedNoteEvent
import io.github.r0x4nk.nexnote.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AgendaViewModel(
    private val observeDistinctLocalDays: ObserveDistinctLocalDaysUseCase,
    private val observeNotesByDateRange: ObserveNotesByDateRangeUseCase,
    private val moveNoteToTrash: MoveNoteToTrashUseCase,
    private val restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    private val toggleNotePin: ToggleNotePinUseCase,
    private val duplicateNoteUseCase: DuplicateNoteUseCase? = null,
    private val observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase? = null,
    observeNoteCardStyle: ObserveNoteCardStyleUseCase? = null
) : ViewModel() {

    private val initialDate = currentAgendaInitialDate()

    private val _displayedMonth = MutableStateFlow(initialDate.displayedMonth)
    private val _selectedDate = MutableStateFlow(initialDate.selectedDate)
    private val _searchQuery      = MutableStateFlow("")
    private val _isSearchActive   = MutableStateFlow(false)
    private val _sortOrder        = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _viewMode         = MutableStateFlow(NoteListViewMode.LIST)
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

    val noteCardStyle: StateFlow<NoteCardStyle> = (
        observeNoteCardStyle?.invoke() ?: flowOf(NoteCardStyle.TITLE_AND_PREVIEW)
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteCardStyle.TITLE_AND_PREVIEW
    )

    /** Global set of startOfDay(device tz) timestamps — one per day that has at least one note. */
    private val _daysWithNotes: StateFlow<Set<Long>> =
        buildAgendaDaysWithNotesFlow(observeDistinctLocalDays, viewModelScope)

    /** Raw notes for the currently selected day, re-emitted on every day change. */
    private val rawNotesForDay: Flow<List<Note>> =
        buildAgendaRawNotesForDayFlow(_selectedDate, observeNotesByDateRange)

    /**
     * Note IDs matching the active tag filters. Emits an empty set when no
     * filters are active (show all notes for the selected day).
     */
    private val filteredNoteIds: Flow<Set<Long>> =
        buildAgendaFilteredNoteIdsFlow(_selectedTagFilters, observeFilteredNoteIds)

    /**
     * Notes after applying tag filter, search filter, and sort order.
     * Debounced 300 ms so rapid typing does not cause excessive recomputations.
     */
    private val processedNotes: Flow<List<Note>> =
        buildAgendaProcessedNotesFlow(
            rawNotesForDay = rawNotesForDay,
            filteredNoteIds = filteredNoteIds,
            searchQuery = _searchQuery,
            sortOrder = _sortOrder,
            searchDebounceMs = SEARCH_DEBOUNCE_MS
        )

    /**
     * Room flows are the single source of truth for the note list. No in-memory
     * pending-trash filter is applied here — [requestTrash] writes to the database
     * immediately, so the DAO query (WHERE isDeleted = 0) automatically excludes
     * trashed notes. Restoring a note from TrashScreen is immediately reflected
     * here via the same Room flow mechanism.
     */
    val uiState: StateFlow<AgendaUiState> = buildAgendaUiStateFlow(
        flows = AgendaUiStateFlows(
            displayedMonth = _displayedMonth,
            daysWithNotes = _daysWithNotes,
            selectedDate = _selectedDate,
            processedNotes = processedNotes,
            searchQuery = _searchQuery,
            isSearchActive = _isSearchActive,
            sortOrder = _sortOrder,
            viewMode = _viewMode,
            selectedTagFilters = _selectedTagFilters
        ),
        scope = viewModelScope
    )

    // ── Note actions ─────────────────────────────────────────────────────────

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
     * the database write.
     */
    fun requestTrash(note: Note) {
        val event = note.toTrashedNoteEvent()
        viewModelScope.launch {
            moveNoteToTrash(note.id)
            _trashEvents.trySend(event)
        }
    }

    /**
     * Called when the undo snackbar is dismissed without pressing Undo.
     * The note is already in the database trash (written in [requestTrash]);
     * nothing else needs to happen here.
     */
    fun confirmTrash(@Suppress("UNUSED_PARAMETER") noteId: Long) {
        // No-op: Room flow already reflects the correct state.
    }

    /**
     * Restores the note from trash. Called when the user presses Undo.
     * Because [requestTrash] performed the database write eagerly, undo must
     * issue a matching [restoreFromTrash] call to bring the note back.
     */
    fun undoPendingTrash(noteId: Long) {
        viewModelScope.launch { restoreNoteFromTrash(noteId) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { toggleNotePin(note) }
    }

    fun duplicateNote(note: Note) {
        val duplicate = duplicateNoteUseCase ?: return
        val noteLabel = note.displayLabel()
        viewModelScope.launch {
            try {
                duplicate(note)
                _noteActionMessages.trySend("Duplicated \"$noteLabel\"")
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _noteActionMessages.trySend("Could not duplicate \"$noteLabel\"")
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onSearchToggle(active: Boolean) {
        _isSearchActive.update { active }
        if (!active) _searchQuery.update { "" }
    }

    // ── Sort & view mode ──────────────────────────────────────────────────────

    fun toggleSortOrder() {
        _sortOrder.update { current ->
            if (current == SortOrder.MODIFIED_DESC) SortOrder.MODIFIED_ASC else SortOrder.MODIFIED_DESC
        }
    }

    fun toggleViewMode() {
        _viewMode.update { current ->
            if (current == NoteListViewMode.LIST) NoteListViewMode.GRID else NoteListViewMode.LIST
        }
    }

    // ── Tag filter actions ────────────────────────────────────────────────────

    /** Toggles a tag filter; empty filter set = show all notes for the day. */
    fun toggleTagFilter(tagName: String) {
        _selectedTagFilters.update { current ->
            if (tagName in current) current - tagName else current + tagName
        }
    }

    /** Removes a single tag from the active filters. */
    fun removeTagFilter(tagName: String) {
        _selectedTagFilters.update { it - tagName }
    }

    /** Clears all active tag filters. */
    fun clearTagFilters() {
        _selectedTagFilters.update { emptySet() }
    }

    // ── Month navigation ──────────────────────────────────────────────────────

    fun navigateToPreviousMonth() {
        val target = _displayedMonth.value.shiftByMonths(-1)
        _displayedMonth.value = target
        clampSelectedDate(target.year, target.month)
    }

    fun navigateToNextMonth() {
        val target = _displayedMonth.value.shiftByMonths(+1)
        _displayedMonth.value = target
        clampSelectedDate(target.year, target.month)
    }

    // ── Day selection ─────────────────────────────────────────────────────────

    fun selectDate(year: Int, month: Int, day: Int) {
        _selectedDate.value = SelectedDate(year, month, day)
        _displayedMonth.value = DisplayedMonth(year, month)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Clamps the selected day to the last valid day of [targetMonth].
     * Example: day 31 selected in January → navigate to February → clamps to 28/29.
     */
    private fun clampSelectedDate(targetYear: Int, targetMonth: Int) {
        val maxDay     = DateUtils.daysInMonth(targetYear, targetMonth)
        val clampedDay = minOf(_selectedDate.value.day, maxDay)
        _selectedDate.value = SelectedDate(targetYear, targetMonth, clampedDay)
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val useCases = app.useCases
                AgendaViewModel(
                    observeDistinctLocalDays = useCases.notes.observeDistinctLocalDays,
                    observeNotesByDateRange = useCases.notes.observeNotesByDateRange,
                    moveNoteToTrash = useCases.notes.moveNoteToTrash,
                    restoreNoteFromTrash = useCases.notes.restoreNoteFromTrash,
                    toggleNotePin = useCases.notes.toggleNotePin,
                    duplicateNoteUseCase = useCases.notes.duplicateNote,
                    observeFilteredNoteIds = useCases.tags.observeFilteredNoteIds,
                    observeNoteCardStyle = useCases.preferences.observeNoteCardStyle
                )
            }
        }
    }
}
