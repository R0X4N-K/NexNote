package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.TemplateDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TemplateRepositoryImpl
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DecryptVaultImageBytesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteImageFileUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.IndexNoteTagsUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTagsForNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNotePreviewModeUseCase
import io.github.r0x4nk.nexnote.testing.NoOpTagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
abstract class EditorViewModelTestBase {

    protected val testDispatcher = StandardTestDispatcher()
    private lateinit var editorSaveOwner: CoroutineScope
    private lateinit var editorSaveCoordinator: EditorSaveCoordinator
    protected lateinit var fakeNoteDao: FakeEditorNoteDao
    protected lateinit var fakeTemplateDao: FakeEditorTemplateDao

    @Before
    fun setupEditorViewModelTestBase() {
        Dispatchers.setMain(testDispatcher)
        editorSaveOwner = CoroutineScope(SupervisorJob() + testDispatcher)
        editorSaveCoordinator = EditorSaveCoordinator(editorSaveOwner, testDispatcher)
        fakeNoteDao = FakeEditorNoteDao()
        fakeTemplateDao = FakeEditorTemplateDao()
    }

    @After
    fun tearDownEditorViewModelTestBase() {
        editorSaveOwner.cancel()
        Dispatchers.resetMain()
    }

    protected fun viewModel(
        noteId: Long = 0L,
        templateId: Long = 0L,
        editTemplateId: Long = 0L,
        creationDate: Long = EditorMode.NO_CREATION_DATE,
        mode: EditorMode = EditorMode.fromRoute(noteId, templateId, editTemplateId, creationDate),
        imageStorage: NoteImageStorage = FakeEditorNoteImageStorage(),
        preferencesRepository: IUserPreferencesRepository = FakeEditorPreferencesRepository(),
        getVaultNoteById: GetVaultNoteByIdUseCase? = null,
        saveVaultNote: SaveVaultNoteUseCase? = null,
        observeVaultNoteLinkCandidates: ObserveVaultNoteLinkCandidatesUseCase? = null,
        observeVaultState: ObserveVaultStateUseCase? = null,
        decryptVaultImageBytes: DecryptVaultImageBytesUseCase? = null
    ): EditorViewModel {
        val noteRepository = NoteRepositoryImpl(fakeNoteDao, imageStorage)
        val templateRepository = TemplateRepositoryImpl(fakeTemplateDao)
        val fallbackVaultNotes = FakeEditorVaultNoteRepository()
        val fallbackVaultState = FakeEditorVaultStateRepository()
        return EditorViewModel(
            copyNoteImageToInternal = CopyNoteImageToInternalUseCase(imageStorage),
            deleteNoteImage = DeleteNoteImageUseCase(imageStorage),
            getNoteImageFile = GetNoteImageFileUseCase(imageStorage),
            getNoteById = GetNoteByIdUseCase(noteRepository),
            getVaultNoteById = getVaultNoteById
                ?: GetVaultNoteByIdUseCase(fallbackVaultNotes),
            getTemplateById = GetTemplateByIdUseCase(templateRepository),
            saveNote = SaveNoteUseCase(noteRepository),
            saveTemplate = SaveTemplateUseCase(templateRepository),
            saveVaultNote = saveVaultNote
                ?: SaveVaultNoteUseCase(fallbackVaultNotes),
            setNotePreviewMode = SetNotePreviewModeUseCase(noteRepository),
            observeNoteLinkCandidates = ObserveNoteLinkCandidatesUseCase(noteRepository),
            observeVaultNoteLinkCandidates = observeVaultNoteLinkCandidates
                ?: ObserveVaultNoteLinkCandidatesUseCase(fallbackVaultNotes),
            observeTagsForNote = ObserveTagsForNoteUseCase(NoOpTagRepository),
            indexNoteTags = IndexNoteTagsUseCase(NoOpTagRepository),
            observeVaultState = observeVaultState
                ?: ObserveVaultStateUseCase(fallbackVaultState),
            observeThemeMode = ObserveThemeModeUseCase(preferencesRepository),
            decryptVaultImageBytesUseCase = decryptVaultImageBytes
                ?: DecryptVaultImageBytesUseCase(fallbackVaultNotes),
            saveCoordinator = editorSaveCoordinator,
            initialMode = mode
        )
    }
}

