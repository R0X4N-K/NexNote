package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import io.github.r0x4nk.nexnote.util.toggleMarkdownTaskListItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownPreviewTaskListTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun taskListTap_togglesRenderedAndSourceStateInBothDirections() {
        val markdown = mutableStateOf("- [ ] Publish release")

        composeRule.setContent {
            NexNoteTheme {
                MarkdownPreview(
                    markdown = markdown.value,
                    lazyListState = rememberLazyListState(),
                    onTaskListItemClick = { markerOffset ->
                        toggleMarkdownTaskListItem(markdown.value, markerOffset)?.let {
                            markdown.value = it
                        }
                    }
                )
            }
        }

        composeRule.waitForText("☐ Publish release")
        composeRule.onNodeWithText("☐ Publish release")
            .performTouchInput { click(Offset(x = 8f, y = center.y)) }

        composeRule.waitForText("☑ Publish release")
        composeRule.onNodeWithText("☑ Publish release").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("- [x] Publish release", markdown.value)
        }

        composeRule.onNodeWithText("☑ Publish release")
            .performTouchInput { click(Offset(x = 8f, y = center.y)) }

        composeRule.waitForText("☐ Publish release")
        composeRule.runOnIdle {
            assertEquals("- [ ] Publish release", markdown.value)
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
