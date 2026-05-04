package com.example.nexnote.ui.screen.trash

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    navController: NavController,
    viewModel: TrashViewModel = viewModel(factory = TrashViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TrashTopBar(
                showEmptyTrash = uiState.notes.isNotEmpty(),
                onBack = { navController.popBackStack() },
                onEmptyTrash = { viewModel.requestEmptyTrash() }
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

    TrashScreenDialogs(uiState, viewModel)
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
    onEmptyTrash: () -> Unit
) {
    TopAppBar(
        title = { Text("Trash") },
        navigationIcon = { TrashBackButton(onBack) },
        actions = {
            if (showEmptyTrash) TrashEmptyButton(onEmptyTrash)
        }
    )
}

@Composable
private fun TrashBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Go back"
        )
    }
}

@Composable
private fun TrashEmptyButton(onEmptyTrash: () -> Unit) {
    IconButton(onClick = onEmptyTrash) {
        Icon(
            imageVector        = Icons.Default.DeleteForever,
            contentDescription = "Empty trash"
        )
    }
}
