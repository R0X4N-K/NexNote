package com.example.nexnote

import android.app.Application
import com.example.nexnote.data.db.NexNoteDatabase
import com.example.nexnote.data.local.InternalNoteImageStorage
import com.example.nexnote.data.preferences.UserPreferencesRepository
import com.example.nexnote.data.repository.NoteRepository
import com.example.nexnote.data.repository.TagRepository
import com.example.nexnote.data.repository.TemplateRepository
import com.example.nexnote.di.AppUseCases
import com.example.nexnote.domain.repository.NoteImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class. Acts as the manual DI root for app dependencies.
 * ViewModels access ready-made use cases via APPLICATION_KEY in viewModelFactory.
 *
 * [appScope] uses a SupervisorJob: a failure in one child does not cancel siblings.
 */
class NexNoteApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: NexNoteDatabase by lazy {
        NexNoteDatabase.getDatabase(this)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    val templateRepository: TemplateRepository by lazy {
        TemplateRepository(database.templateDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val noteImageStorage: NoteImageStorage by lazy {
        InternalNoteImageStorage(filesDir)
    }

    /**
     * Tag repository requires both the tag DAO and the note DAO. The note DAO
     * is used by [TagRepository.deleteTag] to update note content in-place when
     * a tag is removed from the index.
     */
    val tagRepository: TagRepository by lazy {
        TagRepository(database.tagDao(), database.noteDao())
    }

    internal val useCases: AppUseCases by lazy {
        AppUseCases(
            noteRepository = noteRepository,
            tagRepository = tagRepository,
            templateRepository = templateRepository,
            preferencesRepository = userPreferencesRepository,
            imageStorage = noteImageStorage
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            templateRepository.initializePredefinedTemplates()
        }
    }
}
