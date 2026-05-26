package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.SelectionTopAppBar
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost

internal data class TemplatesLayoutActions(
    val onNavigateToApplyTemplate: (Long) -> Unit,
    val onNavigateToEditTemplate: (Long) -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onSearchToggle: (Boolean) -> Unit,
    val onToggleSortOrder: () -> Unit,
    val onToggleViewMode: () -> Unit,
    val onRequestDelete: (Template) -> Unit,
    val onStartSelection: () -> Unit,
    val onExitSelection: () -> Unit,
    val onSelectAll: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onDeleteSelected: () -> Unit,
    val onToggleSelection: (Template) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplatesScreenLayout(
    uiState: TemplatesUiState,
    snackbarHostState: SnackbarHostState,
    floatingBottomPadding: Dp,
    selectionState: SelectionUiState,
    selectableTemplateIds: List<Long>,
    actions: TemplatesLayoutActions
) {
    val searchFocusRequester = remember { FocusRequester() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    TemplatesScreenScaffold(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        searchFocusRequester = searchFocusRequester,
        scrollBehavior = scrollBehavior,
        floatingBottomPadding = floatingBottomPadding,
        selectionState = selectionState,
        selectableTemplateIds = selectableTemplateIds,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesScreenScaffold(
    uiState: TemplatesUiState,
    snackbarHostState: SnackbarHostState,
    searchFocusRequester: FocusRequester,
    scrollBehavior: TopAppBarScrollBehavior,
    floatingBottomPadding: Dp,
    selectionState: SelectionUiState,
    selectableTemplateIds: List<Long>,
    actions: TemplatesLayoutActions
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selectionState.isActive) {
                SelectionTopAppBar(
                    selectedCount = selectionState.selectedCount,
                    totalCount = selectableTemplateIds.size,
                    scrollBehavior = scrollBehavior,
                    onClose = actions.onExitSelection,
                    onSelectAll = actions.onSelectAll,
                    onDeselectAll = actions.onDeselectAll,
                    onDeleteSelected = actions.onDeleteSelected,
                    deleteContentDescription = "Delete selected templates"
                )
            } else {
                TemplatesTopBarSlot(uiState, searchFocusRequester, scrollBehavior, actions)
            }
        },
        // Material 3 "lift the FAB" snackbar host — see [RadialMenuSnackbarHost].
        snackbarHost = {
            RadialMenuSnackbarHost(
                hostState = snackbarHostState,
                bottomInset = floatingBottomPadding
            )
        }
    ) { padding ->
        TemplatesScreenContent(
            uiState = uiState,
            padding = padding,
            floatingBottomPadding = floatingBottomPadding,
            selectionState = selectionState,
            actions = actions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesTopBarSlot(
    uiState: TemplatesUiState,
    searchFocusRequester: FocusRequester,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: TemplatesLayoutActions
) {
    TemplatesTopBar(
        uiState = uiState,
        searchFocusRequester = searchFocusRequester,
        scrollBehavior = scrollBehavior,
        onSearchQueryChange = actions.onSearchQueryChange,
        onSearchToggle = actions.onSearchToggle,
        onToggleSortOrder = actions.onToggleSortOrder,
        onToggleViewMode = actions.onToggleViewMode,
        onStartSelection = actions.onStartSelection
    )
}

@Composable
private fun TemplatesScreenContent(
    uiState: TemplatesUiState,
    padding: PaddingValues,
    floatingBottomPadding: Dp,
    selectionState: SelectionUiState,
    actions: TemplatesLayoutActions
) {
    when {
        uiState.isLoading -> {
            TemplatesLoadingState(padding = padding)
        }

        uiState.predefined.isEmpty() && uiState.custom.isEmpty() -> {
            TemplatesEmptyState(
                isSearchActive = uiState.isSearchActive,
                padding = padding
            )
        }

        else -> {
            TemplatesCollection(
                uiState = uiState,
                padding = padding,
                floatingBottomPadding = floatingBottomPadding,
                selectionState = selectionState,
                onApply = actions.onNavigateToApplyTemplate,
                onEdit = actions.onNavigateToEditTemplate,
                onDelete = actions.onRequestDelete,
                onToggleSelection = actions.onToggleSelection
            )
        }
    }
}
