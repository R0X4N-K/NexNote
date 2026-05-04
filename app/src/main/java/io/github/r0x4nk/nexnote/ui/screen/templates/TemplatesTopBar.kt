package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplatesTopBar(
    uiState: TemplatesUiState,
    searchFocusRequester: FocusRequester,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    TopAppBar(
        title = {
            TemplatesTopBarTitle(
                uiState = uiState,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        actions = {
            TemplatesTopBarActions(
                uiState = uiState,
                onSearchToggle = onSearchToggle,
                onToggleSortOrder = onToggleSortOrder,
                onToggleViewMode = onToggleViewMode
            )
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun TemplatesTopBarTitle(
    uiState: TemplatesUiState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            TemplatesSearchField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                focusRequester = searchFocusRequester
            )
        }
        AnimatedVisibility(
            visible = !uiState.isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            Text("Templates")
        }
    }
}

@Composable
private fun TemplatesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        decorationBox = { innerTextField ->
            TemplatesSearchDecoration(value, innerTextField)
        }
    )
}

@Composable
private fun TemplatesSearchDecoration(
    value: String,
    innerTextField: @Composable () -> Unit
) {
    Box {
        if (value.isEmpty()) {
            Text(
                text = "Search templates…",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
        }
        innerTextField()
    }
}

@Composable
private fun TemplatesTopBarActions(
    uiState: TemplatesUiState,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    if (uiState.isSearchActive) {
        IconButton(onClick = { onSearchToggle(false) }) {
            Icon(Icons.Default.Close, contentDescription = "Close search")
        }
    } else {
        TemplatesDefaultActions(
            uiState = uiState,
            onSearchToggle = onSearchToggle,
            onToggleSortOrder = onToggleSortOrder,
            onToggleViewMode = onToggleViewMode
        )
    }
}

@Composable
private fun TemplatesDefaultActions(
    uiState: TemplatesUiState,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    TemplatesSortButton(uiState.sortOrder, onToggleSortOrder)
    TemplatesViewModeButton(uiState.viewMode, onToggleViewMode)
    IconButton(onClick = { onSearchToggle(true) }) {
        Icon(Icons.Default.Search, contentDescription = "Search")
    }
}

@Composable
private fun TemplatesSortButton(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit
) {
    IconButton(onClick = onToggleSortOrder) {
        Icon(
            imageVector = Icons.Default.SwapVert,
            contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC)
                "Sort: newest first" else "Sort: oldest first",
            tint = if (sortOrder == SortOrder.MODIFIED_ASC)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TemplatesViewModeButton(
    viewMode: NoteListViewMode,
    onToggleViewMode: () -> Unit
) {
    IconButton(onClick = onToggleViewMode) {
        Icon(
            imageVector = if (viewMode == NoteListViewMode.LIST)
                Icons.Default.GridView
            else
                Icons.AutoMirrored.Filled.ViewList,
            contentDescription = if (viewMode == NoteListViewMode.LIST)
                "Grid view" else "List view"
        )
    }
}
