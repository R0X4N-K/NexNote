package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val newTemplateAction = remember(onNavigateToEditTemplate) {
        { onNavigateToEditTemplate(Screen.NEW_TEMPLATE_ID) }
    }

    RadialFabActionEffect(
        contentDescription = "Create template",
        onClick = newTemplateAction
    )

    TemplatesErrorSnackbar(
        errorMessage = uiState.errorMessage,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::clearError
    )
    TemplatesScreenLayout(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        floatingBottomPadding = floatingBottomPadding,
        actions = TemplatesLayoutActions(
            onNavigateToApplyTemplate = onNavigateToApplyTemplate,
            onNavigateToEditTemplate = onNavigateToEditTemplate,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearchToggle = viewModel::onSearchToggle,
            onToggleSortOrder = viewModel::toggleSortOrder,
            onToggleViewMode = viewModel::toggleViewMode,
            onRequestDelete = viewModel::requestDelete
        )
    )
    TemplatesDeleteDialog(
        dialog = uiState.activeDialog,
        onConfirmDelete = viewModel::confirmDelete,
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
