package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NoteSearchFiltersSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun largeTextKeepsPinnedChoicesAndResetReachableWithoutDismissing() {
        val scope = mutableStateOf(NoteSearchScope.TITLE)
        val pinned = mutableStateOf(NotePinnedFilter.ALL)
        val tags = mutableStateOf(setOf("work"))
        var clearCalls = 0
        var dismissCalls = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                NexNoteTheme {
                    NoteSearchFiltersSheet(
                        searchScope = scope.value,
                        pinnedFilter = pinned.value,
                        selectedTagFilters = tags.value,
                        availableTagNames = listOf("work", "ideas"),
                        onSearchScopeChange = { scope.value = it },
                        onPinnedFilterChange = { pinned.value = it },
                        onToggleTagFilter = { tags.value = tags.value + it },
                        onClearTagFilters = { clearCalls++; tags.value = emptySet() },
                        onDismiss = { dismissCalls++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Not pinned").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(NotePinnedFilter.UNPINNED, pinned.value) }
        compose.onNodeWithText("Reset filters").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Reset filters").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(NoteSearchScope.TITLE_AND_CONTENT, scope.value)
            assertEquals(NotePinnedFilter.ALL, pinned.value)
            assertEquals(emptySet<String>(), tags.value)
            assertEquals(1, clearCalls)
            assertEquals(0, dismissCalls)
        }
    }
}
