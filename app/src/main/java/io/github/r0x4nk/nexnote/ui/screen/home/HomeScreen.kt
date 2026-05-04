package io.github.r0x4nk.nexnote.ui.screen.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.ui.common.snackbarMessage

private const val EXIT_BACK_PRESS_WINDOW_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (noteId: Long) -> Unit,
    onNewNote: () -> Unit,
    onNewNoteFromTemplate: (templateId: Long) -> Unit,
    onOpenTrash: () -> Unit,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val noteCardStyle by viewModel.noteCardStyle.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    TrashEventsEffect(viewModel, snackbarHostState)
    DoubleBackToExitHandler()
    HomeRadialMenu(onNewNote = onNewNote, onTemplateClick = viewModel::showTemplatePicker) {
        viewModel.onSearchToggle(true)
    }
    SearchFocusEffect(uiState.isSearchActive, searchFocusRequester)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { HomeSnackbarHost(snackbarHostState, floatingBottomPadding) },
        topBar = {
            HomeTopAppBar(
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearchToggle = viewModel::onSearchToggle,
                onSortToggle = viewModel::toggleSortOrder,
                onViewModeToggle = viewModel::toggleViewMode,
                onOpenTrash = onOpenTrash
            )
        }
    ) { innerPadding ->
        HomeContent(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            onNoteClick = onNoteClick,
            onToggleTagFilter = viewModel::toggleTagFilter,
            onRemoveTagFilter = viewModel::removeTagFilter,
            onClearTagFilters = viewModel::clearTagFilters,
            onTogglePin = viewModel::togglePin,
            onRequestTrash = viewModel::requestTrash,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (uiState.showTemplatePicker) {
        TemplatePickerDialog(
            templates = uiState.templates,
            onSelect = { templateId ->
                viewModel.dismissTemplatePicker()
                onNewNoteFromTemplate(templateId)
            },
            onDismiss = viewModel::dismissTemplatePicker
        )
    }
}

@Composable
private fun TrashEventsEffect(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.trashEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.snackbarMessage(),
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoPendingTrash(event.noteId)
            } else {
                viewModel.confirmTrash(event.noteId)
            }
        }
    }
}

@Composable
private fun DoubleBackToExitHandler() {
    val context = LocalContext.current
    var lastBackPressMs by remember { mutableLongStateOf(0L) }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressMs < EXIT_BACK_PRESS_WINDOW_MS) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressMs = now
            Toast.makeText(context, "Press again to exit", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun HomeRadialMenu(
    onNewNote: () -> Unit,
    onTemplateClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    RadialMenuEffect(items = remember(onNewNote, onTemplateClick, onSearchClick) {
        listOf(
            RadialMenuItem(Icons.Default.Add, "", action = onNewNote),
            RadialMenuItem(Icons.Default.Description, "", action = onTemplateClick),
            RadialMenuItem(Icons.Default.Search, "", action = onSearchClick)
        )
    })
}

@Composable
private fun SearchFocusEffect(
    isSearchActive: Boolean,
    searchFocusRequester: FocusRequester
) {
    LaunchedEffect(isSearchActive, searchFocusRequester) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun HomeSnackbarHost(
    snackbarHostState: SnackbarHostState,
    floatingBottomPadding: Dp
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(
            bottom = RadialMenuOverlayDefaults.snackbarBottomPadding(floatingBottomPadding)
        )
    ) { data ->
        Snackbar(snackbarData = data)
    }
}
