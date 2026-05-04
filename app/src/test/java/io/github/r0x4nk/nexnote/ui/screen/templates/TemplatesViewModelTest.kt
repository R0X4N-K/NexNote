package io.github.r0x4nk.nexnote.ui.screen.templates

import io.github.r0x4nk.nexnote.data.db.TemplateDao
import io.github.r0x4nk.nexnote.data.db.entity.TemplateEntity
import io.github.r0x4nk.nexnote.data.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.usecase.DeleteTemplateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplatesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeTemplateDao
    private lateinit var viewModel: TemplatesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeTemplateDao()
        val repository = TemplateRepository(fakeDao)
        viewModel = TemplatesViewModel(
            observeTemplates = ObserveTemplatesUseCase(repository),
            deleteTemplate = DeleteTemplateUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Subscribes to uiState to activate WhileSubscribed sharing, then runs [block]. */
    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        block()
    }

    // ── Stato iniziale ────────────────────────────────────────────────────────

    @Test
    fun `initial dialog state is None`() = runViewModelTest {
        advanceUntilIdle()
        assertEquals(TemplatesDialog.None, viewModel.uiState.value.activeDialog)
    }

    // ── requestDelete ─────────────────────────────────────────────────────────

    @Test
    fun `requestDelete sets ConfirmDelete dialog`() = runViewModelTest {
        val t = template(id = 3L, name = "To delete")
        viewModel.requestDelete(t)
        advanceUntilIdle()
        val dialog = viewModel.uiState.value.activeDialog
        assertTrue(dialog is TemplatesDialog.ConfirmDelete)
        assertEquals(t, (dialog as TemplatesDialog.ConfirmDelete).template)
    }

    @Test
    fun `requestDelete is no-op for predefined templates`() = runViewModelTest {
        viewModel.requestDelete(template(id = 2L, isPredefined = true))
        advanceUntilIdle()
        assertEquals(TemplatesDialog.None, viewModel.uiState.value.activeDialog)
    }

    // ── closeDialog ───────────────────────────────────────────────────────────

    @Test
    fun `closeDialog resets activeDialog to None`() = runViewModelTest {
        viewModel.requestDelete(template(id = 3L))
        viewModel.closeDialog()
        advanceUntilIdle()
        assertEquals(TemplatesDialog.None, viewModel.uiState.value.activeDialog)
    }

    // ── confirmDelete ─────────────────────────────────────────────────────────

    @Test
    fun `confirmDelete deletes template and closes dialog`() = runViewModelTest {
        viewModel.requestDelete(template(id = 9L))
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertEquals(TemplatesDialog.None, viewModel.uiState.value.activeDialog)
        assertEquals(1, fakeDao.deletedCount)
    }

    // ── errorMessage ──────────────────────────────────────────────────────────

    @Test
    fun `clearError resets errorMessage to null`() = runViewModelTest {
        fakeDao.failOnDelete = true
        viewModel.requestDelete(template(id = 3L))
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMessage?.isNotBlank() == true)
        viewModel.clearError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun template(
        id: Long = 1L,
        name: String = "Template",
        content: String = "",
        isMarkdown: Boolean = true,
        isPredefined: Boolean = false
    ) = Template(id = id, name = name, content = content, isMarkdown = isMarkdown, isPredefined = isPredefined)
}

// ── Fake ─────────────────────────────────────────────────────────────────────

private class FakeTemplateDao : TemplateDao {

    private val _templates = MutableStateFlow<List<TemplateEntity>>(emptyList())
    var insertedCount = 0
    var updatedCount = 0
    var deletedCount = 0
    var failOnInsert = false
    var failOnDelete = false
    private var nextId = 100L

    override fun getAllTemplates(): Flow<List<TemplateEntity>> = _templates

    override suspend fun getTemplateById(id: Long): TemplateEntity? =
        _templates.value.find { it.id == id }

    override suspend fun countPredefinedTemplates(): Int =
        _templates.value.count { it.isPredefined }

    override suspend fun insertTemplate(template: TemplateEntity): Long {
        if (failOnInsert) throw RuntimeException("Insert fallito")
        insertedCount++
        val assignedId = if (template.id == 0L) nextId++ else template.id
        _templates.value = _templates.value + template.copy(id = assignedId)
        return assignedId
    }

    override suspend fun updateTemplate(template: TemplateEntity) {
        updatedCount++
        _templates.value = _templates.value.map { if (it.id == template.id) template else it }
    }

    override suspend fun deleteTemplate(template: TemplateEntity) {
        if (failOnDelete) throw RuntimeException("Delete fallito")
        deletedCount++
        _templates.value = _templates.value.filter { it.id != template.id }
    }
}
