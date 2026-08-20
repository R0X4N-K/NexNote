package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.NoteLinkCandidate
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.TemplateRepository
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultNoteRepository
import io.github.r0x4nk.nexnote.domain.repository.VaultRepository
import io.github.r0x4nk.nexnote.domain.usecase.ConfigureVaultPinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DeleteVaultNotePermanentlyUseCase
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateVaultNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.GetVaultAndroidCredentialAvailabilityUseCase
import io.github.r0x4nk.nexnote.domain.usecase.LockVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveVaultNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTemplatesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialProtectedMaterialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultStateUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultTrashedNotesUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RemoveNoteFromVaultUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreVaultNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleVaultNotePinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithAndroidCredentialUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UnlockVaultWithPinUseCase
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlay
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultScreenTemplatePickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unlockedActiveVaultList_opensDismissesAndSelectsTemplatePicker() {
        val createdVaultNotes = mutableListOf<Unit>()
        val selectedTemplateIds = mutableListOf<Long>()
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = listOf(vaultTemplateFixture())
        )

        composeRule.setVaultScreen(
            harness = harness,
            onCreateVaultNote = { createdVaultNotes += Unit },
            onCreateVaultNoteFromTemplate = { selectedTemplateIds += it }
        )
        composeRule.waitUntilUnlocked(harness)

        composeRule.openVaultCreationMenu()
        composeRule.onNodeWithContentDescription("New Vault note")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            createdVaultNotes.size == 1
        }

        composeRule.openVaultCreationMenu()
        composeRule.onNodeWithContentDescription("New Vault note from template").performClick()
        composeRule.onNodeWithText("Choose a template").assertIsDisplayed()
        composeRule.onNodeWithText("Daily plan").assertIsDisplayed()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitUntilTextGone("Choose a template")

        composeRule.openVaultCreationMenu()
        composeRule.onNodeWithContentDescription("New Vault note from template").performClick()
        composeRule.onNodeWithText("Daily plan").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            selectedTemplateIds == listOf(7L)
        }
        composeRule.waitUntilTextGone("Choose a template")
    }

    @Test
    fun lockedVault_doesNotExposeTemplatePickerEntryPointOrTemplates() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.LOCKED,
            templates = listOf(vaultTemplateFixture())
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.accessViewModel.uiState.value.vaultState == VaultState.LOCKED
        }

        composeRule.onNodeWithText("Unlock Vault").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Vault creation menu")
            .assertDoesNotExist()
        composeRule.onNodeWithContentDescription("New Vault note from template")
            .assertDoesNotExist()
        composeRule.onNodeWithText("Daily plan").assertDoesNotExist()
    }

    @Test
    fun lockedVault_wrongPinShowsFailedAttemptCount() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.LOCKED,
            templates = emptyList(),
            unlockPin = "1234"
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.accessViewModel.uiState.value.vaultState == VaultState.LOCKED
        }

        composeRule.onNodeWithText("PIN").performTextInput("0000")
        composeRule.onNodeWithText("Unlock").performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.accessViewModel.uiState.value.failedPinAttempts == 1
        }
        composeRule.onNodeWithText("Wrong PIN.").assertIsDisplayed()
        composeRule.onNodeWithText("Failed PIN attempts: 1").assertIsDisplayed()
    }

    @Test
    fun vaultTrash_doesNotExposeTemplatePickerEntryPointOrTemplates() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = listOf(vaultTemplateFixture())
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntilUnlocked(harness)

        composeRule.onNodeWithContentDescription("Show Vault trash")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.notesViewModel.uiState.value.isTrashVisible
        }

        composeRule.onNodeWithText("Vault Trash").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Vault creation menu")
            .assertDoesNotExist()
        composeRule.onNodeWithContentDescription("New Vault note from template")
            .assertDoesNotExist()
        composeRule.onNodeWithText("Daily plan").assertDoesNotExist()
    }

    @Test
    fun topBarBackClosesSearchAndVaultTrashBeforeLeavingVault() {
        var backCount = 0
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = emptyList()
        )

        composeRule.setVaultScreen(
            harness = harness,
            onBack = { backCount++ }
        )
        composeRule.waitUntilUnlocked(harness)

        composeRule.onNodeWithContentDescription("Search Vault")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.notesViewModel.uiState.value.isSearchActive
        }

        composeRule.onNodeWithContentDescription("Go back")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            !harness.notesViewModel.uiState.value.isSearchActive
        }
        assertEquals(0, backCount)

        composeRule.onNodeWithContentDescription("Show Vault trash")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.notesViewModel.uiState.value.isTrashVisible
        }

        composeRule.onNodeWithContentDescription("Go back")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            !harness.notesViewModel.uiState.value.isTrashVisible
        }
        assertEquals(0, backCount)

        composeRule.onNodeWithContentDescription("Go back")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(1, backCount)
    }

    @Test
    fun lockingVaultWhileActionsSheetIsOpenDismissesSheetAndContent() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = emptyList(),
            activeNotes = listOf(
                Note(
                    id = 11L,
                    title = "Private sheet title",
                    content = "Private sheet body",
                    isInVault = true
                )
            )
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntilUnlocked(harness)
        composeRule.waitUntilTextVisible("Private sheet title")

        composeRule.onNodeWithText("Private sheet title")
            .performTouchInput { longClick() }
        composeRule.onNodeWithText("Vault note actions").assertIsDisplayed()

        composeRule.lockVaultFromOverflowMenu()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.accessViewModel.uiState.value.vaultState == VaultState.LOCKED &&
                !harness.notesViewModel.uiState.value.isUnlocked
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Unlock Vault").assertIsDisplayed()
        composeRule.onNodeWithText("Vault note actions").assertDoesNotExist()
        composeRule.onNodeWithText("Copy").assertDoesNotExist()
        composeRule.onNodeWithText("Private sheet title").assertDoesNotExist()
        composeRule.onNodeWithText("Private sheet body").assertDoesNotExist()
    }

    @Test
    fun lockingVaultWhileTemplatePickerIsOpenDismissesPickerAndTemplates() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = listOf(vaultTemplateFixture())
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntilUnlocked(harness)

        composeRule.openVaultCreationMenu()
        composeRule.onNodeWithContentDescription("New Vault note from template").performClick()
        composeRule.onNodeWithText("Choose a template").assertIsDisplayed()
        composeRule.onNodeWithText("Daily plan").assertIsDisplayed()

        composeRule.runOnIdle {
            harness.accessViewModel.lock()
        }
        composeRule.waitUntilLocked(harness)

        composeRule.onNodeWithText("Unlock Vault").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a template").assertDoesNotExist()
        composeRule.onNodeWithText("Daily plan").assertDoesNotExist()
    }

    @Test
    fun lockingVaultWhilePermanentDeleteDialogIsOpenDismissesDialogAndContent() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = emptyList(),
            trashedNotes = listOf(
                Note(
                    id = 12L,
                    title = "Deleted dialog title",
                    content = "Deleted dialog body",
                    isInVault = true,
                    isDeleted = true
                )
            )
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntilUnlocked(harness)

        composeRule.onNodeWithContentDescription("Show Vault trash")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            harness.notesViewModel.uiState.value.isTrashVisible
        }
        composeRule.waitUntilTextVisible("Deleted dialog title")

        composeRule.onNodeWithContentDescription("Delete permanently")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Delete Vault note?").assertIsDisplayed()
        composeRule
            .onNodeWithText("Permanently delete this Vault note? This cannot be undone.")
            .assertIsDisplayed()

        composeRule.runOnIdle {
            harness.accessViewModel.lock()
        }
        composeRule.waitUntilLocked(harness)

        composeRule.onNodeWithText("Unlock Vault").assertIsDisplayed()
        composeRule.onNodeWithText("Delete Vault note?").assertDoesNotExist()
        composeRule
            .onNodeWithText("Permanently delete this Vault note? This cannot be undone.")
            .assertDoesNotExist()
        composeRule.onNodeWithText("Deleted dialog title").assertDoesNotExist()
        composeRule.onNodeWithText("Deleted dialog body").assertDoesNotExist()
    }

    @Test
    fun pendingTrashUndoAfterVaultLocksDoesNotRestoreOrExposeContent() {
        val harness = VaultScreenHarness(
            initialVaultState = VaultState.UNLOCKED,
            templates = emptyList(),
            activeNotes = listOf(
                Note(
                    id = 13L,
                    title = "Snackbar private title",
                    content = "Snackbar private body",
                    isInVault = true
                )
            )
        )

        composeRule.setVaultScreen(harness = harness)
        composeRule.waitUntilUnlocked(harness)
        composeRule.waitUntilTextVisible("Snackbar private title")

        composeRule.onNodeWithText("Snackbar private title")
            .performTouchInput { longClick() }
        composeRule.onNodeWithText("Move to trash")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntilTextVisible(VAULT_TRASH_SNACKBAR_MESSAGE)
        assertEquals(listOf(13L), harness.vaultNoteRepository.trashedIds)

        composeRule.lockVaultFromOverflowMenu()
        composeRule.waitUntilLocked(harness)

        composeRule.onNodeWithText("Unlock Vault").assertIsDisplayed()
        composeRule.onNodeWithText(VAULT_TRASH_SNACKBAR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL).performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList<Long>(), harness.vaultNoteRepository.restoredIds)
        composeRule.onNodeWithText("Snackbar private title").assertDoesNotExist()
        composeRule.onNodeWithText("Snackbar private body").assertDoesNotExist()
    }

    private fun ComposeContentTestRule.setVaultScreen(
        harness: VaultScreenHarness,
        onBack: () -> Unit = {},
        onCreateVaultNote: () -> Unit = {},
        onCreateVaultNoteFromTemplate: (Long) -> Unit = {}
    ) {
        setContent {
            NexNoteTheme {
                RadialMenuOverlay {
                    VaultScreen(
                        onBack = onBack,
                        onCreateVaultNote = onCreateVaultNote,
                        onCreateVaultNoteFromTemplate = onCreateVaultNoteFromTemplate,
                        onNoteClick = {},
                        accessViewModel = harness.accessViewModel,
                        notesViewModel = harness.notesViewModel
                    )
                }
            }
        }
    }

    private fun ComposeContentTestRule.openVaultCreationMenu() {
        onNodeWithContentDescription("Open Vault creation menu")
            .assertIsDisplayed()
            .performClick()
    }

    private fun ComposeContentTestRule.lockVaultFromOverflowMenu() {
        onNodeWithContentDescription("Vault options")
            .assertIsDisplayed()
            .performClick()
        onNodeWithText("Lock Vault")
            .assertIsDisplayed()
            .performClick()
    }

    private fun ComposeContentTestRule.waitUntilUnlocked(harness: VaultScreenHarness) {
        waitUntil(timeoutMillis = 3_000) {
            harness.accessViewModel.uiState.value.isUnlocked &&
                harness.notesViewModel.uiState.value.isUnlocked
        }
        waitForIdle()
    }

    private fun ComposeContentTestRule.waitUntilLocked(harness: VaultScreenHarness) {
        waitUntil(timeoutMillis = 3_000) {
            harness.accessViewModel.uiState.value.vaultState == VaultState.LOCKED &&
                !harness.notesViewModel.uiState.value.isUnlocked
        }
        waitForIdle()
    }

    private fun ComposeContentTestRule.waitUntilTextVisible(text: String) {
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        waitForIdle()
    }

    private fun ComposeContentTestRule.waitUntilTextGone(text: String) {
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
        waitForIdle()
    }

    private fun vaultTemplateFixture(): Template = Template(
        id = 7L,
        name = "Daily plan",
        category = "planning",
        content = "Template body",
        isMarkdown = true
    )
}

