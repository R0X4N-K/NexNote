package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun overflowMenuOpensVaultAndTrashActions() {
        var openedVault = false
        var openedTrash = false

        composeRule.setContent {
            NexNoteTheme {
                HomeTopAppBar(
                    uiState = HomeUiState(isLoading = false),
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                    searchFocusRequester = remember { FocusRequester() },
                    onSearchQueryChange = {},
                    onSearchToggle = {},
                    onSortToggle = {},
                    onViewModeToggle = {},
                    onOpenTrash = { openedTrash = true },
                    onOpenVault = { openedVault = true },
                    onStartSelection = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Access Vault")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Trash")
            .assertIsDisplayed()
            .performClick()

        assertTrue(openedVault)
        assertTrue(openedTrash)
    }
}
