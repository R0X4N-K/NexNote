package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.NOTE_COLOR_PALETTE
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import io.github.r0x4nk.nexnote.ui.theme.NoteContentTheme
import io.github.r0x4nk.nexnote.ui.theme.rememberNoteColors
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorScreenScaffoldVaultLockTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun coloredDirectPreviewFinishesWarmupAfterThemeChanges() {
        val accent = mutableStateOf(AccentColor.VIOLET)
        composeRule.setContent {
            NexNoteTheme(darkTheme = true, accentColor = accent.value) {
                val state = rememberEditorScreenState(EditorMode.ExistingNote(77L))
                val uiState = EditorUiState(
                    noteId = 77L, title = "Colored preview", content = "Visible preview body",
                    backgroundColor = NOTE_COLOR_PALETTE[3], showPreview = true,
                    openedDirectlyInPreview = true
                )
                val colors = rememberNoteColors(uiState.backgroundColor)
                NoteContentTheme(colors) { EditorDirectPreviewWarmupEffect(uiState, state) }
                EditorScreenScaffold(
                    content = EditorScreenScaffoldContent(
                        uiState = uiState, undoRedoState = EditorUndoRedoState(), noteId = 77L,
                        tagsForCurrentNote = emptyList(), selectedTagsInEditor = null,
                        noteBackground = colors.container, isKeyboardVisible = false,
                        imageFileProvider = { File("unused") }, vaultImageByteProvider = { null },
                        noteLinkTargets = emptyList(), state = state
                    ),
                    actions = noOpEditorActions()
                )
            }
        }
        for (choice in listOf(AccentColor.VIOLET, AccentColor.GREEN, AccentColor.SAGE)) {
            composeRule.runOnIdle { accent.value = choice }
            composeRule.waitUntil(5_000) {
                composeRule.onAllNodesWithText("Visible preview body").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Visible preview body").assertIsDisplayed()
        }
    }

    @Test
    fun directPreviewDoesNotShowLoadingSkeletonAfterTaskToggle() {
        val content = mutableStateOf("- [ ] Task")
        val contentVersion = mutableStateOf(1)

        composeRule.setContent {
            NexNoteTheme(darkTheme = false) {
                val state = rememberEditorScreenState(EditorMode.ExistingNote(88L))
                val uiState = EditorUiState(
                    noteId = 88L,
                    title = "Checklist",
                    content = content.value,
                    contentVersion = contentVersion.value,
                    showPreview = true,
                    openedDirectlyInPreview = true
                )
                val colors = rememberNoteColors(uiState.backgroundColor)
                NoteContentTheme(colors) { EditorDirectPreviewWarmupEffect(uiState, state) }
                EditorScreenScaffold(
                    content = EditorScreenScaffoldContent(
                        uiState = uiState, undoRedoState = EditorUndoRedoState(), noteId = 88L,
                        tagsForCurrentNote = emptyList(), selectedTagsInEditor = null,
                        noteBackground = colors.container, isKeyboardVisible = false,
                        imageFileProvider = { File("unused") }, vaultImageByteProvider = { null },
                        noteLinkTargets = emptyList(), state = state
                    ),
                    actions = noOpEditorActions()
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Task").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.runOnIdle {
            content.value = "- [x] Task"
            contentVersion.value = 2
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Task").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Loading preview").assertCountEquals(0)
    }

    @Test
    fun lockedVaultEditorShowsPlaceholderWithoutProtectedContentOrImages() {
        val fileProviderCalls = AtomicInteger(0)
        val vaultImageProviderCalls = AtomicInteger(0)

        composeRule.setContent {
            NexNoteTheme {
                val state = rememberEditorScreenState(EditorMode.VaultNote(77L))
                EditorScreenScaffold(
                    content = EditorScreenScaffoldContent(
                        uiState = EditorUiState(
                            noteId = 77L,
                            title = "Private vault title",
                            content = "Private vault body\n\n![Vault secret](images/private.png)",
                            showPreview = true,
                            imagePaths = listOf("images/private.png"),
                            isVaultNote = true,
                            isVaultLocked = true,
                            isReadOnly = true
                        ),
                        undoRedoState = EditorUndoRedoState(),
                        noteId = 77L,
                        tagsForCurrentNote = emptyList(),
                        selectedTagsInEditor = null,
                        noteBackground = Color.White,
                        isKeyboardVisible = false,
                        imageFileProvider = {
                            fileProviderCalls.incrementAndGet()
                            File("unused")
                        },
                        vaultImageByteProvider = {
                            vaultImageProviderCalls.incrementAndGet()
                            byteArrayOf(1, 2, 3)
                        },
                        noteLinkTargets = emptyList(),
                        state = state
                    ),
                    actions = noOpEditorActions()
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Vault locked").assertCountEquals(2)
        composeRule.onNodeWithText("Unlock the Vault to view this note.").assertIsDisplayed()
        composeRule.onNodeWithText("Unlock Vault").assertIsDisplayed()
        composeRule.onNodeWithText("Private vault title").assertDoesNotExist()
        composeRule.onNodeWithText("Private vault body").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Vault secret").assertDoesNotExist()
        assertEquals(0, fileProviderCalls.get())
        assertEquals(0, vaultImageProviderCalls.get())
    }
}

private fun noOpEditorActions(): EditorScreenActions =
    EditorScreenActions(
        onBack = {},
        onExport = null,
        onTogglePreview = {},
        onInsertAttachment = {},
        onInsertNoteLink = {},
        insertAtCursor = {},
        applyMarkdownEdit = {},
        onNoteLinkAutocompleteSelected = { _, _ -> },
        onPreviewNoteLinkClick = {},
        onToggleColorPicker = {},
        onBackgroundColorChange = {},
        onTitleChange = {},
        onTagClick = {},
        onClearTagSelection = {},
        onContentEdited = {},
        onContentSelectionChange = {},
        onUndo = {},
        onRedo = {},
        onCreationDateTap = {},
        onSearchOpen = {},
        onSearchClose = {},
        onSearchQueryChange = {},
        onSearchPrevious = {},
        onSearchNext = {},
        onUnlockVault = {}
    )
