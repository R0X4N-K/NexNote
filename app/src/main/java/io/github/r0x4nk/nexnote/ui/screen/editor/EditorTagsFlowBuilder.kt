package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import io.github.r0x4nk.nexnote.util.TagParser
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
internal fun buildTagsForCurrentNoteFlow(
    uiState: StateFlow<EditorUiState>,
    observeTagsForNote: ObserveTagsForNoteUseCase?,
    scope: CoroutineScope
): StateFlow<List<Tag>> {
    return uiState
        .map { state -> state.toEditorTagSource() }
        .distinctUntilChanged()
        .flatMapLatest { source ->
            when (source) {
                EditorTagSource.Empty -> flowOf(emptyList())
                is EditorTagSource.Normal -> {
                    if (observeTagsForNote == null) {
                        flowOf(emptyList())
                    } else {
                        observeTagsForNote(source.noteId)
                    }
                }
                is EditorTagSource.Vault -> flowOf(source.toTags())
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

private sealed interface EditorTagSource {
    data object Empty : EditorTagSource
    data class Normal(val noteId: Long) : EditorTagSource
    data class Vault(
        val content: String,
        val creationDate: Long,
        val lastModifiedDate: Long?
    ) : EditorTagSource
}

private fun EditorUiState.toEditorTagSource(): EditorTagSource =
    when {
        isVaultNote && !isVaultLocked -> EditorTagSource.Vault(
            content = content,
            creationDate = creationDate,
            lastModifiedDate = lastModifiedDate
        )
        isVaultNote -> EditorTagSource.Empty
        noteId == EditorViewModel.NO_ID -> EditorTagSource.Empty
        else -> EditorTagSource.Normal(noteId)
    }

private fun EditorTagSource.Vault.toTags(): List<Tag> {
    val timestamp = lastModifiedDate ?: creationDate
    return TagParser.extractTags(content)
        .sorted()
        .map { tagName ->
            Tag(
                name = tagName,
                noteCount = 1,
                createdDate = creationDate,
                lastUpdatedDate = timestamp
            )
        }
}
