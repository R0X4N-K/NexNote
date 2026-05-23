package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import kotlinx.coroutines.flow.Flow
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
        val trashEvents = newTrashEventFlow()

        composeRule.setVaultTrashSnackbarContent(trashEvents)

        composeRule.waitForIdle()
        trashEvents.tryEmit(VaultTrashSnackbarEvent.MovedToTrash(42L))

        composeRule.awaitText(VAULT_TRASH_SNACKBAR_MESSAGE)

        composeRule.onNodeWithText(VAULT_TRASH_SNACKBAR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL).assertIsDisplayed()
    }

    @Test
    fun restoreEvent_showsNonSensitiveMessageAndUndoLabel() {
        val trashEvents = newTrashEventFlow()

        composeRule.setVaultTrashSnackbarContent(trashEvents)

        composeRule.waitForIdle()
        trashEvents.tryEmit(VaultTrashSnackbarEvent.RestoredFromTrash(24L))

        composeRule.awaitText(VAULT_RESTORE_SNACKBAR_MESSAGE)

        composeRule.onNodeWithText(VAULT_RESTORE_SNACKBAR_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL).assertIsDisplayed()
    }

    @Test
    fun undoButton_invokesCallbackWithEmittedNoteId() {
        val trashEvents = newTrashEventFlow()
        val undoCalls = mutableListOf<VaultTrashSnackbarEvent>()

        composeRule.setVaultTrashSnackbarContent(trashEvents) { undoCalls += it }

        composeRule.waitForIdle()
        val event = VaultTrashSnackbarEvent.MovedToTrash(7L)
        trashEvents.tryEmit(event)

        composeRule.awaitText(VAULT_TRASH_SNACKBAR_UNDO_LABEL)

        composeRule.onNodeWithText(VAULT_TRASH_SNACKBAR_UNDO_LABEL).performClick()

        composeRule.waitUntil(timeoutMillis = SNACKBAR_TIMEOUT_MILLIS) { undoCalls.isNotEmpty() }
        assertEquals(listOf(event), undoCalls)
    }
}

private const val SNACKBAR_EFFECT_HOST_TAG = "vault_snackbar_effect_host"
private const val SNACKBAR_TIMEOUT_MILLIS = 5_000L

/** Creates a hot flow able to buffer a single event emitted before collection starts. */
private fun newTrashEventFlow(): MutableSharedFlow<VaultTrashSnackbarEvent> =
    MutableSharedFlow(extraBufferCapacity = 1)

/**
 * Hosts [VaultTrashSnackbarEffect] inside a Material3 [Scaffold], applying the Scaffold's
 * content padding to the host so the layout matches production usage. Centralising the setup
 * keeps the individual test bodies focused on behaviour rather than boilerplate.
 */
private fun ComposeContentTestRule.setVaultTrashSnackbarContent(
    trashEvents: Flow<VaultTrashSnackbarEvent>,
    onUndoTrashEvent: (VaultTrashSnackbarEvent) -> Unit = {}
) {
    setContent {
        NexNoteTheme {
            val snackbarHostState = remember { SnackbarHostState() }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag(SNACKBAR_EFFECT_HOST_TAG)
                )
                VaultTrashSnackbarEffect(
                    trashEvents = trashEvents,
                    snackbarHostState = snackbarHostState,
                    onUndoTrashEvent = onUndoTrashEvent
                )
            }
        }
    }
}

/** Waits until at least one node displaying [text] is present, or fails after the timeout. */
private fun ComposeContentTestRule.awaitText(text: String) {
    waitUntil(timeoutMillis = SNACKBAR_TIMEOUT_MILLIS) {
        onAllNodesWithText(text)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }
}
