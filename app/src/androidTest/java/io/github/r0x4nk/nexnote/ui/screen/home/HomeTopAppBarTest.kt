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
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun browsingActionsExposeBrandSortAndOverflowDestinations() {
        var openedVault = false
        var openedTrash = false
        var openedStatistics = false
        var toggledSort = false

        composeRule.setContent {
            NexNoteTheme {
                HomeTopAppBar(
                    uiState = HomeUiState(totalNoteCount = 500, isLoading = false),
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                    searchFocusRequester = remember { FocusRequester() },
                    onSearchQueryChange = {},
                    onSearchToggle = {},
                    onOpenSearchFilters = {},
                    onSearchSortChange = {},
                    onSortToggle = { toggledSort = true },
                    onViewModeToggle = {},
                    onOpenTrash = { openedTrash = true },
                    onOpenStatistics = { openedStatistics = true },
                    onOpenVault = { openedVault = true },
                    onStartSelection = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("NexNote app icon").assertIsDisplayed()
        composeRule.onNodeWithText("500 notes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort oldest first")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("More options")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Oldest first").assertDoesNotExist()
        composeRule.onNodeWithText("Grid view").assertIsDisplayed()
        composeRule.onNodeWithText("Statistics")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Access Vault")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Trash")
            .assertIsDisplayed()
            .performClick()

        assertTrue(openedVault)
        assertTrue(openedTrash)
        assertTrue(openedStatistics)
        assertTrue(toggledSort)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun searchActionsExposeFiltersAndResultOrdering() {
        var openedFilters = false
        var selectedSort: HomeSearchSort? = null

        composeRule.setContent {
            NexNoteTheme {
                HomeTopAppBar(
                    uiState = HomeUiState(isSearchActive = true),
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                    searchFocusRequester = remember { FocusRequester() },
                    onSearchQueryChange = {},
                    onSearchToggle = {},
                    onOpenSearchFilters = { openedFilters = true },
                    onSearchSortChange = { selectedSort = it },
                    onSortToggle = {},
                    onViewModeToggle = {},
                    onOpenTrash = {},
                    onOpenStatistics = {},
                    onOpenVault = {},
                    onStartSelection = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Filter search results")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("Sort search results: Relevance")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Title A–Z").performClick()

        assertTrue(openedFilters)
        assertEquals(HomeSearchSort.TITLE_ASC, selectedSort)
    }
}
