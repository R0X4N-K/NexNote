package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class EditorScreenScaffoldVaultLockTest {

    @get:Rule
    val composeRule = createComposeRule()

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
        onInsertImage = {},
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
