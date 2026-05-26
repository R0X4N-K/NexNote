package io.github.r0x4nk.nexnote

import android.app.Application
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.local.InternalNoteImageStorage
import io.github.r0x4nk.nexnote.data.preferences.UserPreferencesRepository
import io.github.r0x4nk.nexnote.data.security.AndroidVaultCredentialRepository
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TagRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TemplateRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.VaultNoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.VaultRepositoryImpl
import io.github.r0x4nk.nexnote.di.AppUseCases
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class. Acts as the manual DI root for app dependencies.
 * ViewModels access ready-made use cases via APPLICATION_KEY in viewModelFactory.
 *
 * [appScope] uses a SupervisorJob: a failure in one child does not cancel siblings.
 * Individual IO operations still choose Dispatchers.IO at their own boundary.
 */
class NexNoteApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: NexNoteDatabase by lazy {
        NexNoteDatabase.getDatabase(this)
    }

    val noteImageStorage: NoteImageStorage by lazy {
        InternalNoteImageStorage(filesDir)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(
            dao = database.noteDao(),
            imageStorage = noteImageStorage,
            appScope = appScope
        )
    }

    private val templateRepositoryImpl: TemplateRepositoryImpl by lazy {
        TemplateRepositoryImpl(database.templateDao())
    }

    val templateRepository: TemplateRepository by lazy {
        templateRepositoryImpl
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    private val vaultRepositoryImpl: VaultRepositoryImpl by lazy {
        // The note rewrapper and the note wiper are provided lazily to break
        // the construction cycle: VaultNoteRepositoryImpl needs the
        // unlocked-key provider that this repository exposes, while
        // changePin/resetVault need the boundaries that VaultNoteRepositoryImpl
        // implements. The lambdas are invoked only on demand, by which time
        // both repositories exist.
        VaultRepositoryImpl(
            context = this,
            noteRewrapperProvider = { vaultNoteRepositoryImpl },
            noteWiperProvider = { vaultNoteRepositoryImpl }
        )
    }

    val vaultRepository: VaultRepository by lazy {
        vaultRepositoryImpl
    }

    val vaultAndroidCredentialRepository: VaultAndroidCredentialRepository by lazy {
        AndroidVaultCredentialRepository(this)
    }

    private val vaultNoteRepositoryImpl: VaultNoteRepositoryImpl by lazy {
        VaultNoteRepositoryImpl(
            database = database,
            dao = database.noteDao(),
            tagDao = database.tagDao(),
            keyProvider = vaultRepositoryImpl,
            imageStorage = noteImageStorage
        )
    }

    val vaultNoteRepository: VaultNoteRepository by lazy {
        vaultNoteRepositoryImpl
    }

    /**
     * Tag repository receives a narrow note-content patch DAO for tag deletion.
     * This keeps tag maintenance away from the full note persistence API.
     */
    val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(
            database = database,
            tagDao = database.tagDao(),
            noteContentPatchDao = database.noteContentPatchDao()
        )
    }

    internal val useCases: AppUseCases by lazy {
        AppUseCases(
            noteRepository = noteRepository,
            tagRepository = tagRepository,
            templateRepository = templateRepository,
            preferencesRepository = userPreferencesRepository,
            imageStorage = noteImageStorage,
            vaultRepository = vaultRepository,
            vaultAndroidCredentialRepository = vaultAndroidCredentialRepository,
            vaultNoteRepository = vaultNoteRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            if (!userPreferencesRepository.hasSeededPredefinedTemplates()) {
                templateRepositoryImpl.initializePredefinedTemplates()
                userPreferencesRepository.setPredefinedTemplatesSeeded()
            }
        }
    }
}