private class VaultScreenHarness(
    initialVaultState: VaultState,
    templates: List<Template>,
    activeNotes: List<Note> = emptyList(),
    trashedNotes: List<Note> = emptyList(),
    unlockPin: String = "1234"
) {
    private val vaultRepository = FakeVaultScreenVaultRepository(
        initialVaultState = initialVaultState,
        unlockPin = unlockPin
    )
    val vaultNoteRepository = FakeVaultScreenVaultNoteRepository(
        activeNotes = activeNotes,
        trashedNotes = trashedNotes
    )
    private val templateRepository = FakeVaultScreenTemplateRepository(templates)
    private val preferencesRepository = FakeVaultScreenPreferencesRepository()
    private val androidCredentialRepository = FakeVaultScreenAndroidCredentialRepository()

    val accessViewModel = VaultAccessViewModel(
        observeVaultState = ObserveVaultStateUseCase(vaultRepository),
        configureVaultPin = ConfigureVaultPinUseCase(vaultRepository),
        unlockVaultWithPin = UnlockVaultWithPinUseCase(vaultRepository),
        unlockVaultWithAndroidCredential =
            UnlockVaultWithAndroidCredentialUseCase(vaultRepository),
        lockVault = LockVaultUseCase(vaultRepository),
        getVaultAndroidCredentialAvailability =
            GetVaultAndroidCredentialAvailabilityUseCase(androidCredentialRepository),
        observeVaultAndroidCredentialUnlock =
            ObserveVaultAndroidCredentialUnlockUseCase(preferencesRepository),
        observeVaultAndroidCredentialProtectedMaterial =
            ObserveVaultAndroidCredentialProtectedMaterialUseCase(vaultRepository)
    )

    val notesViewModel = VaultNotesViewModel(
        observeVaultState = ObserveVaultStateUseCase(vaultRepository),
        observeVaultNotes = ObserveVaultNotesUseCase(vaultNoteRepository),
        observeVaultTrashedNotes = ObserveVaultTrashedNotesUseCase(vaultNoteRepository),
        moveNoteToVault = MoveNoteToVaultUseCase(vaultNoteRepository),
        moveVaultNoteToTrash = MoveVaultNoteToTrashUseCase(vaultNoteRepository),
        restoreVaultNoteFromTrash = RestoreVaultNoteFromTrashUseCase(vaultNoteRepository),
        deleteVaultNotePermanently =
            DeleteVaultNotePermanentlyUseCase(vaultNoteRepository),
        toggleVaultNotePin = ToggleVaultNotePinUseCase(vaultNoteRepository),
        duplicateVaultNote = DuplicateVaultNoteUseCase(vaultNoteRepository),
        removeNoteFromVault = RemoveNoteFromVaultUseCase(vaultNoteRepository),
        observeTemplates = ObserveTemplatesUseCase(templateRepository),
        observeNoteCardStyle = ObserveNoteCardStyleUseCase(preferencesRepository)
    )
}

