package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.di.requireAppDependencies

@Composable
fun ExportScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: ExportViewModel = viewModel(
        factory = if (noteId != 0L) ExportViewModel.factory(noteId) else ExportViewModel.Factory
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.requireAppDependencies()
    val coroutineScope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val imageFileProvider = remember(app) { app.useCases.images.getNoteImageFile::invoke }
    val exportManager = remember(context, imageFileProvider) {
        ExportManager(context, imageFileProvider)
    }
    val actions = rememberExportActions(
        uiState = uiState,
        viewModel = viewModel,
        context = context,
        coroutineScope = coroutineScope,
        exportManager = exportManager
    )

    ExportErrorSnackbar(
        error = uiState.error,
        snackbar = snackbar,
        onErrorShown = viewModel::clearError
    )
    ExportScreenLayout(
        uiState = uiState,
        hasInitialNote = viewModel.initialNoteId != 0L,
        snackbar = snackbar,
        onBack = onBack,
        actions = actions
    )
}

@Composable
private fun ExportErrorSnackbar(
    error: String?,
    snackbar: SnackbarHostState,
    onErrorShown: () -> Unit
) {
    LaunchedEffect(error, snackbar, onErrorShown) {
        error?.let {
            snackbar.showSnackbar(it)
            onErrorShown()
        }
    }
}