class FakeEditorVaultStateRepository(
    initialState: VaultState = VaultState.UNLOCKED
) : VaultRepository {
    private val stateFlow = MutableStateFlow(initialState)
    private val hasProtectedMaterial = MutableStateFlow(false)
    override val state: Flow<VaultState> = stateFlow
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        hasProtectedMaterial

    fun setState(state: VaultState) {
        stateFlow.value = state
    }

    override suspend fun configurePin(pin: CharArray) {
        stateFlow.value = VaultState.LOCKED
    }

    override suspend fun unlockWithPin(pin: CharArray): Boolean {
        stateFlow.value = VaultState.UNLOCKED
        return true
    }

    override suspend fun unlockWithAndroidCredential(): UnlockVaultWithAndroidCredentialResult =
        UnlockVaultWithAndroidCredentialResult.Failed

    override suspend fun refreshAndroidCredentialProtectedUnlockMaterial():
        RefreshVaultAndroidCredentialProtectedMaterialResult =
        RefreshVaultAndroidCredentialProtectedMaterialResult.Failed

    override suspend fun clearAndroidCredentialProtectedUnlockMaterial() = Unit

    override suspend fun changePin(
        currentPin: CharArray,
        newPin: CharArray
    ): ChangeVaultPinResult = ChangeVaultPinResult.RewrapFailed

    override suspend fun resetVault(): ResetVaultResult = ResetVaultResult.Failed

    override fun lock() {
        stateFlow.value = VaultState.LOCKED
    }
}

class FakeEditorVaultNoteRepository : VaultNoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L
    override val vaultNotes: Flow<List<Note>> = notes
    override val vaultTrashedNotes: Flow<List<Note>> = MutableStateFlow(emptyList())
    override val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>> =
        notes.map { list ->
            list.filter { it.isInVault && !it.isDeleted }
                .map { note -> NoteLinkCandidate(id = note.id, title = note.title) }
        }
    val savedNotes = mutableListOf<Note>()
    var failOnSave = false
    var saveFailure: Throwable? = null

    fun addNote(note: Note) {
        notes.value = notes.value + note
    }

    override suspend fun getVaultNoteById(id: Long): Note? =
        notes.value.firstOrNull { it.id == id && it.isInVault && !it.isDeleted }

    override suspend fun saveVaultNote(note: Note): Long {
        saveFailure?.let { throw it }
        if (failOnSave) throw RuntimeException("Vault save failed")
        val id = if (note.id == 0L) nextId++ else note.id
        val saved = note.copy(id = id, isInVault = true)
        savedNotes += saved
        notes.value = notes.value
            .filterNot { it.id == saved.id }
            .plus(saved)
        return saved.id
    }

    override suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult =
        DuplicateVaultNoteResult.NotFound

    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult {
        throw UnsupportedOperationException("Not needed for editor Vault read-only tests")
    }

    override suspend fun removeNoteFromVault(id: Long): Boolean {
        throw UnsupportedOperationException("Not needed for editor Vault tests")
    }

    override suspend fun moveVaultNoteToTrash(id: Long): Boolean {
        throw UnsupportedOperationException("Not needed for editor Vault tests")
    }

    override suspend fun restoreVaultNoteFromTrash(id: Long): Boolean {
        throw UnsupportedOperationException("Not needed for editor Vault tests")
    }

    override suspend fun deleteVaultNotePermanently(id: Long): Boolean {
        throw UnsupportedOperationException("Not needed for editor Vault tests")
    }

    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? = null
}

