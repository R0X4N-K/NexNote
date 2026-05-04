package com.example.nexnote.ui.screen.editor

import com.example.nexnote.domain.model.NoteLinkCandidate
import com.example.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal fun buildNoteLinkTargetsFlow(
    uiState: StateFlow<EditorUiState>,
    observeNoteLinkCandidates: ObserveNoteLinkCandidatesUseCase?,
    scope: CoroutineScope
): StateFlow<List<NoteLinkTarget>> {
    val candidatesFlow = observeNoteLinkCandidates?.invoke() ?: flowOf(emptyList())
    val currentNoteIdFlow = uiState
        .map { it.noteId }
        .distinctUntilChanged()

    return combine(candidatesFlow, currentNoteIdFlow) { candidates, currentNoteId ->
        candidates
            .asSequence()
            .filter { candidate -> currentNoteId == EditorViewModel.NO_ID || candidate.id != currentNoteId }
            .map { candidate -> candidate.toNoteLinkTarget() }
            .sortedWith(compareBy<NoteLinkTarget> { it.title.lowercase() }.thenBy { it.id })
            .toList()
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}

private fun NoteLinkCandidate.toNoteLinkTarget(): NoteLinkTarget =
    NoteLinkTarget(
        id = id,
        title = title.trim().ifBlank { "Untitled note" }
    )
