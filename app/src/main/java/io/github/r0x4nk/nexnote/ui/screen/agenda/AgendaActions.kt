package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.r0x4nk.nexnote.domain.model.Note
import java.util.Calendar

internal data class AgendaActions(
    val onPreviousMonth: () -> Unit,
    val onNextMonth: () -> Unit,
    val onGoToToday: () -> Unit,
    val onSelectDate: (Int, Int, Int) -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onSearchToggle: (Boolean) -> Unit,
    val onToggleSort: () -> Unit,
    val onToggleView: () -> Unit,
    val onRemoveTagFilter: (String) -> Unit,
    val onClearTagFilters: () -> Unit,
    val onNewNote: (Long) -> Unit,
    val onNoteClick: (Long) -> Unit,
    val onTogglePin: (Note) -> Unit,
    val onDuplicateNote: (Note) -> Unit,
    val onRequestTrash: (Note) -> Unit,
    val onRequestNoteActions: (Note) -> Unit,
    val onStartNoteSelection: () -> Unit,
    val onExitNoteSelection: () -> Unit,
    val onSelectAllVisibleNotes: () -> Unit,
    val onDeselectAllNotes: () -> Unit,
    val onShareSelectedNotes: () -> Unit,
    val onCopySelectedNotesAsText: () -> Unit,
    val onCopySelectedNotesAsMarkdown: () -> Unit,
    val onDeleteSelectedNotes: () -> Unit,
    val onToggleNoteSelection: (Note) -> Unit,
    val onUndoTrash: (Long) -> Unit,
    val onConfirmTrash: () -> Unit
)

@Composable
internal fun rememberAgendaActions(
    viewModel: AgendaViewModel,
    onNoteClick: (Long) -> Unit,
    onNewNote: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onStartNoteSelection: () -> Unit,
    onExitNoteSelection: () -> Unit,
    onSelectAllVisibleNotes: () -> Unit,
    onDeselectAllNotes: () -> Unit,
    onShareSelectedNotes: () -> Unit,
    onCopySelectedNotesAsText: () -> Unit,
    onCopySelectedNotesAsMarkdown: () -> Unit,
    onDeleteSelectedNotes: () -> Unit,
    onToggleNoteSelection: (Note) -> Unit
): AgendaActions {
    return remember(
        viewModel,
        onNoteClick,
        onNewNote,
        onRequestNoteActions,
        onStartNoteSelection,
        onExitNoteSelection,
        onSelectAllVisibleNotes,
        onDeselectAllNotes,
        onShareSelectedNotes,
        onCopySelectedNotesAsText,
        onCopySelectedNotesAsMarkdown,
        onDeleteSelectedNotes,
        onToggleNoteSelection
    ) {
        buildAgendaActions(
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            onNewNote = onNewNote,
            onRequestNoteActions = onRequestNoteActions,
            onStartNoteSelection = onStartNoteSelection,
            onExitNoteSelection = onExitNoteSelection,
            onSelectAllVisibleNotes = onSelectAllVisibleNotes,
            onDeselectAllNotes = onDeselectAllNotes,
            onShareSelectedNotes = onShareSelectedNotes,
            onCopySelectedNotesAsText = onCopySelectedNotesAsText,
            onCopySelectedNotesAsMarkdown = onCopySelectedNotesAsMarkdown,
            onDeleteSelectedNotes = onDeleteSelectedNotes,
            onToggleNoteSelection = onToggleNoteSelection
        )
    }
}

private fun buildAgendaActions(
    viewModel: AgendaViewModel,
    onNoteClick: (Long) -> Unit,
    onNewNote: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onStartNoteSelection: () -> Unit,
    onExitNoteSelection: () -> Unit,
    onSelectAllVisibleNotes: () -> Unit,
    onDeselectAllNotes: () -> Unit,
    onShareSelectedNotes: () -> Unit,
    onCopySelectedNotesAsText: () -> Unit,
    onCopySelectedNotesAsMarkdown: () -> Unit,
    onDeleteSelectedNotes: () -> Unit,
    onToggleNoteSelection: (Note) -> Unit
): AgendaActions = AgendaActions(
    onPreviousMonth = viewModel::navigateToPreviousMonth,
    onNextMonth = viewModel::navigateToNextMonth,
    onGoToToday = { viewModel.selectToday() },
    onSelectDate = viewModel::selectDate,
    onSearchQueryChange = viewModel::onSearchQueryChange,
    onSearchToggle = viewModel::onSearchToggle,
    onToggleSort = viewModel::toggleSortOrder,
    onToggleView = viewModel::toggleViewMode,
    onRemoveTagFilter = viewModel::removeTagFilter,
    onClearTagFilters = viewModel::clearTagFilters,
    onNewNote = onNewNote,
    onNoteClick = onNoteClick,
    onTogglePin = viewModel::togglePin,
    onDuplicateNote = viewModel::duplicateNote,
    onRequestTrash = viewModel::requestTrash,
    onRequestNoteActions = onRequestNoteActions,
    onStartNoteSelection = onStartNoteSelection,
    onExitNoteSelection = onExitNoteSelection,
    onSelectAllVisibleNotes = onSelectAllVisibleNotes,
    onDeselectAllNotes = onDeselectAllNotes,
    onShareSelectedNotes = onShareSelectedNotes,
    onCopySelectedNotesAsText = onCopySelectedNotesAsText,
    onCopySelectedNotesAsMarkdown = onCopySelectedNotesAsMarkdown,
    onDeleteSelectedNotes = onDeleteSelectedNotes,
    onToggleNoteSelection = onToggleNoteSelection,
    onUndoTrash = viewModel::undoPendingTrash,
    onConfirmTrash = viewModel::confirmTrash
)

private fun AgendaViewModel.selectToday() {
    val today = Calendar.getInstance()
    selectDate(
        today.get(Calendar.YEAR),
        today.get(Calendar.MONTH),
        today.get(Calendar.DAY_OF_MONTH)
    )
}
