package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNoteLinkCandidatesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal fun buildNoteLinkTargetsFlow(
    uiState: StateFlow<EditorUiState>,
    observeNoteLinkCandidates: ObserveNoteLinkCandidatesUseCase?,
    observeVaultNoteLinkCandidates: ObserveVaultNoteLinkCandidatesUseCase?,
    scope: CoroutineScope
): StateFlow<List<NoteLinkTarget>> {
    val normalCandidatesFlow = observeNoteLinkCandidates?.invoke() ?: flowOf(emptyList())
    val vaultCandidatesFlow = observeVaultNoteLinkCandidates?.invoke() ?: flowOf(emptyList())
    val linkScopeFlow = uiState
        .map { state -> NoteLinkTargetScope(isVaultNote = state.isVaultNote, noteId = state.noteId) }
        .distinctUntilChanged()

    return linkScopeFlow.flatMapLatest { linkScope ->
        val candidatesFlow = if (linkScope.isVaultNote) {
            vaultCandidatesFlow
        } else {
            normalCandidatesFlow
        }
        candidatesFlow.map { candidates -> candidates.toNoteLinkTargets(linkScope.noteId) }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}

private data class NoteLinkTargetScope(
    val isVaultNote: Boolean,
    val noteId: Long
)

private fun List<NoteLinkCandidate>.toNoteLinkTargets(currentNoteId: Long): List<NoteLinkTarget> =
    asSequence()
        .filter { candidate -> currentNoteId == EditorViewModel.NO_ID || candidate.id != currentNoteId }
        .map { candidate -> candidate.toNoteLinkTarget() }
        .sortedWith(compareBy<NoteLinkTarget> { it.title.lowercase() }.thenBy { it.id })
        .toList()

private fun NoteLinkCandidate.toNoteLinkTarget(): NoteLinkTarget =
    NoteLinkTarget(
        id = id,
        title = title.trim().ifBlank { "Untitled note" }
    )
