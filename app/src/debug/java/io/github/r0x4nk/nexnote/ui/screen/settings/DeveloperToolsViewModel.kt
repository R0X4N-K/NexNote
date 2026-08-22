package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.di.requireAppDependencies
import io.github.r0x4nk.nexnote.domain.usecase.GenerateDebugNotesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DeveloperToolsUiState(
    val noteCountInput: String = GenerateDebugNotesUseCase.DEFAULT_NOTE_COUNT.toString(),
    val requestedCount: Int = 0,
    val generatedCount: Int = 0,
    val isGenerating: Boolean = false,
    val lastGeneratedCount: Int? = null,
    val error: DeveloperToolsError? = null
)

internal enum class DeveloperToolsError {
    INVALID_NOTE_COUNT,
    GENERATION_FAILED
}

/** Coordinates validation, progress and background execution for developer data generation. */
internal class DeveloperToolsViewModel(
    private val generateDebugNotes: GenerateDebugNotesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperToolsUiState())
    val uiState: StateFlow<DeveloperToolsUiState> = _uiState.asStateFlow()

    /** Updates the requested quantity while keeping the field numeric. */
    fun setNoteCountInput(value: String) {
        if (_uiState.value.isGenerating) return
        _uiState.update {
            it.copy(
                noteCountInput = value.filter(Char::isDigit),
                lastGeneratedCount = null,
                error = null
            )
        }
    }

    /** Validates the requested quantity and starts one generation job at a time. */
    fun generateNotes() {
        val state = _uiState.value
        if (state.isGenerating) return

        val count = state.noteCountInput.toIntOrNull()
            ?.takeIf {
                it in GenerateDebugNotesUseCase.MIN_NOTE_COUNT..GenerateDebugNotesUseCase.MAX_NOTE_COUNT
            }
        if (count == null) {
            _uiState.update { it.copy(error = DeveloperToolsError.INVALID_NOTE_COUNT) }
            return
        }

        _uiState.update {
            it.copy(
                requestedCount = count,
                generatedCount = 0,
                isGenerating = true,
                lastGeneratedCount = null,
                error = null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val generatedCount = generateDebugNotes(count) { progress ->
                    _uiState.update { it.copy(generatedCount = progress) }
                }
                _uiState.update {
                    it.copy(
                        generatedCount = generatedCount,
                        isGenerating = false,
                        lastGeneratedCount = generatedCount
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = DeveloperToolsError.GENERATION_FAILED
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = requireAppDependencies()
                DeveloperToolsViewModel(
                    generateDebugNotes = GenerateDebugNotesUseCase(
                        saveNote = app.useCases.notes.saveNote::invoke,
                        indexNoteTags = app.useCases.tags.indexNoteTags::invoke
                    )
                )
            }
        }
    }
}