class FakeEditorNoteImageStorage : NoteImageStorage {
    val copiedNoteIds = mutableListOf<Long>()
    val deletedPaths = mutableListOf<String>()
    var failOnCopy = false
    var copyFailure: Throwable? = null
    var deleteFailure: Throwable? = null
    private var nextTimestamp = 100L

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String {
        copyFailure?.let { throw it }
        if (failOnCopy) throw RuntimeException("Copy failed")
        openInputStream()?.use { it.readBytes() }
        copiedNoteIds += noteId
        return "images/note_${noteId}_img_${nextTimestamp++}.jpg"
    }

    override suspend fun deleteImage(relativePath: String): Boolean {
        deletedPaths += relativePath
        deleteFailure?.let { throw it }
        return true
    }

    override fun getImageFile(relativePath: String): File = File(relativePath)
}

class FakeEditorPreferencesRepository : IUserPreferencesRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _fontScale = MutableStateFlow(FontScale.NORMAL)
    private val _timezoneId = MutableStateFlow("")
    private val _accentColor = MutableStateFlow(AccentColor.VIOLET)
    private val _noteCardStyle = MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)
    private val _tableLayoutMode = MutableStateFlow(TableLayoutMode.FIT_SCREEN)
    private val _protectVaultRecentPreviews = MutableStateFlow(true)
    private val _lockVaultOnBackground = MutableStateFlow(true)
    private val _vaultAutoLockTimeout =
        MutableStateFlow(VaultAutoLockTimeout.IMMEDIATELY)
    private val _unlockVaultWithAndroidCredential = MutableStateFlow(false)

    var lastThemeMode: ThemeMode? = null

    override val themeMode: Flow<ThemeMode> = _themeMode
    override val fontScale: Flow<FontScale> = _fontScale
    override val timezoneId: Flow<String> = _timezoneId
    override val accentColor: Flow<AccentColor> = _accentColor
    override val noteCardStyle: Flow<NoteCardStyle> = _noteCardStyle
    override val tableLayoutMode: Flow<TableLayoutMode> = _tableLayoutMode
    override val protectVaultRecentPreviews: Flow<Boolean> = _protectVaultRecentPreviews
    override val lockVaultOnBackground: Flow<Boolean> = _lockVaultOnBackground
    override val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout> = _vaultAutoLockTimeout
    override val unlockVaultWithAndroidCredential: Flow<Boolean> =
        _unlockVaultWithAndroidCredential

    override suspend fun setThemeMode(mode: ThemeMode) {
        lastThemeMode = mode
        _themeMode.value = mode
    }

    override suspend fun setFontScale(scale: FontScale) {
        _fontScale.value = scale
    }

    override suspend fun setTimezoneId(id: String) {
        _timezoneId.value = id
    }

    override suspend fun setAccentColor(color: AccentColor) {
        _accentColor.value = color
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        _noteCardStyle.value = style
    }

    override suspend fun setTableLayoutMode(mode: TableLayoutMode) {
        _tableLayoutMode.value = mode
    }

    override suspend fun setProtectVaultRecentPreviews(value: Boolean) {
        _protectVaultRecentPreviews.value = value
    }

    override suspend fun setLockVaultOnBackground(value: Boolean) {
        _lockVaultOnBackground.value = value
    }

    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) {
        _vaultAutoLockTimeout.value = timeout
    }

    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) {
        _unlockVaultWithAndroidCredential.value = value
    }
}

class FakeEditorNoteDao : NoteDao {

    private val notes = mutableMapOf<Long, NoteEntity>()
    private var nextId = 1L
    var insertedCount = 0
    var updatedCount = 0
    var failOnInsert = false
    var insertFailure: Throwable? = null

    fun addNote(entity: NoteEntity) {
        notes[entity.id] = entity
    }

    override fun getAllNotes(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { !it.isDeleted }.toList())

    override fun getAllNotesSortedAsc(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { !it.isDeleted }.toList())

    override fun getDeletedNotes(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { it.isDeleted }.toList())

