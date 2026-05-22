package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.component.NoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultNoteActionsSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun vaultNoteActionsSheet_matchesNormalActionOrderWithVaultSpecificActions() {
        composeRule.setVaultNoteActionsSheet()

        composeRule.onNodeWithText("Vault note actions").assertIsDisplayed()
        composeRule.onNodeWithText("Copy").assertIsDisplayed()
        composeRule.onNodeWithText("Duplicate").assertIsDisplayed()
        composeRule.onNodeWithText("Remove from Vault").assertIsDisplayed()
        composeRule.onNodeWithText("Move to trash").assertIsDisplayed()

        val copyTop = composeRule.onNodeWithTag(VAULT_NOTE_ACTION_COPY_TAG)
            .getUnclippedBoundsInRoot().top.value
        val duplicateTop = composeRule.onNodeWithTag(VAULT_NOTE_ACTION_DUPLICATE_TAG)
            .getUnclippedBoundsInRoot().top.value
        val removeTop = composeRule.onNodeWithTag(VAULT_NOTE_ACTION_REMOVE_FROM_VAULT_TAG)
            .getUnclippedBoundsInRoot().top.value
        val trashTop = composeRule.onNodeWithTag(VAULT_NOTE_ACTION_MOVE_TO_TRASH_TAG)
            .getUnclippedBoundsInRoot().top.value

        assertTrue(copyTop < duplicateTop)
        assertTrue(duplicateTop < removeTop)
        assertTrue(removeTop < trashTop)
    }

    @Test
    fun vaultNoteActionsSheet_doesNotExposeNoteContentInHeader() {
        composeRule.setVaultNoteActionsSheet(
            note = Note(
                id = 42L,
                title = "Private title",
                content = "Private body",
                isInVault = true
            )
        )

        composeRule.onNodeWithText("Selected Vault note").assertIsDisplayed()
        composeRule.onAllNodesWithText("Private title").assertCountEquals(0)
        composeRule.onAllNodesWithText("Private body").assertCountEquals(0)

        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_COPY_TAG).performClick()

        composeRule.onNodeWithText("Copy Vault note").assertIsDisplayed()
        composeRule.onAllNodesWithText("Private title").assertCountEquals(0)
        composeRule.onAllNodesWithText("Private body").assertCountEquals(0)
    }

    @Test
    fun vaultNoteActionsSheet_copyAsTextInvokesOnlyPlainTextCallback() {
        var copiedPlainTextNoteId: Long? = null
        var copiedMarkdownNoteId: Long? = null
        var trashedNoteId: Long? = null
        var duplicatedNoteId: Long? = null
        var removedNoteId: Long? = null
        var dismissCount = 0

        composeRule.setVaultNoteActionsSheet(
            clipboardCallbacks = NoteClipboardCallbacks(
                onCopyPlainText = { copiedPlainTextNoteId = it.id },
                onCopyMarkdown = { copiedMarkdownNoteId = it.id }
            ),
            onMoveToTrash = { trashedNoteId = it.id },
            onDuplicate = { duplicatedNoteId = it.id },
            onRemoveFromVault = { removedNoteId = it.id },
            onDismiss = { dismissCount++ }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_COPY_TAG).performClick()
        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_COPY_TEXT_TAG).performClick()

        assertEquals(42L, copiedPlainTextNoteId)
        assertNull(copiedMarkdownNoteId)
        assertNull(trashedNoteId)
        assertNull(duplicatedNoteId)
        assertNull(removedNoteId)
        assertEquals(1, dismissCount)
    }

    @Test
    fun vaultNoteActionsSheet_copyAsMarkdownInvokesOnlyMarkdownCallback() {
        var copiedPlainTextNoteId: Long? = null
        var copiedMarkdownNoteId: Long? = null
        var trashedNoteId: Long? = null
        var duplicatedNoteId: Long? = null
        var removedNoteId: Long? = null
        var dismissCount = 0

        composeRule.setVaultNoteActionsSheet(
            clipboardCallbacks = NoteClipboardCallbacks(
                onCopyPlainText = { copiedPlainTextNoteId = it.id },
                onCopyMarkdown = { copiedMarkdownNoteId = it.id }
            ),
            onMoveToTrash = { trashedNoteId = it.id },
            onDuplicate = { duplicatedNoteId = it.id },
            onRemoveFromVault = { removedNoteId = it.id },
            onDismiss = { dismissCount++ }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_COPY_TAG).performClick()
        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_COPY_MARKDOWN_TAG).performClick()

        assertNull(copiedPlainTextNoteId)
        assertEquals(42L, copiedMarkdownNoteId)
        assertNull(trashedNoteId)
        assertNull(duplicatedNoteId)
        assertNull(removedNoteId)
        assertEquals(1, dismissCount)
    }

    @Test
    fun vaultNoteActionsSheet_moveToTrashInvokesOnlyTrashCallback() {
        var trashedNoteId: Long? = null
        var duplicatedNoteId: Long? = null
        var removedNoteId: Long? = null
        var dismissCount = 0

        composeRule.setVaultNoteActionsSheet(
            onMoveToTrash = { trashedNoteId = it.id },
            onDuplicate = { duplicatedNoteId = it.id },
            onRemoveFromVault = { removedNoteId = it.id },
            onDismiss = { dismissCount++ }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_MOVE_TO_TRASH_TAG).performClick()

        assertEquals(42L, trashedNoteId)
        assertNull(duplicatedNoteId)
        assertNull(removedNoteId)
        assertEquals(1, dismissCount)
    }

    @Test
    fun vaultNoteActionsSheet_duplicateInvokesOnlyDuplicateCallback() {
        var trashedNoteId: Long? = null
        var duplicatedNoteId: Long? = null
        var removedNoteId: Long? = null
        var dismissCount = 0

        composeRule.setVaultNoteActionsSheet(
            onMoveToTrash = { trashedNoteId = it.id },
            onDuplicate = { duplicatedNoteId = it.id },
            onRemoveFromVault = { removedNoteId = it.id },
            onDismiss = { dismissCount++ }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_DUPLICATE_TAG).performClick()

        assertNull(trashedNoteId)
        assertEquals(42L, duplicatedNoteId)
        assertNull(removedNoteId)
        assertEquals(1, dismissCount)
    }

    @Test
    fun vaultNoteActionsSheet_removeFromVaultInvokesOnlyRemoveCallback() {
        var trashedNoteId: Long? = null
        var duplicatedNoteId: Long? = null
        var removedNoteId: Long? = null
        var dismissCount = 0

        composeRule.setVaultNoteActionsSheet(
            onMoveToTrash = { trashedNoteId = it.id },
            onDuplicate = { duplicatedNoteId = it.id },
            onRemoveFromVault = { removedNoteId = it.id },
            onDismiss = { dismissCount++ }
        )

        composeRule.onNodeWithTag(VAULT_NOTE_ACTION_REMOVE_FROM_VAULT_TAG).performClick()

        assertNull(trashedNoteId)
        assertNull(duplicatedNoteId)
        assertEquals(42L, removedNoteId)
        assertEquals(1, dismissCount)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule
        .setVaultNoteActionsSheet(
            note: Note? = Note(id = 42L, isInVault = true),
            clipboardCallbacks: NoteClipboardCallbacks = NoteClipboardCallbacks(
                onCopyPlainText = {},
                onCopyMarkdown = {}
            ),
            onMoveToTrash: (Note) -> Unit = {},
            onDuplicate: (Note) -> Unit = {},
            onRemoveFromVault: (Note) -> Unit = {},
            onDismiss: () -> Unit = {}
        ) {
            setContent {
                NexNoteTheme {
                    VaultNoteActionsSheet(
                        note = note,
                        clipboardCallbacks = clipboardCallbacks,
                        onMoveToTrash = onMoveToTrash,
                        onDuplicate = onDuplicate,
                        onRemoveFromVault = onRemoveFromVault,
                        onDismiss = onDismiss
                    )
                }
            }
        }
}
