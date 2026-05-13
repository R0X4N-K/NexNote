package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.db.TemplateDao
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import io.github.r0x4nk.nexnote.data.db.model.NoteLinkCandidateProjection
import io.github.r0x4nk.nexnote.data.repository.NoteRepositoryImpl
import io.github.r0x4nk.nexnote.data.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.usecase.CopyNoteImageToInternalUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteNoteImageUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetNoteImageFileUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetTemplateByIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteLinkCandidatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SaveTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNotePreviewModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
abstract class EditorViewModelTestBase {

    private val testDispatcher = StandardTestDispatcher()
    protected lateinit var fakeNoteDao: FakeEditorNoteDao
    protected lateinit var fakeTemplateDao: FakeEditorTemplateDao

    @Before
    fun setupEditorViewModelTestBase() {
        Dispatchers.setMain(testDispatcher)
        fakeNoteDao = FakeEditorNoteDao()
        fakeTemplateDao = FakeEditorTemplateDao()
    }

    @After
    fun tearDownEditorViewModelTestBase() {
        Dispatchers.resetMain()
    }

    protected fun viewModel(
        noteId: Long = 0L,
        templateId: Long = 0L,
        editTemplateId: Long = 0L,
        mode: EditorMode = EditorMode.fromRoute(noteId, templateId, editTemplateId),
        imageStorage: NoteImageStorage = FakeEditorNoteImageStorage(),
        preferencesRepository: IUserPreferencesRepository = FakeEditorPreferencesRepository()
    ): EditorViewModel {
        val noteRepository = NoteRepositoryImpl(fakeNoteDao, imageStorage)
        val templateRepository = TemplateRepository(fakeTemplateDao)
        return EditorViewModel(
            copyNoteImageToInternal = CopyNoteImageToInternalUseCase(imageStorage),
            deleteNoteImage = DeleteNoteImageUseCase(imageStorage),
            getNoteImageFile = GetNoteImageFileUseCase(imageStorage),
            getNoteById = GetNoteByIdUseCase(noteRepository),
            getTemplateById = GetTemplateByIdUseCase(templateRepository),
            saveNote = SaveNoteUseCase(noteRepository),
            saveTemplate = SaveTemplateUseCase(templateRepository),
            setNotePreviewMode = SetNotePreviewModeUseCase(noteRepository),
            observeNoteLinkCandidates = ObserveNoteLinkCandidatesUseCase(noteRepository),
            observeThemeMode = ObserveThemeModeUseCase(preferencesRepository),
            setThemeMode = SetThemeModeUseCase(preferencesRepository),
            initialMode = mode
        )
    }
}

class FakeEditorNoteImageStorage : NoteImageStorage {
    val copiedNoteIds = mutableListOf<Long>()
    var failOnCopy = false
    private var nextTimestamp = 100L

    override suspend fun copyImageToInternal(
        noteId: Long,
        openInputStream: () -> InputStream?
    ): String {
        if (failOnCopy) throw RuntimeException("Copy failed")
        openInputStream()?.use { it.readBytes() }
        copiedNoteIds += noteId
        return "images/note_${noteId}_img_${nextTimestamp++}.jpg"
    }

    override suspend fun deleteImage(relativePath: String): Boolean = true

    override fun getImageFile(relativePath: String): File = File(relativePath)
}

class FakeEditorPreferencesRepository : IUserPreferencesRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _fontScale = MutableStateFlow(FontScale.NORMAL)
    private val _timezoneId = MutableStateFlow("")
    private val _isLeftHanded = MutableStateFlow(false)
    private val _accentColor = MutableStateFlow(AccentColor.VIOLET)
    private val _noteCardStyle = MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)

    var lastThemeMode: ThemeMode? = null

    override val themeMode: Flow<ThemeMode> = _themeMode
    override val fontScale: Flow<FontScale> = _fontScale
    override val timezoneId: Flow<String> = _timezoneId
    override val isLeftHanded: Flow<Boolean> = _isLeftHanded
    override val accentColor: Flow<AccentColor> = _accentColor
    override val noteCardStyle: Flow<NoteCardStyle> = _noteCardStyle

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

    override suspend fun setLeftHanded(value: Boolean) {
        _isLeftHanded.value = value
    }

    override suspend fun setAccentColor(color: AccentColor) {
        _accentColor.value = color
    }

    override suspend fun setNoteCardStyle(style: NoteCardStyle) {
        _noteCardStyle.value = style
    }
}

class FakeEditorNoteDao : NoteDao {

    private val notes = mutableMapOf<Long, NoteEntity>()
    private var nextId = 1L
    var insertedCount = 0
    var updatedCount = 0
    var failOnInsert = false

    fun addNote(entity: NoteEntity) {
        notes[entity.id] = entity
    }

    override fun getAllNotes(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { !it.isDeleted }.toList())

    override fun getAllNotesSortedAsc(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { !it.isDeleted }.toList())

    override fun getDeletedNotes(): Flow<List<NoteEntity>> =
        MutableStateFlow(notes.values.filter { it.isDeleted }.toList())

    override fun getNoteLinkCandidates(): Flow<List<NoteLinkCandidateProjection>> =
        MutableStateFlow(
            notes.values
                .filter { !it.isDeleted }
                .map { NoteLinkCandidateProjection(id = it.id, title = it.title) }
        )

    override suspend fun getNoteById(id: Long): NoteEntity? = notes[id]

    override fun searchNotes(query: String): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override fun getNotesByDateRange(startMs: Long, endMs: Long): Flow<List<NoteEntity>> =
        MutableStateFlow(emptyList())

    override fun getAllCreationDates(): Flow<List<Long>> =
        MutableStateFlow(emptyList())

    override suspend fun insertNote(note: NoteEntity): Long {
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
    override suspend fun restoreFromTrash(id: Long) = Unit
    override suspend fun deleteNotePermanently(id: Long): Int = 0
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

    fun addTemplate(entity: TemplateEntity) {
        templates[entity.id] = entity
    }

    override fun getAllTemplates(): Flow<List<TemplateEntity>> =
        MutableStateFlow(templates.values.toList())

    override suspend fun getTemplateById(id: Long): TemplateEntity? = templates[id]

    override suspend fun countPredefinedTemplates(): Int =
        templates.values.count { it.isPredefined }

    override suspend fun insertTemplate(template: TemplateEntity): Long {
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
