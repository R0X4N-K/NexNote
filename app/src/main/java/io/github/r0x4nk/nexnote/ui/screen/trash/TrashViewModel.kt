package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.di.requireAppDependencies
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.EmptyTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteOperationRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TrashUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val noteToDelete: Note? = null,        // non-null → delete-confirmation dialog is shown
    val showEmptyTrashDialog: Boolean = false,
    val errorMessage: String? = null
)

class TrashViewModel(
    private val observeDeletedNotes: ObserveDeletedNotesUseCase,
    private val restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    private val deleteNotePermanently: DeleteNotePermanentlyUseCase,
    private val emptyTrash: EmptyTrashUseCase,
    private val strings: StringProvider
) : ViewModel() {

    private val _extra = MutableStateFlow(TrashExtraState())

    val uiState: StateFlow<TrashUiState> = buildTrashUiStateFlow(
        deletedNotes = observeDeletedNotes(),
        extra = _extra,
        scope = viewModelScope
    )

    // ── Restore ───────────────────────────────────────────────────────────────

    private val operations = NoteOperationRunner(viewModelScope)
    val operationProgress = operations.progress

    fun restoreNote(noteId: Long) {
        operations.launch(strings.get(R.string.trash_progress_restore_note), onError = {
            _extra.update { it.copy(errorMessage = strings.get(R.string.trash_error_restore_note)) }
        }) { restoreNoteFromTrash(noteId) }
    }

    fun restoreAll() {
        val ids = uiState.value.notes.map { it.id }
        if (ids.isEmpty()) return
        operations.launch(strings.get(R.string.trash_progress_restore_notes), onError = {
            _extra.update { it.copy(errorMessage = strings.get(R.string.trash_error_restore_notes)) }
        }) { restoreNoteFromTrash(ids) }
    }

    fun requestDeletePermanently(note: Note) {
        if (operationProgress.value != null) return
        _extra.update { it.copy(noteToDelete = note) }
    }

    fun confirmDeletePermanently() {
        val note = _extra.value.noteToDelete ?: return
        operations.launch(strings.get(R.string.trash_progress_delete_note), onError = {
            _extra.update { it.copy(errorMessage = strings.get(R.string.trash_error_delete_note)) }
        }) {
            deleteNotePermanently(note.id)
            _extra.update { it.copy(noteToDelete = null, errorMessage = null) }
        }
    }

    fun cancelDelete() {
        if (operationProgress.value != null) return
        _extra.update { it.copy(noteToDelete = null) }
    }

    fun requestEmptyTrash() {
        if (operationProgress.value != null) return
        _extra.update { it.copy(showEmptyTrashDialog = true) }
    }

    fun confirmEmptyTrash() {
        if (!_extra.value.showEmptyTrashDialog) return
        operations.launch(strings.get(R.string.trash_progress_empty), onError = {
            _extra.update { it.copy(errorMessage = strings.get(R.string.trash_error_empty)) }
        }) {
            emptyTrash()
            _extra.update { it.copy(showEmptyTrashDialog = false, errorMessage = null) }
        }
    }

    fun cancelEmptyTrash() {
        if (operationProgress.value != null) return
        _extra.update { it.copy(showEmptyTrashDialog = false) }
    }

    fun clearError() {
        _extra.update { it.copy(errorMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = requireAppDependencies()
                val useCases = app.useCases
                TrashViewModel(
                    observeDeletedNotes = useCases.notes.observeDeletedNotes,
                    restoreNoteFromTrash = useCases.notes.restoreNoteFromTrash,
                    deleteNotePermanently = useCases.notes.deleteNotePermanently,
                    emptyTrash = useCases.notes.emptyTrash,
                    strings = app.strings
                )
            }
        }
    }
}