package io.github.r0x4nk.nexnote

import android.app.Application
import io.github.r0x4nk.nexnote.data.local.AttachmentImportRecovery
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.local.InternalNoteImageStorage
import io.github.r0x4nk.nexnote.data.preferences.UserPreferencesRepository
import io.github.r0x4nk.nexnote.data.security.AndroidVaultCredentialRepository
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.PendingImageCleanup
import io.github.r0x4nk.nexnote.util.runCatchingPreservingCancellation
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import io.github.r0x4nk.nexnote.data.repository.NoteStatisticsRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TagRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TemplateRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.VaultNoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.VaultRepositoryImpl
import io.github.r0x4nk.nexnote.di.AppUseCases
import io.github.r0x4nk.nexnote.di.AppDependencies
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository
import io.github.r0x4nk.nexnote.domain.repository.TagRepository
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.ui.screen.editor.EditorSaveCoordinator
import io.github.r0x4nk.nexnote.ui.screen.export.ExportCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Application class. Acts as the manual DI root for app dependencies.
 * ViewModels access ready-made use cases via APPLICATION_KEY in viewModelFactory.
 *
 * [appScope] uses a SupervisorJob: a failure in one child does not cancel siblings.
 * Individual IO operations still choose Dispatchers.IO at their own boundary.
 */
internal class NexNoteApp : Application(), AppDependencies {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val editorSaveCoordinator: EditorSaveCoordinator by lazy {
        EditorSaveCoordinator(ownerScope = appScope)
    }

    override val strings: StringProvider = StringProvider { id, formatArgs ->
        getString(id, *formatArgs)
    }

    val database: NexNoteDatabase by lazy {
        NexNoteDatabase.getDatabase(this)
    }

    private val internalFileStorage by lazy { InternalNoteImageStorage(filesDir) }

    val noteImageStorage: NoteImageStorage get() = internalFileStorage

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(
            dao = database.noteDao(),
            imageStorage = noteImageStorage,
            appScope = appScope,
            database = database,
            statisticsDao = database.noteStatisticsDao(),
            homeNoteDao = database.homeNoteDao(),
            pendingImageDeletionDao = database.pendingImageDeletionDao()
        )
    }

    private val statisticsRepository: NoteStatisticsRepositoryImpl by lazy {
        NoteStatisticsRepositoryImpl(
            dao = database.noteStatisticsDao(),
            appScope = appScope
        )
    }

    private val templateRepositoryImpl: TemplateRepositoryImpl by lazy {
        TemplateRepositoryImpl(database.templateDao(), strings)
    }

    val templateRepository: TemplateRepository by lazy {
        templateRepositoryImpl
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    private val vaultGraph: VaultGraph by lazy {
        val keyRepository = VaultRepositoryImpl(this)
        val noteRepository = VaultNoteRepositoryImpl(
            database = database,
            dao = database.noteDao(),
            tagDao = database.tagDao(),
            keyProvider = keyRepository,
            imageStorage = noteImageStorage,
            statisticsDao = database.noteStatisticsDao()
        )
        keyRepository.bindNoteMaintenance(
            rewrapper = noteRepository,
            wiper = noteRepository
        )
        VaultGraph(keyRepository, noteRepository)
    }

    val vaultRepository: VaultRepository
        get() = vaultGraph.keyRepository

    val vaultAndroidCredentialRepository: VaultAndroidCredentialRepository by lazy {
        AndroidVaultCredentialRepository(this)
    }

    val vaultNoteRepository: VaultNoteRepository
        get() = vaultGraph.noteRepository

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

    override val useCases: AppUseCases by lazy {
        AppUseCases(
            noteRepository = noteRepository,
            tagRepository = tagRepository,
            templateRepository = templateRepository,
            preferencesRepository = userPreferencesRepository,
            statisticsRepository = statisticsRepository,
            imageStorage = noteImageStorage,
            attachmentStorage = internalFileStorage,
            vaultRepository = vaultRepository,
            vaultAndroidCredentialRepository = vaultAndroidCredentialRepository,
            vaultNoteRepository = vaultNoteRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        statisticsRepository.start()
        appScope.launch(Dispatchers.IO) {
            val recovery = AttachmentImportRecovery(filesDir, database.noteDao())
            vaultGraph.keyRepository.unlockedVaultKey.collect { key ->
                runCatchingPreservingCancellation {
                    if (key == null) recovery.run() else {
                        vaultGraph.keyRepository.withUnlockedVaultKey { activeKey -> recovery.run(activeKey) }
                    }
                }.onFailure {
                    NexNoteDebugLog.repositoryWarning(event = "attachmentImportRecoveryFailed") {
                        "error=${it::class.java.simpleName}"
                    }
                }
            }
        }
        appScope.launch(Dispatchers.IO) {
            ExportCache(cacheDir).cleanupExpired()
        }
        appScope.launch(Dispatchers.IO) {
            val cleanup = PendingImageCleanup(database.pendingImageDeletionDao(), noteImageStorage)
            while (isActive) {
                runCatchingPreservingCancellation { cleanup.runOnce() }
                    .onFailure { error ->
                        NexNoteDebugLog.repositoryWarning(event = "pendingImageCleanupFailed") {
                            NexNoteDebugLog.throwableSummary(error)
                        }
                    }
                delay(15 * 60_000L)
            }
        }
        appScope.launch {
            if (!userPreferencesRepository.hasSeededPredefinedTemplates()) {
                templateRepositoryImpl.initializePredefinedTemplates()
                userPreferencesRepository.setPredefinedTemplatesSeeded()
            }
        }
    }

    private data class VaultGraph(
        val keyRepository: VaultRepositoryImpl,
        val noteRepository: VaultNoteRepositoryImpl
    )
}
