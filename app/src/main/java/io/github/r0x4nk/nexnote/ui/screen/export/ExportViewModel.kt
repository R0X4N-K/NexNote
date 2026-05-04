package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAllNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNotesByDateRangeUseCase
import io.github.r0x4nk.nexnote.ui.navigation.Screen
import io.github.r0x4nk.nexnote.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class ExportScope {
    SingleNote,   // the note opened from the editor
    DateRange,    // notes within a user-specified date range
    AllNotes      // all active notes
}

enum class ExportFormat { TXT, MD, PDF, PRINT }

// ── UiState ───────────────────────────────────────────────────────────────────

@Immutable
data class ExportUiState(
    val scope: ExportScope = ExportScope.AllNotes,
    val dateFrom: Long? = null,   // startOfDay timestamp for the "from" day
    val dateTo: Long? = null,     // startOfDay timestamp for the "to" day (inclusive)
    val format: ExportFormat = ExportFormat.TXT,
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val error: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ExportViewModel(
    private val getNoteById: GetNoteByIdUseCase,
    private val observeAllNotes: ObserveAllNotesUseCase,
    private val observeNotesByDateRange: ObserveNotesByDateRangeUseCase,
    /** noteId of the note navigated from; Screen.NO_ID when opened from the global menu. */
    val initialNoteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExportUiState(
            scope = if (initialNoteId != Screen.NO_ID) ExportScope.SingleNote
            else ExportScope.AllNotes
        )
    )
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadNotes()
    }

    // ── User actions ─────────────────────────────────────────────────────────

    fun selectScope(scope: ExportScope) {
        _uiState.update { it.copy(scope = scope) }
        loadNotes()
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(format = format) }
    }

    /**
     * Sets the export date range. [from] and [to] are normalised to startOfDay
     * (local midnight). The load query uses startOfNextDay([to]) as the exclusive
     * upper bound so all notes created on [to] are included.
     */
    fun selectDateRange(from: Long, to: Long) {
        val normalizedFrom = DateUtils.startOfDay(from)
        val normalizedTo = DateUtils.startOfDay(to)
        _uiState.update { it.copy(dateFrom = normalizedFrom, dateTo = normalizedTo) }
        loadNotes()
    }

    // ── Callbacks from the UI layer (ExportManager) ──────────────────────────

    fun onExportStart() {
        _uiState.update { it.copy(isExporting = true, error = null) }
    }

    fun onExportComplete() {
        _uiState.update { it.copy(isExporting = false) }
    }

    fun onExportError(message: String) {
        _uiState.update { it.copy(isExporting = false, error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Note loading ──────────────────────────────────────────────────────────

    private fun loadNotes() {
        loadJob?.cancel()
        val state = _uiState.value
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val notes: List<Note> = when (state.scope) {
                ExportScope.SingleNote -> listOfNotNull(
                    if (initialNoteId != Screen.NO_ID) getNoteById(initialNoteId)
                    else null
                )

                ExportScope.AllNotes -> observeAllNotes().first()
                ExportScope.DateRange -> {
                    val from = state.dateFrom
                    val to = state.dateTo
                    if (from != null && to != null) {
                        // Exclusive upper bound = start of the day after [to], so notes
                        // created up to 23:59:59 on [to] are all included.
                        observeNotesByDateRange(
                            startMs = from,
                            endMs = DateUtils.startOfNextDay(to)
                        ).first()
                    } else emptyList()
                }
            }
            _uiState.update { it.copy(notes = notes, isLoading = false) }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        /** Factory without a noteId (global access from Agenda or Settings). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val notes = app.useCases.notes
                ExportViewModel(
                    getNoteById = notes.getNoteById,
                    observeAllNotes = notes.observeAllNotes,
                    observeNotesByDateRange = notes.observeNotesByDateRange,
                    initialNoteId = Screen.NO_ID
                )
            }
        }

        /** Factory with a noteId (navigation from the editor). */
        fun factory(noteId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val notes = app.useCases.notes
                ExportViewModel(
                    getNoteById = notes.getNoteById,
                    observeAllNotes = notes.observeAllNotes,
                    observeNotesByDateRange = notes.observeNotesByDateRange,
                    initialNoteId = noteId
                )
            }
        }
    }
}
