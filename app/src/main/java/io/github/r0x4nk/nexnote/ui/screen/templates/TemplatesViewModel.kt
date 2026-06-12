package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.common.nextIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Dialog ────────────────────────────────────────────────────────────────────

sealed class TemplatesDialog {
    data object None : TemplatesDialog()
    data class ConfirmDelete(val template: Template) : TemplatesDialog()
    data class ConfirmDeleteSelection(val templates: List<Template>) : TemplatesDialog()
}

// ── UiState ───────────────────────────────────────────────────────────────────

@Immutable
data class TemplatesUiState(
    val predefined: List<Template> = emptyList(),
    val custom: List<Template> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activeDialog: TemplatesDialog = TemplatesDialog.None,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val viewMode: NoteListViewMode = NoteListViewMode.LIST
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class TemplatesViewModel(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val deleteTemplate: DeleteTemplateUseCase
) : ViewModel() {

    private val _searchQuery  = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _sortOrder    = MutableStateFlow(SortOrder.MODIFIED_DESC)
    private val _viewMode     = MutableStateFlow(NoteListViewMode.LIST)
    private val _extra        = MutableStateFlow(TemplatesExtraState())

    /** Dialog and error state kept separate from the repository flow. */
    val uiState: StateFlow<TemplatesUiState> = buildTemplatesUiStateFlow(
        flows = TemplatesUiStateFlows(
            templates = observeTemplates(),
            searchQuery = _searchQuery,
            isSearchActive = _isSearchActive,
            sortOrder = _sortOrder,
            viewMode = _viewMode,
            extra = _extra
        ),
        scope = viewModelScope
    )

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onSearchToggle(active: Boolean) {
        _isSearchActive.update { active }
        if (!active) _searchQuery.update { "" }
    }

    // ── Sort / view ───────────────────────────────────────────────────────────

    fun toggleSortOrder() {
        _sortOrder.update { current ->
            if (current == SortOrder.MODIFIED_DESC) SortOrder.MODIFIED_ASC else SortOrder.MODIFIED_DESC
        }
    }

    fun toggleViewMode() {
        _viewMode.update { current -> current.nextIn(NoteListViewMode.listGridModes) }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun requestDelete(template: Template) {
        _extra.update { it.copy(activeDialog = TemplatesDialog.ConfirmDelete(template)) }
    }

    fun requestDeleteSelection(templates: List<Template>) {
        if (templates.isEmpty()) return
        _extra.update {
            it.copy(activeDialog = TemplatesDialog.ConfirmDeleteSelection(templates))
        }
    }

    fun closeDialog() {
        _extra.update { it.copy(activeDialog = TemplatesDialog.None) }
    }

    /**
     * Confirms and executes deletion of the selected template.
     * No-op if the active dialog is not ConfirmDelete (safe by construction).
     */
    fun confirmDelete() {
        val dialog = _extra.value.activeDialog
        viewModelScope.launch {
            try {
                when (dialog) {
                    is TemplatesDialog.ConfirmDelete -> deleteTemplate(dialog.template)
                    is TemplatesDialog.ConfirmDeleteSelection -> {
                        dialog.templates.forEach { template ->
                            deleteTemplate(template)
                        }
                    }
                    TemplatesDialog.None -> return@launch
                }
                closeDialog()
            } catch (e: Exception) {
                _extra.update {
                    it.copy(
                        activeDialog = TemplatesDialog.None,
                        errorMessage = if (dialog is TemplatesDialog.ConfirmDeleteSelection) {
                            "Could not delete templates"
                        } else {
                            "Could not delete template"
                        }
                    )
                }
            }
        }
    }

    fun clearError() {
        _extra.update { it.copy(errorMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
                val templates = app.useCases.templates
                TemplatesViewModel(
                    observeTemplates = templates.observeTemplates,
                    deleteTemplate = templates.deleteTemplate
                )
            }
        }
    }
}
