package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.runtime.Composable
import io.github.r0x4nk.nexnote.domain.model.HomePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.HomeSearchScope
import io.github.r0x4nk.nexnote.ui.component.NoteSearchFiltersSheet

/** Adapts Home state to the shared note-search filter sheet. */
@Composable
internal fun HomeSearchFiltersSheet(
    uiState: HomeUiState,
    onSearchScopeChange: (HomeSearchScope) -> Unit,
    onPinnedFilterChange: (HomePinnedFilter) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    NoteSearchFiltersSheet(
        searchScope = uiState.searchScope,
        pinnedFilter = uiState.pinnedFilter,
        selectedTagFilters = uiState.selectedTagFilters,
        availableTagNames = uiState.topTags.map { tag -> tag.name },
        onSearchScopeChange = onSearchScopeChange,
        onPinnedFilterChange = onPinnedFilterChange,
        onToggleTagFilter = onToggleTagFilter,
        onClearTagFilters = onClearTagFilters,
        onDismiss = onDismiss
    )
}
