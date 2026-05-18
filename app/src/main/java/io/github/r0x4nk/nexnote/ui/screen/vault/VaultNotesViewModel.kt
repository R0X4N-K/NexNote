package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the unlocked Vault notes surface.
 *
 * When the Vault is not in the [VaultState.UNLOCKED] state the [notes] list is
 * always empty and the search state is reset: no Vault content must reach the
 * UI while the Vault is locked or not configured. The decryption itself is
 * performed by the data layer, which
 * already returns an empty flow when the in-memory Vault key is missing; this
 * ViewModel is an additional UI-level boundary on top of that contract.
 */
@Immutable
data class VaultNotesUiState(
    val isUnlocked: Boolean = false,
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST,
    val totalNoteCount: Int = 0
)

class VaultNotesViewModel(
    observeVaultState: ObserveVaultStateUseCase,
    observeVaultNotes: ObserveVaultNotesUseCase,
    private val moveNoteToVault: MoveNoteToVaultUseCase,
    private val removeNoteFromVault: RemoveNoteFromVaultUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _viewMode = MutableStateFlow(NoteListViewMode.LIST)
    private val _vaultActionMessages = Channel<String>(Channel.BUFFERED)
    val vaultActionMessages = _vaultActionMessages.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<VaultNotesUiState> =
        observeVaultState()
            .onEach { state ->
                if (state != VaultState.UNLOCKED) {
                    clearSearchState()
                }
            }
            .flatMapLatest { state ->
                if (state == VaultState.UNLOCKED) {
                    observeVaultNotes().map { notes ->
                        VaultNotesSource(isUnlocked = true, notes = notes)
                    }
                } else {
                    flowOf(VaultNotesSource(isUnlocked = false, notes = emptyList()))
                }
            }
            .combine(_searchQuery) { source, query -> source to query }
            .combine(_isSearchActive) { (source, query), isSearchActive ->
                VaultNotesFilterInput(
                    source = source,
                    query = query,
                    isSearchActive = isSearchActive
                )
            }
            .combine(_sortOrder) { filterInput, sortOrder ->
                filterInput to sortOrder
            }
            .combine(_viewMode) { (filterInput, sortOrder), viewMode ->
                val source = filterInput.source
                if (!source.isUnlocked) {
                    VaultNotesUiState(
                        sortOrder = sortOrder,
                        viewMode = viewMode
                    )
                } else {
                    val effectiveQuery = if (filterInput.isSearchActive) filterInput.query else ""
                    VaultNotesUiState(
                        isUnlocked = true,
                        notes = sortVaultNotes(
                            notes = filterVaultNotes(source.notes, effectiveQuery),
                            sortOrder = sortOrder
                        ),
                        searchQuery = effectiveQuery,
                        isSearchActive = filterInput.isSearchActive,
                        sortOrder = sortOrder,
                        viewMode = viewMode,
                        totalNoteCount = source.notes.size
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = VaultNotesUiState()
            )

    fun onSearchQueryChange(query: String) {
        if (uiState.value.isUnlocked) {
            _searchQuery.update { query }
        }
    }

    fun onSearchToggle(active: Boolean) {
        if (active && uiState.value.isUnlocked) {
            _isSearchActive.update { true }
        } else {
            clearSearchState()
        }
    }

    fun toggleSortOrder() {
        _sortOrder.update { current ->
            if (current == SortOrder.MODIFIED_DESC) {
                SortOrder.MODIFIED_ASC
            } else {
                SortOrder.MODIFIED_DESC
            }
        }
    }

    fun toggleViewMode() {
        _viewMode.update { current ->
            if (current == NoteListViewMode.LIST) {
                NoteListViewMode.GRID
            } else {
                NoteListViewMode.LIST
            }
        }
    }

    fun removeFromVault(note: Note) {
        if (!note.isInVault || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                val removed = removeNoteFromVault(note.id)
                _vaultActionMessages.trySend(
                    if (removed) {
                        "Note removed from Vault"
                    } else {
                        "Could not remove note from Vault"
                    }
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not remove note from Vault")
            }
        }
    }

    fun moveNormalNoteToVault(noteId: Long) {
        if (noteId <= 0L || !uiState.value.isUnlocked) return

        viewModelScope.launch {
            try {
                _vaultActionMessages.trySend(moveNoteToVault(noteId).toMessage())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _vaultActionMessages.trySend("Could not move note to Vault")
            }
        }
    }

    private fun clearSearchState() {
        _searchQuery.update { "" }
        _isSearchActive.update { false }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val vault = app.useCases.vault
                VaultNotesViewModel(
                    observeVaultState = vault.observeVaultState,
                    observeVaultNotes = vault.observeVaultNotes,
                    moveNoteToVault = vault.moveNoteToVault,
                    removeNoteFromVault = vault.removeNoteFromVault
                )
            }
        }
    }
}

private fun MoveNoteToVaultResult.toMessage(): String =
    when (this) {
        MoveNoteToVaultResult.Success -> "Note moved to Vault"
        MoveNoteToVaultResult.NotFound -> "Could not move note to Vault"
    }

private data class VaultNotesSource(
    val isUnlocked: Boolean,
    val notes: List<Note>
)

private data class VaultNotesFilterInput(
    val source: VaultNotesSource,
    val query: String,
    val isSearchActive: Boolean
)

private fun filterVaultNotes(notes: List<Note>, query: String): List<Note> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return notes

    return notes.filter { note ->
        note.title.contains(normalizedQuery, ignoreCase = true) ||
            note.content.contains(normalizedQuery, ignoreCase = true)
    }
}

private fun sortVaultNotes(notes: List<Note>, sortOrder: SortOrder): List<Note> {
    val comparator = when (sortOrder) {
        SortOrder.MODIFIED_DESC ->
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.lastModifiedDate }
                .thenBy { it.id }

        SortOrder.MODIFIED_ASC ->
            compareByDescending<Note> { it.isPinned }
                .thenBy { it.lastModifiedDate }
                .thenBy { it.id }
    }

    return notes.sortedWith(comparator)
}
