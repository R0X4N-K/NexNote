package com.example.nexnote.ui.screen.editor

import androidx.compose.runtime.Immutable
import java.util.TimeZone

data class TagSearchState(
    val tagName: String,
    val charOffset: Int,
    val occurrenceIndex: Int,
    val totalOccurrences: Int
)

@Immutable
data class EditorUiState(
    val noteId: Long = EditorViewModel.NO_ID,
    val templateId: Long = EditorViewModel.NO_ID,
    val isTemplateMode: Boolean = false,
    val isLoading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val isMarkdown: Boolean = false,
    val showPreview: Boolean = false,
    val openedDirectlyInPreview: Boolean = false,
    val creationDate: Long = System.currentTimeMillis(),
    val lastModifiedDate: Long? = null,
    val timezone: String = TimeZone.getDefault().id,
    val isPinned: Boolean = false,
    val imagePaths: List<String> = emptyList(),
    val backgroundColor: Int? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val contentVersion: Int = 0,
    val contentSelectionOffset: Int? = null
)
