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
    val onNoteClick: (Long) -> Unit,
    val onTogglePin: (Note) -> Unit,
    val onRequestTrash: (Note) -> Unit,
    val onUndoTrash: (Long) -> Unit,
    val onConfirmTrash: (Long) -> Unit
)

@Composable
internal fun rememberAgendaActions(
    viewModel: AgendaViewModel,
    onNoteClick: (Long) -> Unit
): AgendaActions {
    return remember(viewModel, onNoteClick) {
        buildAgendaActions(viewModel, onNoteClick)
    }
}

private fun buildAgendaActions(
    viewModel: AgendaViewModel,
    onNoteClick: (Long) -> Unit
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
    onNoteClick = onNoteClick,
    onTogglePin = viewModel::togglePin,
    onRequestTrash = viewModel::requestTrash,
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
