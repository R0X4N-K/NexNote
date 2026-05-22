package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultTrashSnackbarEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun trashEvent_showsNonSensitiveMessageAndUndoLabel() {
        val trashEvents = MutableSharedFlow<VaultTrashSnackbarEvent>(extraBufferCapacity = 1)

        composeRule.setContent {
            NexNoteTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("vault_snackbar_effect_host"))
                    VaultTrashSnackbarEffect(
                        trashEvents = trashEvents,
                        snackbarHostState = snackbarHostState,
                        onUndoTrashEvent = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        trashEvents.tryEmit(VaultTrashSnackbarEvent.MovedToTrash(42L))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(VAULT_TRASH_SNACKBAR_MESSAGE)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }

        composeRule
            .onNodeWithText(VAULT_TRASH_SNACKBAR_MESSAGE)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL)
            .assertIsDisplayed()
    }

    @Test
    fun restoreEvent_showsNonSensitiveMessageAndUndoLabel() {
        val trashEvents = MutableSharedFlow<VaultTrashSnackbarEvent>(extraBufferCapacity = 1)

        composeRule.setContent {
            NexNoteTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("vault_snackbar_effect_host"))
                    VaultTrashSnackbarEffect(
                        trashEvents = trashEvents,
                        snackbarHostState = snackbarHostState,
                        onUndoTrashEvent = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        trashEvents.tryEmit(VaultTrashSnackbarEvent.RestoredFromTrash(24L))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(VAULT_RESTORE_SNACKBAR_MESSAGE)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }

        composeRule
            .onNodeWithText(VAULT_RESTORE_SNACKBAR_MESSAGE)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL)
            .assertIsDisplayed()
    }

    @Test
    fun undoButton_invokesCallbackWithEmittedNoteId() {
        val trashEvents = MutableSharedFlow<VaultTrashSnackbarEvent>(extraBufferCapacity = 1)
        val undoCalls = mutableListOf<VaultTrashSnackbarEvent>()

        composeRule.setContent {
            NexNoteTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize().testTag("vault_snackbar_effect_host"))
                    VaultTrashSnackbarEffect(
                        trashEvents = trashEvents,
                        snackbarHostState = snackbarHostState,
                        onUndoTrashEvent = { undoCalls += it }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val event = VaultTrashSnackbarEvent.MovedToTrash(7L)
        trashEvents.tryEmit(event)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }

        composeRule
            .onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { undoCalls.isNotEmpty() }
        assertEquals(listOf(event), undoCalls)
    }
}
