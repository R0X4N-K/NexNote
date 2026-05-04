package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect

/**
 * Tags screen route: connects [TagsViewModel] state to the Tags UI.
 *
 * Navigation: tapping a note in the expanded list calls [onNoteClick] to open
 * the Editor. The Tags screen remains self-contained and does not set global
 * cross-screen filter state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onNoteClick: (noteId: Long) -> Unit,
    viewModel: TagsViewModel = viewModel(factory = TagsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val searchFocusRequester = remember { FocusRequester() }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val actions = rememberTagsActions(
        viewModel = viewModel,
        onNoteClick = onNoteClick,
        onSearchActiveChange = { isSearchActive = it },
        onSortMenuChange = { showSortMenu = it }
    )

    RadialMenuEffect(items = emptyList())

    TagsScreenLayout(
        uiState = uiState,
        scrollBehavior = scrollBehavior,
        searchFocusRequester = searchFocusRequester,
        isSearchActive = isSearchActive,
        showSortMenu = showSortMenu,
        actions = actions
    )
    TagsDialogHost(
        activeDialog = uiState.activeDialog,
        actions = actions
    )
}
