package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class SelectionTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singleSelectionOffersNoteActionsInsideTheOverflowMenu() {
        var noteActions = 0
        composeRule.setContent {
            NexNoteTheme {
                SelectionTopAppBar(
                    selectedCount = 1,
                    totalCount = 3,
                    onClose = {},
                    onSelectAll = {},
                    onDeselectAll = {},
                    onNoteActions = { noteActions++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Selection options").performClick()
        composeRule.onNodeWithText("Note actions").assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(1, noteActions) }
    }

    @Test
    fun multiSelectionKeepsTheOverflowMenuFreeOfSingleNoteActions() {
        composeRule.setContent {
            NexNoteTheme {
                SelectionTopAppBar(
                    selectedCount = 2,
                    totalCount = 3,
                    onClose = {},
                    onSelectAll = {},
                    onDeselectAll = {},
                    onNoteActions = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Selection options").performClick()
        composeRule.onNodeWithText("Note actions").assertDoesNotExist()
        composeRule.onNodeWithText("Select all").assertIsDisplayed()
        composeRule.onNodeWithText("Deselect all").assertIsDisplayed()
    }
}