    override fun getDeletedVaultNotes(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { it.isDeleted && it.isInVault }.toList())

    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        MutableStateFlow(
            notes.values
                .filter { !it.isDeleted && !it.isInVault }
                .map { NoteLinkCandidateProjection(id = it.id, title = it.title) }
        )

    override fun getVaultNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        MutableStateFlow(
            notes.values
                .filter { !it.isDeleted && it.isInVault }
                .map { NoteLinkCandidateProjection(id = it.id, title = it.title) }
        )

    override suspend fun getNoteById(id: Long): NoteEntity? = notes[id]

    override fun getAllVaultNotes(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { !it.isDeleted && it.isInVault }.toList())

    override suspend fun getVaultNoteById(id: Long): NoteEntity? =
        notes[id]?.takeIf { !it.isDeleted && it.isInVault }

    override suspend fun getAllVaultNotesForWipeOnce(): List<NoteEntity> =
        notes.values.filter { it.isInVault }.toList()

    override suspend fun getDeletedVaultNoteById(id: Long): NoteEntity? =
        notes[id]?.takeIf { it.isInVault && it.isDeleted }

    override suspend fun deleteAllVaultNotes(): Int {
        val toRemove = notes.values.filter { it.isInVault }.map { it.id }
        toRemove.forEach { notes.remove(it) }
        return toRemove.size
    }

    override fun searchNotes(query: String): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override fun getAllCreationDates(): Flow<List<Long>> =
        MutableStateFlow(emptyList())

    override suspend fun insertNote(note: NoteEntity): Long {
        insertFailure?.let { throw it }
        if (failOnInsert) throw RuntimeException("Insert failed")
        insertedCount++
        val id = if (note.id == 0L) nextId++ else note.id
        notes[id] = note.copy(id = id)
        return id
    }

    override suspend fun updateNote(note: NoteEntity) {
        updatedCount++
        notes[note.id] = note
    }

    override suspend fun moveToTrash(id: Long, deletedDate: Long) = Unit
    override suspend fun moveVaultNoteToTrash(id: Long, deletedDate: Long): Int = 0
    override suspend fun restoreVaultNoteFromTrash(id: Long): Int = 0
    override suspend fun restoreFromTrash(id: Long) = Unit
    override suspend fun deleteNotePermanently(id: Long): Int = 0
    override suspend fun deleteVaultNotePermanently(id: Long): Int = 0
    override suspend fun emptyTrash(): Int = 0
    override suspend fun getDeletedImagePathsRaw(): List<String> = emptyList()
    override suspend fun setPinned(id: Long, isPinned: Boolean) = Unit

    override suspend fun setPreviewMode(id: Long, isPreviewMode: Boolean) {
        notes[id]?.let { notes[id] = it.copy(isPreviewMode = isPreviewMode) }
    }

}

class FakeEditorTemplateDao : TemplateDao {

    private val templates = mutableMapOf<Long, TemplateEntity>()
    private var nextId = 100L
    var insertedCount = 0
    var updatedCount = 0
    var insertFailure: Throwable? = null

    fun addTemplate(entity: TemplateEntity) {
        templates[entity.id] = entity
    }

    override fun getAllTemplates(): Flow<List<TemplateEntity>> =
        MutableStateFlow(templates.values.toList())

    override suspend fun getTemplateById(id: Long): TemplateEntity? = templates[id]

    override suspend fun countPredefinedTemplates(): Int =
        templates.values.count { it.isPredefined }

    override suspend fun insertTemplate(template: TemplateEntity): Long {
        insertFailure?.let { throw it }
        insertedCount++
        val id = if (template.id == 0L) nextId++ else template.id
        templates[id] = template.copy(id = id)
        return id
    }

    override suspend fun updateTemplate(template: TemplateEntity) {
        updatedCount++
        templates[template.id] = template
    }

    override suspend fun deleteTemplate(template: TemplateEntity) {
        templates.remove(template.id)
    }
}
