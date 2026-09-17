package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import io.github.r0x4nk.nexnote.util.toggleMarkdownTaskListItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownTaskAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readOnlyPreview_exposesTaskStateWithoutAnEditAction() {
        composeRule.setContent {
            NexNoteTheme {
                MarkdownPreview(
                    markdown = "- [x] Archived task",
                    lazyListState = rememberLazyListState()
                )
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Archived task").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Archived task")
            .assertIsOn()
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun eachTaskHasIndependentStateAndAction_includingNestedAndQuotedTasks() {
        val markdown = mutableStateOf("- [ ] **First** task\n  - [X] Nested task\n\n> - [ ] Quoted task")
        composeRule.setContent {
            NexNoteTheme {
                MarkdownPreview(
                    markdown = markdown.value,
                    lazyListState = rememberLazyListState(),
                    onTaskListItemClick = { offset ->
                        toggleMarkdownTaskListItem(markdown.value, offset)?.let { markdown.value = it }
                    }
                )
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Quoted task").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("First task")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff().performClick()
        composeRule.waitUntil(5_000) { markdown.value.startsWith("- [x]") }
        composeRule.onNodeWithContentDescription("First task").assertIsOn()
        composeRule.onNodeWithContentDescription("Nested task").assertIsOn().performClick()
        composeRule.onNodeWithContentDescription("Quoted task").assertIsOff().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onNodeWithContentDescription("Quoted task").fetchSemanticsNode()
                .config[SemanticsProperties.ToggleableState] == androidx.compose.ui.state.ToggleableState.On
        }
        composeRule.runOnIdle {
            assertEquals("- [x] **First** task\n  - [ ] Nested task\n\n> - [x] Quoted task", markdown.value)
        }
        composeRule.onNodeWithContentDescription("First task").performClick()
        composeRule.waitUntil(5_000) { markdown.value.startsWith("- [ ]") }
    }
}
