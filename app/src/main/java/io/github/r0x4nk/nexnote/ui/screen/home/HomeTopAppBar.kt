package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu
import io.github.r0x4nk.nexnote.ui.component.NoteListSortButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopAppBar(
    uiState: HomeUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onStartSelection: () -> Unit
) {
    TopAppBar(
        title = {
            HomeTopAppBarTitle(
                uiState = uiState,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        actions = {
            HomeTopAppBarActions(
                uiState = uiState,
                onSearchToggle = onSearchToggle,
                onSortToggle = onSortToggle,
                onViewModeToggle = onViewModeToggle,
                onOpenTrash = onOpenTrash,
                onOpenVault = onOpenVault,
                onStartSelection = onStartSelection
            )
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun HomeTopAppBarTitle(
    uiState: HomeUiState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            HomeSearchField(
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
            HomeBrandTitle()
        }
    }
}

@Composable
private fun HomeBrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2D2D2A))
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "NexNote app icon",
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Notes",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
private fun HomeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    NexSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Search notes",
        modifier = Modifier
            .fillMaxWidth(),
        focusRequester = focusRequester,
        textStyle = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun HomeTopAppBarActions(
    uiState: HomeUiState,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onStartSelection: () -> Unit
) {
    if (uiState.isSearchActive) {
        NexIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = "Close search",
            onClick = { onSearchToggle(false) }
        )
    } else {
        HomeBrowsingActions(
            uiState = uiState,
            onSearchToggle = onSearchToggle,
            onSortToggle = onSortToggle,
            onViewModeToggle = onViewModeToggle,
            onOpenTrash = onOpenTrash,
            onOpenVault = onOpenVault,
            onStartSelection = onStartSelection
        )
    }
}

@Composable
private fun HomeBrowsingActions(
    uiState: HomeUiState,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onStartSelection: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        onClick = { onSearchToggle(true) }
    )
    NoteListSortButton(
        sortOrder = uiState.sortOrder,
        onToggleSortOrder = onSortToggle
    )
    HomeOverflowMenu(
        uiState = uiState,
        onViewModeToggle = onViewModeToggle,
        onOpenVault = onOpenVault,
        onOpenTrash = onOpenTrash,
        onStartSelection = onStartSelection
    )
}

@Composable
private fun HomeOverflowMenu(
    uiState: HomeUiState,
    onViewModeToggle: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenTrash: () -> Unit,
    onStartSelection: () -> Unit
) {
    NoteListOverflowMenu(
        viewMode = uiState.viewMode,
        onToggleViewMode = onViewModeToggle
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text("Select notes") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                onStartSelection()
            }
        )
        DropdownMenuItem(
            text = { Text("Access Vault") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                onOpenVault()
            }
        )
        DropdownMenuItem(
            text = { Text("Trash") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                onOpenTrash()
            }
        )
    }
}
