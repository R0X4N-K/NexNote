package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    navController: NavController,
    viewModel: TrashViewModel = viewModel(factory = TrashViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TrashTopBar(
                showEmptyTrash = uiState.notes.isNotEmpty(),
                onBack = { navController.popBackStack() },
                onEmptyTrash = { viewModel.requestEmptyTrash() },
                onRestoreAll = viewModel::restoreAll
            )
        }
    ) { innerPadding ->
        TrashContent(
            uiState = uiState,
            onRestore = { note -> viewModel.restoreNote(note.id) },
            onDeletePermanently = { note -> viewModel.requestDeletePermanently(note) },
            modifier = Modifier.padding(innerPadding)
        )
    }

    val progress by viewModel.operationProgress.collectAsStateWithLifecycle()
    if (progress == null) TrashScreenDialogs(uiState, viewModel)
    OperationProgressDialog(progress)
}

@Composable
private fun TrashScreenDialogs(
    uiState: TrashUiState,
    viewModel: TrashViewModel
) {
    DeleteNoteDialog(
        note = uiState.noteToDelete,
        onConfirm = { viewModel.confirmDeletePermanently() },
        onDismiss = { viewModel.cancelDelete() }
    )
    EmptyTrashDialog(
        visible = uiState.showEmptyTrashDialog,
        onConfirm = { viewModel.confirmEmptyTrash() },
        onDismiss = { viewModel.cancelEmptyTrash() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashTopBar(
    showEmptyTrash: Boolean,
    onBack: () -> Unit,
    onEmptyTrash: () -> Unit,
    onRestoreAll: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.trash_title),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = { TrashBackButton(onBack) },
        actions = {
            if (showEmptyTrash) {
                TrashRestoreAllButton(onRestoreAll)
                TrashEmptyButton(onEmptyTrash)
            }
        },
        colors = nexTopAppBarColors()
    )
}

@Composable
private fun TrashBackButton(onBack: () -> Unit) {
    NexIconButton(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(R.string.common_back),
        onClick = onBack
    )
}

@Composable
private fun TrashRestoreAllButton(onRestoreAll: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.RestoreFromTrash,
        contentDescription = stringResource(R.string.trash_restore_all),
        onClick = onRestoreAll
    )
}

@Composable
private fun TrashEmptyButton(onEmptyTrash: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.DeleteForever,
        contentDescription = stringResource(R.string.trash_empty_action),
        destructive = true,
        onClick = onEmptyTrash
    )
}