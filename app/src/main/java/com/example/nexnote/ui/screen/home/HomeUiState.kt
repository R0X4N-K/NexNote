package com.example.nexnote.ui.screen.home

import androidx.compose.runtime.Immutable
import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.ScoredNote
import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.model.Template
import com.example.nexnote.ui.common.NoteListViewMode
import com.example.nexnote.ui.common.SortOrder

@Immutable
data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = true,
    val scoredResults: List<ScoredNote> = emptyList(),
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST,
    val showTemplatePicker: Boolean = false,
    val templates: List<Template> = emptyList(),
    val selectedTagFilters: Set<String> = emptySet(),
    val topTags: List<Tag> = emptyList()
)
