package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import io.github.r0x4nk.nexnote.domain.model.HomeSearchSort
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderExpansionState
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun searchTransitionKeepsFocusTypingAndCloseActionOnCompactWidth() {
        val state = mutableStateOf(HomeUiState(isLoading = false))
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.3f)) {
                NexNoteTheme {
                    val requester = remember { FocusRequester() }
                    LaunchedEffect(state.value.isSearchActive) {
                        if (state.value.isSearchActive) requester.requestFocus()
                    }
                    Box(Modifier.width(320.dp)) {
                        HomeTopAppBar(
                            uiState = state.value,
                            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                            tagFolderExpansion = NoteTagFolderExpansionState(),
                            searchFocusRequester = requester,
                            onSearchQueryChange = { state.value = state.value.copy(searchQuery = it) },
                            onSearchToggle = { state.value = state.value.copy(isSearchActive = it) },
                            onOpenSearchFilters = {},
                            onSearchSortChange = {},
                            onSortToggle = {},
                            onViewModeToggle = {},
                            onOpenTrash = {},
                            onOpenStatistics = {},
                            onOpenVault = {},
                            onStartSelection = {}
                        )
                    }
                }
            }
        }
        repeat(2) {
            composeRule.onNodeWithContentDescription("Search").performClick()
            composeRule.onNode(hasSetTextAction()).assertIsDisplayed().assertIsFocused()
                .performTextInput("note")
            composeRule.onNodeWithContentDescription("Filter search results").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Close search").assertIsDisplayed().performClick()
            composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        }
        assertEquals("notenote", state.value.searchQuery)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun browsingActionsExposeBrandSortAndOverflowDestinations() {
        var openedVault = false
        var openedTrash = false
        var openedStatistics = false
        var toggledSort = false
        val darkTheme = mutableStateOf(false)
        lateinit var scrollBehavior: TopAppBarScrollBehavior
        var expectedBackdrop = Color.Unspecified

        composeRule.setContent {
            NexNoteTheme(darkTheme = darkTheme.value) {
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                // A distinct backdrop proves the bar stays transparent, including after scrolling.
                expectedBackdrop = MaterialTheme.colorScheme.primaryContainer
                Box(Modifier.background(expectedBackdrop)) {
                    HomeTopAppBar(
                        uiState = HomeUiState(totalNoteCount = 500, isLoading = false),
                        scrollBehavior = scrollBehavior,
                        tagFolderExpansion = NoteTagFolderExpansionState(),
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

        for (dark in listOf(false, true)) {
            composeRule.runOnIdle {
                darkTheme.value = dark
                scrollBehavior.state.contentOffset = 0f
            }
            composeRule.waitForIdle()
            val barY = composeRule.onNodeWithContentDescription("NexNote app icon")
                .fetchSemanticsNode().boundsInRoot.center.y.toInt()
            val resting = composeRule.onRoot().captureToImage().toPixelMap()
            assertEquals(expectedBackdrop, resting[0, barY])
            composeRule.runOnIdle { scrollBehavior.state.contentOffset = -1_000f }
            composeRule.waitForIdle()
            val scrolled = composeRule.onRoot().captureToImage().toPixelMap()
            assertTrue(scrollBehavior.state.overlappedFraction > 0.99f)
            assertEquals(resting[0, barY], scrolled[0, barY])
        }
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
                    tagFolderExpansion = NoteTagFolderExpansionState(),
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
