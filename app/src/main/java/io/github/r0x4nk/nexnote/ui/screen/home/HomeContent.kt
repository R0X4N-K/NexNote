package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionLayoutDefaults
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.AutoScrollingTagRow
import io.github.r0x4nk.nexnote.ui.component.NoteCard
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    vaultPullEnabled: Boolean,
    onNoteClick: (Long) -> Unit,
    onOpenVault: () -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    floatingBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        HomeLoadingState(modifier)
    } else {
        HomeLoadedContent(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            selectionState = selectionState,
            vaultPullEnabled = vaultPullEnabled,
            onNoteClick = onNoteClick,
            onOpenVault = onOpenVault,
            onToggleTagFilter = onToggleTagFilter,
            onRemoveTagFilter = onRemoveTagFilter,
            onClearTagFilters = onClearTagFilters,
            onTogglePin = onTogglePin,
            onRequestTrash = onRequestTrash,
            onRequestNoteActions = onRequestNoteActions,
            onToggleNoteSelection = onToggleNoteSelection,
            floatingBottomPadding = floatingBottomPadding,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeLoadingState(modifier: Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeLoadedContent(
    uiState: HomeUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    vaultPullEnabled: Boolean,
    onNoteClick: (Long) -> Unit,
    onOpenVault: () -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    floatingBottomPadding: Dp,
    modifier: Modifier
) {
    val vaultPullState = rememberHomeVaultPullGestureState(
        enabled = vaultPullEnabled,
        onOpenVault = onOpenVault
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .homeVaultPullAccess(vaultPullState)
    ) {
        HomeVaultPullAccessIndicator(vaultPullState)
        HomeFilterBars(uiState, onToggleTagFilter, onRemoveTagFilter, onClearTagFilters)
        HomeNotesBody(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            selectionState = selectionState,
            onNoteClick = onNoteClick,
            onTogglePin = onTogglePin,
            onRequestTrash = onRequestTrash,
            onRequestNoteActions = onRequestNoteActions,
            onToggleNoteSelection = onToggleNoteSelection,
            bottomContentPadding = RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)
        )
    }
}

@Composable
private fun HomeFilterBars(
    uiState: HomeUiState,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit
) {
    HomeTopTags(uiState, onToggleTagFilter)
    TagFilterBar(
        selectedTags = uiState.selectedTagFilters,
        onTagRemove = onRemoveTagFilter,
        onClearAll = onClearTagFilters
    )
}

@Composable
private fun HomeTopTags(
    uiState: HomeUiState,
    onToggleTagFilter: (String) -> Unit
) {
    if (uiState.topTags.isNotEmpty() && !uiState.isSearchActive) {
        AutoScrollingTagRow(
            tags = uiState.topTags,
            onTagClick = onToggleTagFilter,
            selectedTags = uiState.selectedTagFilters,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun HomeNotesBody(
    uiState: HomeUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    if (uiState.notes.isEmpty()) {
        EmptyState(
            isSearchActive = uiState.isSearchActive,
            hasTagFilter = uiState.selectedTagFilters.isNotEmpty(),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )
    } else {
        HomeNoteCollection(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            selectionState = selectionState,
            onNoteClick = onNoteClick,
            onTogglePin = onTogglePin,
            onRequestTrash = onRequestTrash,
            onRequestNoteActions = onRequestNoteActions,
            onToggleNoteSelection = onToggleNoteSelection,
            bottomContentPadding = bottomContentPadding
        )
    }
}

@Composable
private fun HomeNoteCollection(
    uiState: HomeUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    val displayItems = rememberDisplayItems(uiState)

    if (uiState.viewMode == NoteListViewMode.GRID) {
        HomeNoteGrid(
            displayItems,
            noteCardStyle,
            gridState,
            selectionState,
            onNoteClick,
            onTogglePin,
            onRequestTrash,
            onRequestNoteActions,
            onToggleNoteSelection,
            bottomContentPadding
        )
    } else {
        HomeNoteList(
            displayItems,
            noteCardStyle,
            listState,
            selectionState,
            onNoteClick,
            onTogglePin,
            onRequestTrash,
            onRequestNoteActions,
            onToggleNoteSelection,
            bottomContentPadding
        )
    }
}

@Composable
private fun rememberDisplayItems(uiState: HomeUiState): List<ScoredNote> =
    remember(
        uiState.isSearchActive,
        uiState.searchQuery,
        uiState.scoredResults,
        uiState.notes
    ) {
        if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
            uiState.scoredResults
        } else {
            uiState.notes.map { ScoredNote(it, 0, emptyList(), emptyList()) }
        }
    }

@Composable
private fun HomeNoteGrid(
    displayItems: List<ScoredNote>,
    noteCardStyle: NoteCardStyle,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = NoteCollectionLayoutDefaults.gridContentPadding(
            bottomPadding = bottomContentPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(NoteCollectionLayoutDefaults.itemSpacing),
        verticalItemSpacing = NoteCollectionLayoutDefaults.itemSpacing
    ) {
        items(
            items = displayItems,
            key = { scored -> scored.note.id },
            contentType = { "note_card" }
        ) { scored ->
            HomeNoteCard(
                scored,
                noteCardStyle,
                selectionState,
                onNoteClick,
                onTogglePin,
                onRequestTrash,
                onRequestNoteActions,
                onToggleNoteSelection,
                Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun HomeNoteList(
    displayItems: List<ScoredNote>,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = NoteCollectionLayoutDefaults.listContentPadding(
            bottomPadding = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(NoteCollectionLayoutDefaults.itemSpacing)
    ) {
        items(
            items = displayItems,
            key = { scored -> scored.note.id },
            contentType = { "note_card" }
        ) { scored ->
            HomeNoteCard(
                scored,
                noteCardStyle,
                selectionState,
                onNoteClick,
                onTogglePin,
                onRequestTrash,
                onRequestNoteActions,
                onToggleNoteSelection,
                Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun HomeNoteCard(
    scored: ScoredNote,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val note = scored.note
    val noteId = note.id
    NoteCard(
        note = note,
        onClick = remember(note, noteId, selectionState.isActive, onNoteClick, onToggleNoteSelection) {
            {
                if (selectionState.isActive) {
                    onToggleNoteSelection(note)
                } else {
                    onNoteClick(noteId)
                }
            }
        },
        noteCardStyle = noteCardStyle,
        titleHighlightRanges = scored.titleRanges,
        contentHighlightRanges = scored.contentRanges,
        onPin = remember(note, onTogglePin) { { onTogglePin(note) } },
        onLongPress = remember(note, onToggleNoteSelection) { { onToggleNoteSelection(note) } },
        onActions = remember(note, selectionState.isActive, onRequestNoteActions) {
            if (selectionState.isActive) {
                null
            } else {
                { onRequestNoteActions(note) }
            }
        },
        selectionMode = selectionState.isActive,
        selected = selectionState.isSelected(noteId),
        modifier = modifier,
        onTrash = remember(note, onRequestTrash) { { onRequestTrash(note) } }
    )
}
