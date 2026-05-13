package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
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
    onNoteClick: (Long) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
            onNoteClick = onNoteClick,
            onToggleTagFilter = onToggleTagFilter,
            onRemoveTagFilter = onRemoveTagFilter,
            onClearTagFilters = onClearTagFilters,
            onTogglePin = onTogglePin,
            onRequestTrash = onRequestTrash,
            onRequestNoteActions = onRequestNoteActions,
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
    onNoteClick: (Long) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    floatingBottomPadding: Dp,
    modifier: Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        HomeFilterBars(uiState, onToggleTagFilter, onRemoveTagFilter, onClearTagFilters)
        HomeNotesBody(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            onNoteClick = onNoteClick,
            onTogglePin = onTogglePin,
            onRequestTrash = onRequestTrash,
            onRequestNoteActions = onRequestNoteActions,
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
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    if (uiState.notes.isEmpty()) {
        EmptyState(
            isSearchActive = uiState.isSearchActive,
            hasTagFilter = uiState.selectedTagFilters.isNotEmpty(),
            modifier = Modifier.fillMaxSize()
        )
    } else {
        HomeNoteCollection(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            onNoteClick = onNoteClick,
            onTogglePin = onTogglePin,
            onRequestTrash = onRequestTrash,
            onRequestNoteActions = onRequestNoteActions,
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
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    val displayItems = rememberDisplayItems(uiState)

    if (uiState.viewMode == NoteListViewMode.GRID) {
        HomeNoteGrid(
            displayItems,
            noteCardStyle,
            gridState,
            onNoteClick,
            onTogglePin,
            onRequestTrash,
            onRequestNoteActions,
            bottomContentPadding
        )
    } else {
        HomeNoteList(
            displayItems,
            noteCardStyle,
            listState,
            onNoteClick,
            onTogglePin,
            onRequestTrash,
            onRequestNoteActions,
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
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 8.dp,
            end = 12.dp,
            bottom = bottomContentPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(
            items = displayItems,
            key = { scored -> scored.note.id },
            contentType = { "note_card" }
        ) { scored ->
            HomeNoteCard(
                scored,
                noteCardStyle,
                onNoteClick,
                onTogglePin,
                onRequestTrash,
                onRequestNoteActions,
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
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    bottomContentPadding: Dp
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = displayItems,
            key = { scored -> scored.note.id },
            contentType = { "note_card" }
        ) { scored ->
            HomeNoteCard(
                scored,
                noteCardStyle,
                onNoteClick,
                onTogglePin,
                onRequestTrash,
                onRequestNoteActions,
                Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun HomeNoteCard(
    scored: ScoredNote,
    noteCardStyle: NoteCardStyle,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val note = scored.note
    val noteId = note.id
    NoteCard(
        note = note,
        onClick = remember(noteId, onNoteClick) { { onNoteClick(noteId) } },
        noteCardStyle = noteCardStyle,
        titleHighlightRanges = scored.titleRanges,
        contentHighlightRanges = scored.contentRanges,
        onPin = remember(note, onTogglePin) { { onTogglePin(note) } },
        onLongPress = remember(note, onRequestNoteActions) { { onRequestNoteActions(note) } },
        modifier = modifier,
        onTrash = remember(note, onRequestTrash) { { onRequestTrash(note) } }
    )
}
