package io.github.r0x4nk.nexnote.ui.screen.trash

import io.github.r0x4nk.nexnote.domain.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class TrashExtraState(
    val noteToDelete: Note? = null,
    val showEmptyTrashDialog: Boolean = false
)

internal fun buildTrashUiStateFlow(
    deletedNotes: Flow<List<Note>>,
    extra: Flow<TrashExtraState>,
    scope: CoroutineScope
): StateFlow<TrashUiState> {
    return combine(deletedNotes, extra) { notes, extraState ->
        TrashUiState(
            notes = notes,
            isLoading = false,
            noteToDelete = extraState.noteToDelete,
            showEmptyTrashDialog = extraState.showEmptyTrashDialog
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrashUiState()
    )
}
