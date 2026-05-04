package com.example.nexnote.ui.screen.editor

import com.example.nexnote.domain.model.Tag
import com.example.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildTagsForCurrentNoteFlow(
    uiState: StateFlow<EditorUiState>,
    observeTagsForNote: ObserveTagsForNoteUseCase?,
    scope: CoroutineScope
): StateFlow<List<Tag>> {
    return uiState
        .map { it.noteId }
        .flatMapLatest { noteId ->
            if (noteId == EditorViewModel.NO_ID || observeTagsForNote == null) {
                flowOf(emptyList())
            } else {
                observeTagsForNote(noteId)
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
