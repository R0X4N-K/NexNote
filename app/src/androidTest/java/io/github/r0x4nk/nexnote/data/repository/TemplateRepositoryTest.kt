package io.github.r0x4nk.nexnote.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.domain.model.Template
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateRepositoryTest {

    private lateinit var db: NexNoteDatabase
    private lateinit var repository: TemplateRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TemplateRepository(db.templateDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Predefined Init ───────────────────────────────────────────────────────

    @Test
    fun initializePredefinedTemplates_insertsAllPredefined() = runTest {
        repository.initializePredefinedTemplates()
        val templates = repository.allTemplates.first()
        val predefined = templates.filter { it.isPredefined }
        assertEquals(6, predefined.size)
    }

    @Test
    fun initializePredefinedTemplates_isIdempotent() = runTest {
        repository.initializePredefinedTemplates()
        repository.initializePredefinedTemplates()
        val templates = repository.allTemplates.first()
        val predefined = templates.filter { it.isPredefined }
        assertEquals(6, predefined.size)
    }

    // ── CRUD Custom ───────────────────────────────────────────────────────────

    @Test
    fun saveTemplate_insertNewTemplate_returnsPositiveId() = runTest {
        val id = repository.saveTemplate(Template(name = "My template", content = "..."))
        assertTrue(id > 0)
    }

    @Test
    fun saveTemplate_updateExisting_reflectsChanges() = runTest {
        val id = repository.saveTemplate(Template(name = "Original", content = "v1"))
        val template = repository.getTemplateById(id)!!
        repository.saveTemplate(template.copy(name = "Updated"))
        val updated = repository.getTemplateById(id)
        assertEquals("Updated", updated?.name)
    }

    @Test
    fun deleteTemplate_removesCustomTemplate() = runTest {
        val id = repository.saveTemplate(Template(name = "To delete", content = ""))
        val template = repository.getTemplateById(id)!!
        repository.deleteTemplate(template)
        val found = repository.getTemplateById(id)
        assertNull(found)
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteTemplate_predefinedTemplate_throwsException() = runTest {
        repository.initializePredefinedTemplates()
        val predefined = repository.allTemplates.first().first { it.isPredefined }
        repository.deleteTemplate(predefined) // must throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveTemplate_predefinedTemplate_throwsException() = runTest {
        val predefined = Template(name = "Test", isPredefined = true)
        repository.saveTemplate(predefined) // must throw
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    @Test
    fun allTemplates_predefinedFirst() = runTest {
        repository.saveTemplate(Template(name = "Aaaa custom", content = ""))
        repository.initializePredefinedTemplates()
        val templates = repository.allTemplates.first()
        assertTrue(templates.first().isPredefined)
    }
}
