package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.selectedItems
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuFabHideEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialFabActionEffect
import io.github.r0x4nk.nexnote.ui.navigation.Screen

@Composable
fun TemplatesScreen(
    onNavigateToApplyTemplate: (templateId: Long) -> Unit,
    onNavigateToEditTemplate: (editTemplateId: Long) -> Unit,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: TemplatesViewModel = viewModel(factory = TemplatesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectionState by rememberSaveable(stateSaver = SelectionUiState.Saver) {
        mutableStateOf(SelectionUiState())
    }
    val selectableTemplates = rememberSelectableTemplates(uiState)
    val selectableTemplateIds = remember(selectableTemplates) {
        selectableTemplates.map { it.id }
    }
    val selectedTemplates = remember(selectionState, selectableTemplates) {
        selectionState.selectedItems(selectableTemplates) { it.id }
    }
    val newTemplateAction = remember(onNavigateToEditTemplate) {
        { onNavigateToEditTemplate(Screen.NEW_TEMPLATE_ID) }
    }

    RadialFabActionEffect(
        contentDescription = "Create template",
        onClick = newTemplateAction
    )
    RadialMenuFabHideEffect(selectionState.isActive)

    TemplatesErrorSnackbar(
        errorMessage = uiState.errorMessage,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::clearError
    )
    TemplatesScreenLayout(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        floatingBottomPadding = floatingBottomPadding,
        selectionState = selectionState,
        selectableTemplateIds = selectableTemplateIds,
        actions = TemplatesLayoutActions(
            onNavigateToApplyTemplate = onNavigateToApplyTemplate,
            onNavigateToEditTemplate = onNavigateToEditTemplate,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearchToggle = viewModel::onSearchToggle,
            onToggleSortOrder = viewModel::toggleSortOrder,
            onToggleViewMode = viewModel::toggleViewMode,
            onRequestDelete = viewModel::requestDelete,
            onStartSelection = { selectionState = selectionState.enter() },
            onExitSelection = { selectionState = selectionState.exit() },
            onSelectAll = {
                selectionState = selectionState.selectAll(selectableTemplateIds)
            },
            onDeselectAll = {
                selectionState = selectionState.deselectAll()
            },
            onDeleteSelected = {
                viewModel.requestDeleteSelection(selectedTemplates)
            },
            onToggleSelection = { template ->
                selectionState = selectionState.toggle(template.id)
            }
        )
    )
    TemplatesSelectionCleanupEffect(
        selectionState = selectionState,
        selectableIds = selectableTemplateIds,
        onSelectionChange = { selectionState = it }
    )
    BackHandler(enabled = selectionState.isActive) {
        selectionState = selectionState.exit()
    }
    TemplatesDeleteDialog(
        dialog = uiState.activeDialog,
        onConfirmDelete = {
            viewModel.confirmDelete()
            selectionState = selectionState.exit()
        },
        onDismiss = viewModel::closeDialog
    )
}

@Composable
private fun TemplatesErrorSnackbar(
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit
) {
    LaunchedEffect(errorMessage, snackbarHostState, onErrorShown) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onErrorShown()
        }
    }
}

@Composable
private fun rememberSelectableTemplates(uiState: TemplatesUiState): List<Template> =
    remember(uiState.predefined, uiState.custom) {
        uiState.predefined + uiState.custom
    }

@Composable
private fun TemplatesSelectionCleanupEffect(
    selectionState: SelectionUiState,
    selectableIds: List<Long>,
    onSelectionChange: (SelectionUiState) -> Unit
) {
    LaunchedEffect(selectionState, selectableIds) {
        val retained = selectionState.retainSelectableIds(selectableIds)
        if (retained != selectionState) {
            onSelectionChange(retained)
        }
    }
}
