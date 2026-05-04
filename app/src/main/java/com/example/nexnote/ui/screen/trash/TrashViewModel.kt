package com.example.nexnote.ui.screen.trash

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nexnote.NexNoteApp
import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.usecase.DeleteNotePermanentlyUseCase
import com.example.nexnote.domain.usecase.DeleteNoteImageUseCase
import com.example.nexnote.domain.usecase.EmptyTrashUseCase
import com.example.nexnote.domain.usecase.ObserveDeletedNotesUseCase
import com.example.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TrashUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val noteToDelete: Note? = null,        // non-null → delete-confirmation dialog is shown
    val showEmptyTrashDialog: Boolean = false
)

class TrashViewModel(
    private val observeDeletedNotes: ObserveDeletedNotesUseCase,
    private val restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    private val deleteNotePermanently: DeleteNotePermanentlyUseCase,
    private val emptyTrash: EmptyTrashUseCase,
    private val deleteNoteImage: DeleteNoteImageUseCase
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
     * Deletes image files first (on IO), then removes the database record.
     */
    fun confirmDeletePermanently() {
        val note = _extra.value.noteToDelete ?: return
        viewModelScope.launch {
            if (note.imagePaths.isNotEmpty()) {
                note.imagePaths.forEach { deleteNoteImage(it) }
            }
            deleteNotePermanently(note.id)
            _extra.update { it.copy(noteToDelete = null) }
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
     * Image files for every trashed note are deleted first (on IO),
     * then the database records are removed.
     */
    fun confirmEmptyTrash() {
        val pathsToDelete = uiState.value.notes.flatMap { it.imagePaths }
        viewModelScope.launch {
            if (pathsToDelete.isNotEmpty()) {
                pathsToDelete.forEach { deleteNoteImage(it) }
            }
            emptyTrash()
            _extra.update { it.copy(showEmptyTrashDialog = false) }
        }
    }

    fun cancelEmptyTrash() {
        _extra.update { it.copy(showEmptyTrashDialog = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val useCases = app.useCases
                TrashViewModel(
                    observeDeletedNotes = useCases.notes.observeDeletedNotes,
                    restoreNoteFromTrash = useCases.notes.restoreNoteFromTrash,
                    deleteNotePermanently = useCases.notes.deleteNotePermanently,
                    emptyTrash = useCases.notes.emptyTrash,
                    deleteNoteImage = useCases.images.deleteNoteImage
                )
            }
        }
    }
}
