package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.di.requireAppDependencies
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.EmptyTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
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
    private val emptyTrash: EmptyTrashUseCase
) : ViewModel() {

    private val _extra = MutableStateFlow(TrashExtraState())

    val uiState: StateFlow<TrashUiState> = buildTrashUiStateFlow(
        deletedNotes = observeDeletedNotes(),
        extra = _extra,
        scope = viewModelScope
    )

    // ── Restore ───────────────────────────────────────────────────────────────

    fun restoreNote(noteId: Long) {
        viewModelScope.launch { restoreNoteFromTrash(noteId) }
    }

    // ── Single deletion (with confirmation) ──────────────────────────────────

    fun requestDeletePermanently(note: Note) {
        _extra.update { it.copy(noteToDelete = note) }
    }

    /**
     * Permanently deletes the note confirmed in the dialog.
     * The repository removes the database row and its internal image files.
     */
    fun confirmDeletePermanently() {
        val note = _extra.value.noteToDelete ?: return
        viewModelScope.launch {
            try {
                deleteNotePermanently(note.id)
                _extra.update { it.copy(noteToDelete = null, errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _extra.update {
                    it.copy(errorMessage = "Could not permanently delete the note. Try again.")
                }
            }
        }
    }

    fun cancelDelete() {
        _extra.update { it.copy(noteToDelete = null) }
    }

    // ── Empty trash (with confirmation) ──────────────────────────────────────

    fun requestEmptyTrash() {
        _extra.update { it.copy(showEmptyTrashDialog = true) }
    }

    /**
     * Permanently deletes all notes in the trash.
     * The repository removes database rows and their internal image files.
     */
    fun confirmEmptyTrash() {
        viewModelScope.launch {
            try {
                emptyTrash()
                _extra.update { it.copy(showEmptyTrashDialog = false, errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _extra.update { it.copy(errorMessage = "Could not empty the trash. Try again.") }
            }
        }
    }

    fun cancelEmptyTrash() {
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
                    emptyTrash = useCases.notes.emptyTrash
                )
            }
        }
    }
}
