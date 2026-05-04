package com.example.nexnote.ui.screen.tags

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nexnote.NexNoteApp
import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.usecase.DeleteTagUseCase
import com.example.nexnote.domain.usecase.ObserveAllNotesUseCase
import com.example.nexnote.domain.usecase.ObserveFilteredNoteIdsUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByDateAscUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByDateDescUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByUsageAscUseCase
import com.example.nexnote.domain.usecase.ObserveTagsByUsageDescUseCase
import com.example.nexnote.domain.usecase.SearchTagsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Sort order ────────────────────────────────────────────────────────────────

/**
 * Available sort orders for the Tags screen scoreboard.
 * [USAGE_DESC] is the default (most-used tags first).
 */
enum class TagSortOrder {
    USAGE_DESC,
    USAGE_ASC,
    DATE_DESC,   // Most recently used first
    DATE_ASC     // Oldest tag first
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

/** Sealed hierarchy for modal dialogs on the Tags screen. */
sealed class TagsDialog {
    data object None : TagsDialog()
    /** Confirmation dialog before permanently removing a tag from all notes. */
    data class ConfirmDelete(val tag: Tag) : TagsDialog()
}

// ── UiState ───────────────────────────────────────────────────────────────────

/**
 * UI state for the Tags screen.
 *
 * [tags] is the full (possibly search-filtered) sorted list of tags.
 * [selectedTagName] is the tag whose associated notes are displayed inline.
 *   null = no expansion. Tapping the same tag again collapses the section.
 * [notesForSelectedTag] is the list of active notes for [selectedTagName].
 * [isLoading] is true only in the initial [StateFlow] emission.
 */
@Immutable
data class TagsUiState(
    val tags: List<Tag>                    = emptyList(),
    val searchQuery: String                = "",
    val sortOrder: TagSortOrder            = TagSortOrder.USAGE_DESC,
    val selectedTagName: String?           = null,
    val notesForSelectedTag: List<Note>    = emptyList(),
    val isLoading: Boolean                 = true,
    val activeDialog: TagsDialog           = TagsDialog.None
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * ViewModel for the Tags screen.
 *
 * Role: UI layer — bridges tag/note use cases with the composable UI.
 * Exposes a single [uiState] StateFlow following the same MVVM
 * pattern used by all other screens in this project.
 *
 * Navigation decision: tapping a tag in the scoreboard expands an inline note
 * list within the Tags screen itself. This avoids cross-screen state sharing and
 * is consistent with the "detail within the same destination" pattern used by the
 * Templates screen. The [onNoteClick] callback (passed to [TagsScreen]) handles
 * navigation to the Editor when the user taps a note in the expanded list.
 */
class TagsViewModel(
    private val observeTagsByUsageDesc: ObserveTagsByUsageDescUseCase,
    private val observeTagsByUsageAsc: ObserveTagsByUsageAscUseCase,
    private val observeTagsByDateDesc: ObserveTagsByDateDescUseCase,
    private val observeTagsByDateAsc: ObserveTagsByDateAscUseCase,
    private val searchTags: SearchTagsUseCase,
    private val observeFilteredNoteIds: ObserveFilteredNoteIdsUseCase,
    private val observeAllNotes: ObserveAllNotesUseCase,
    private val deleteTag: DeleteTagUseCase
) : ViewModel() {

    private val _searchQuery     = MutableStateFlow("")
    private val _sortOrder       = MutableStateFlow(TagSortOrder.USAGE_DESC)
    private val _selectedTagName = MutableStateFlow<String?>(null)
    private val _activeDialog    = MutableStateFlow<TagsDialog>(TagsDialog.None)

    private val tagsFlow = buildTagsFlow(
        searchQuery = _searchQuery,
        sortOrder = _sortOrder,
        observeTagsByUsageDesc = observeTagsByUsageDesc,
        observeTagsByUsageAsc = observeTagsByUsageAsc,
        observeTagsByDateDesc = observeTagsByDateDesc,
        observeTagsByDateAsc = observeTagsByDateAsc,
        searchTags = searchTags
    )

    private val notesForSelected = buildNotesForSelectedTagFlow(
        selectedTagName = _selectedTagName,
        observeFilteredNoteIds = observeFilteredNoteIds,
        observeAllNotes = observeAllNotes
    )

    val uiState: StateFlow<TagsUiState> = buildTagsUiStateFlow(
        flows = TagsUiStateFlows(
            tags = tagsFlow,
            searchQuery = _searchQuery,
            sortOrder = _sortOrder,
            selectedTagName = _selectedTagName,
            notesForSelectedTag = notesForSelected,
            activeDialog = _activeDialog
        ),
        scope = viewModelScope
    )

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun clearSearch() {
        _searchQuery.update { "" }
    }

    // ── Sort ──────────────────────────────────────────────────────────────────

    fun setSortOrder(order: TagSortOrder) {
        _sortOrder.update { order }
    }

    // ── Tag selection ─────────────────────────────────────────────────────────

    /**
     * Toggles the selected tag: tapping an already-selected tag collapses the
     * inline note list; tapping a new tag expands it.
     */
    fun toggleTagSelection(tagName: String) {
        _selectedTagName.update { current ->
            if (current == tagName) null else tagName
        }
    }

    fun clearSelectedTag() {
        _selectedTagName.update { null }
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    /** Shows the confirmation dialog before deleting [tag]. */
    fun requestDeleteTag(tag: Tag) {
        _activeDialog.update { TagsDialog.ConfirmDelete(tag) }
    }

    fun dismissDialog() {
        _activeDialog.update { TagsDialog.None }
    }

    /**
     * Executes tag deletion after the user confirms the dialog.
     *
     * Collapses the inline note list if the deleted tag was selected, then
     * delegates to [DeleteTagUseCase] which:
     * - Replaces `#tagName` → `tagName` in all affected note contents.
     * - Removes all [NoteTagCrossRef] rows for this tag.
     * - Removes the [TagEntity] row.
     */
    fun confirmDeleteTag(tag: Tag) {
        _activeDialog.update { TagsDialog.None }
        _selectedTagName.update { current -> if (current == tag.name) null else current }

        viewModelScope.launch {
            deleteTag(tag.name)
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val useCases = app.useCases
                TagsViewModel(
                    observeTagsByUsageDesc = useCases.tags.observeTagsByUsageDesc,
                    observeTagsByUsageAsc = useCases.tags.observeTagsByUsageAsc,
                    observeTagsByDateDesc = useCases.tags.observeTagsByDateDesc,
                    observeTagsByDateAsc = useCases.tags.observeTagsByDateAsc,
                    searchTags = useCases.tags.searchTags,
                    observeFilteredNoteIds = useCases.tags.observeFilteredNoteIds,
                    observeAllNotes = useCases.notes.observeAllNotes,
                    deleteTag = useCases.tags.deleteTag
                )
            }
        }
    }
}
