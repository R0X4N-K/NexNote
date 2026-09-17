package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.FilterAlt
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu
import io.github.r0x4nk.nexnote.ui.component.NoteListSortButton
import io.github.r0x4nk.nexnote.ui.component.NoteSearchSortMenu
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.TagFolderExpandAllButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopAppBar(
    uiState: HomeUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    tagFolderExpansion: NoteTagFolderExpansionState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onOpenSearchFilters: () -> Unit,
    onSearchSortChange: (HomeSearchSort) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStatistics: () -> Unit,
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
                tagFolderExpansion = tagFolderExpansion,
                onSearchToggle = onSearchToggle,
                onOpenSearchFilters = onOpenSearchFilters,
                onSearchSortChange = onSearchSortChange,
                onSortToggle = onSortToggle,
                onViewModeToggle = onViewModeToggle,
                onOpenTrash = onOpenTrash,
                onOpenStatistics = onOpenStatistics,
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
            enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 8 },
            exit = fadeOut(tween(100)) + slideOutHorizontally(tween(120)) { it / 8 }
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
            HomeBrandTitle(
                noteCount = uiState.totalNoteCount,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
private fun HomeBrandTitle(noteCount: Int, isLoading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                contentDescription = stringResource(R.string.home_app_icon),
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    isLoading -> stringResource(R.string.home_loading_notes)
                    else -> pluralStringResource(
                        R.plurals.home_note_count,
                        noteCount,
                        noteCount
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
        placeholder = stringResource(R.string.home_search_notes),
        modifier = Modifier
            .fillMaxWidth(),
        focusRequester = focusRequester,
        textStyle = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun HomeTopAppBarActions(
    uiState: HomeUiState,
    tagFolderExpansion: NoteTagFolderExpansionState,
    onSearchToggle: (Boolean) -> Unit,
    onOpenSearchFilters: () -> Unit,
    onSearchSortChange: (HomeSearchSort) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenVault: () -> Unit,
    onStartSelection: () -> Unit
) {
    if (uiState.isSearchActive) {
        NoteSearchSortMenu(
            selected = uiState.searchSort,
            onSelect = onSearchSortChange
        )
        NexIconButton(
            imageVector = Icons.Default.FilterAlt,
            contentDescription = stringResource(R.string.common_filter_search_results),
            selected = uiState.hasActiveSearchFilters,
            onClick = onOpenSearchFilters
        )
        NexIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.common_close_search),
            onClick = { onSearchToggle(false) }
        )
    } else {
        HomeBrowsingActions(
            uiState = uiState,
            tagFolderExpansion = tagFolderExpansion,
            onSearchToggle = onSearchToggle,
            onSortToggle = onSortToggle,
            onViewModeToggle = onViewModeToggle,
            onOpenTrash = onOpenTrash,
            onOpenStatistics = onOpenStatistics,
            onOpenVault = onOpenVault,
            onStartSelection = onStartSelection
        )
    }
}

@Composable
private fun HomeBrowsingActions(
    uiState: HomeUiState,
    tagFolderExpansion: NoteTagFolderExpansionState,
    onSearchToggle: (Boolean) -> Unit,
    onSortToggle: () -> Unit,
    onViewModeToggle: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenVault: () -> Unit,
    onStartSelection: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.common_search),
        onClick = { onSearchToggle(true) }
    )
    NoteListSortButton(
        sortOrder = uiState.sortOrder,
        onToggleSortOrder = onSortToggle
    )
    if (uiState.viewMode == NoteListViewMode.TAGS && uiState.notes.isNotEmpty()) {
        TagFolderExpandAllButton(
            isAllCollapsed = tagFolderExpansion.isAllCollapsed,
            onClick = tagFolderExpansion::toggleAll
        )
    }
    HomeOverflowMenu(
        uiState = uiState,
        onViewModeToggle = onViewModeToggle,
        onOpenVault = onOpenVault,
        onOpenTrash = onOpenTrash,
        onOpenStatistics = onOpenStatistics,
        onStartSelection = onStartSelection
    )
}



@Composable
private fun HomeOverflowMenu(
    uiState: HomeUiState,
    onViewModeToggle: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStatistics: () -> Unit,
    onStartSelection: () -> Unit
) {
    NoteListOverflowMenu(
        viewMode = uiState.viewMode,
        onToggleViewMode = onViewModeToggle
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.statistics_title)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                onOpenStatistics()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.home_select_notes)) },
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
            text = { Text(stringResource(R.string.home_access_vault)) },
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
            text = { Text(stringResource(R.string.trash_title)) },
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
