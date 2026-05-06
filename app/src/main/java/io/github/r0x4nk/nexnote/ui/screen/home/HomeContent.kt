package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.AutoScrollingTagRow
import io.github.r0x4nk.nexnote.ui.component.NoteCard
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyGridState,
    onNoteClick: (Long) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
    gridState: LazyGridState,
    onNoteClick: (Long) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
            onRequestNoteActions = onRequestNoteActions
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
    gridState: LazyGridState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit
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
            onRequestNoteActions = onRequestNoteActions
        )
    }
}

@Composable
private fun HomeNoteCollection(
    uiState: HomeUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyGridState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit
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
            onRequestNoteActions
        )
    } else {
        HomeNoteList(
            displayItems,
            noteCardStyle,
            listState,
            onNoteClick,
            onTogglePin,
            onRequestTrash,
            onRequestNoteActions
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
    gridState: LazyGridState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun HomeNoteList(
    displayItems: List<ScoredNote>,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    onNoteClick: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRequestTrash: (Note) -> Unit,
    onRequestNoteActions: (Note) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
        item { Spacer(Modifier.height(32.dp)) }
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
