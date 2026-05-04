package io.github.r0x4nk.nexnote.ui.screen.editor

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class EditorTagNavigator(
    private val uiState: StateFlow<EditorUiState>,
    private val onError: (String) -> Unit
) {
    private val _selectedTagsInEditor = MutableStateFlow<String?>(null)
    val selectedTagsInEditor: StateFlow<String?> = _selectedTagsInEditor.asStateFlow()

    private val _tagSearchEvent = MutableSharedFlow<TagSearchState>(extraBufferCapacity = 1)
    val tagSearchEvent: SharedFlow<TagSearchState> = _tagSearchEvent.asSharedFlow()

    private var tagCycleIndex: Int = 0

    fun onTagChipClick(tagName: String) {
        val content = uiState.value.content
        val occurrences = findTagOccurrences(tagName, content)

        if (_selectedTagsInEditor.value == tagName) {
            if (occurrences.isNotEmpty()) {
                tagCycleIndex = (tagCycleIndex + 1) % occurrences.size
            }
        } else {
            _selectedTagsInEditor.update { tagName }
            tagCycleIndex = 0
        }

        if (occurrences.isEmpty()) {
            if (content.isNotEmpty()) onError("\"#$tagName\" not found in note")
            return
        }

        _tagSearchEvent.tryEmit(
            TagSearchState(
                tagName = tagName,
                charOffset = occurrences[tagCycleIndex],
                occurrenceIndex = tagCycleIndex,
                totalOccurrences = occurrences.size
            )
        )
    }

    fun clearSelection() {
        _selectedTagsInEditor.update { null }
        tagCycleIndex = 0
    }

    private fun findTagOccurrences(tagName: String, content: String): List<Int> {
        if (content.isEmpty()) return emptyList()
        val pattern = Regex(
            pattern = "#${Regex.escape(tagName)}(?![a-zA-Z0-9_])",
            option = RegexOption.IGNORE_CASE
        )
        return pattern.findAll(content).map { it.range.first }.toList()
    }
}
