package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.r0x4nk.nexnote.NexNoteApp

internal fun homeViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NexNoteApp
        val useCases = app.useCases
        HomeViewModel(
            searchNotesScored = useCases.notes.searchNotesScored,
            observeAllNotesSortedAsc = useCases.notes.observeAllNotesSortedAsc,
            observeAllNotes = useCases.notes.observeAllNotes,
            moveNoteToTrash = useCases.notes.moveNoteToTrash,
            restoreNoteFromTrash = useCases.notes.restoreNoteFromTrash,
            toggleNotePin = useCases.notes.toggleNotePin,
            observeTemplates = useCases.templates.observeTemplates,
            observeMostUsedTags = useCases.tags.observeMostUsedTags,
            observeFilteredNoteIds = useCases.tags.observeFilteredNoteIds,
            observeNoteCardStyle = useCases.preferences.observeNoteCardStyle
        )
    }
}