private class FakeVaultScreenVaultRepository(
    initialVaultState: VaultState,
    unlockPin: String
) : VaultRepository {
    private val stateFlow = MutableStateFlow(initialVaultState)
    private val expectedUnlockPin = unlockPin.toCharArray()
    override val state: Flow<VaultState> = stateFlow
    override val hasAndroidCredentialProtectedUnlockMaterial: Flow<Boolean> =
        MutableStateFlow(false)

    override suspend fun configurePin(pin: CharArray) {
        stateFlow.value = VaultState.LOCKED
    }

    override suspend fun unlockWithPin(pin: CharArray): Boolean {
        val unlocked = expectedUnlockPin.contentEquals(pin)
        stateFlow.value = if (unlocked) {
            VaultState.UNLOCKED
        } else {
            VaultState.LOCKED
        }
        return unlocked
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

private class FakeVaultScreenVaultNoteRepository(
    activeNotes: List<Note> = emptyList(),
    trashedNotes: List<Note> = emptyList()
) : VaultNoteRepository {
    private val activeNotesFlow = MutableStateFlow(activeNotes)
    private val trashedNotesFlow = MutableStateFlow(trashedNotes)
    override val vaultNotes: Flow<List<Note>> = activeNotesFlow
    override val vaultTrashedNotes: Flow<List<Note>> = trashedNotesFlow
    override val vaultNoteLinkCandidates: Flow<List<NoteLinkCandidate>> =
        MutableStateFlow(emptyList())
    val trashedIds = mutableListOf<Long>()
    val restoredIds = mutableListOf<Long>()

    override suspend fun getVaultNoteById(id: Long): Note? = null

    override suspend fun saveVaultNote(note: Note): Long = note.id

    override suspend fun duplicateVaultNote(id: Long): DuplicateVaultNoteResult =
        DuplicateVaultNoteResult.NotFound

    override suspend fun moveNormalNoteToVault(id: Long): MoveNoteToVaultResult =
        MoveNoteToVaultResult.NotFound

    override suspend fun removeNoteFromVault(id: Long): Boolean = false

    override suspend fun moveVaultNoteToTrash(id: Long): Boolean {
        trashedIds += id
        val note = activeNotesFlow.value.firstOrNull { it.id == id && it.isInVault }
            ?: return false
        activeNotesFlow.value = activeNotesFlow.value.filterNot { it.id == id }
        trashedNotesFlow.value = trashedNotesFlow.value + note.copy(isDeleted = true)
        return true
    }

    override suspend fun restoreVaultNoteFromTrash(id: Long): Boolean {
        restoredIds += id
        val note = trashedNotesFlow.value.firstOrNull { it.id == id && it.isInVault }
            ?: return false
        trashedNotesFlow.value = trashedNotesFlow.value.filterNot { it.id == id }
        activeNotesFlow.value = activeNotesFlow.value + note.copy(isDeleted = false)
        return true
    }

    override suspend fun deleteVaultNotePermanently(id: Long): Boolean = false

    override suspend fun decryptVaultImageBytes(relativePath: String): ByteArray? = null
}

private class FakeVaultScreenTemplateRepository(
    templates: List<Template>
) : TemplateRepository {
    private val templatesFlow = MutableStateFlow(templates)
    override val allTemplates: Flow<List<Template>> = templatesFlow

    override suspend fun getTemplateById(id: Long): Template? =
        templatesFlow.value.firstOrNull { it.id == id }

    override suspend fun saveTemplate(template: Template): Long {
        throw UnsupportedOperationException("Not needed for Vault screen tests")
    }

    override suspend fun deleteTemplate(template: Template) {
        throw UnsupportedOperationException("Not needed for Vault screen tests")
    }
}

private class FakeVaultScreenPreferencesRepository : IUserPreferencesRepository {
    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
    override val fontScale: Flow<FontScale> = MutableStateFlow(FontScale.NORMAL)
    override val timezoneId: Flow<String> = MutableStateFlow("UTC")
    override val accentColor: Flow<AccentColor> = MutableStateFlow(AccentColor.VIOLET)
    override val noteCardStyle: Flow<NoteCardStyle> =
        MutableStateFlow(NoteCardStyle.TITLE_AND_PREVIEW)
    override val tableLayoutMode: Flow<TableLayoutMode> =
        MutableStateFlow(TableLayoutMode.FIT_SCREEN)
    override val protectVaultRecentPreviews: Flow<Boolean> = MutableStateFlow(true)
    override val lockVaultOnBackground: Flow<Boolean> = MutableStateFlow(true)
    override val vaultAutoLockTimeout: Flow<VaultAutoLockTimeout> =
        MutableStateFlow(VaultAutoLockTimeout.IMMEDIATELY)
    override val unlockVaultWithAndroidCredential: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
    override suspend fun setFontScale(scale: FontScale) = Unit
    override suspend fun setTimezoneId(id: String) = Unit
    override suspend fun setAccentColor(color: AccentColor) = Unit
    override suspend fun setNoteCardStyle(style: NoteCardStyle) = Unit
    override suspend fun setTableLayoutMode(mode: TableLayoutMode) = Unit
    override suspend fun setProtectVaultRecentPreviews(value: Boolean) = Unit
    override suspend fun setLockVaultOnBackground(value: Boolean) = Unit
    override suspend fun setVaultAutoLockTimeout(timeout: VaultAutoLockTimeout) = Unit
    override suspend fun setUnlockVaultWithAndroidCredential(value: Boolean) = Unit
}

private class FakeVaultScreenAndroidCredentialRepository : VaultAndroidCredentialRepository {
    override fun getAvailability(): VaultAndroidCredentialAvailability =
        VaultAndroidCredentialAvailability.UNAVAILABLE
}
