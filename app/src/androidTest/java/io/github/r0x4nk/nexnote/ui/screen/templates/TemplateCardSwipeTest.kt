package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.Template
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateCardSwipeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun predefinedTemplateCard_swipeLeftRequestsDelete() {
        var deleteRequests = 0

        composeRule.setContent {
            NexNoteTheme {
                TemplateCard(
                    template = Template(
                        id = 7L,
                        name = "Daily plan",
                        content = "Template body",
                        isPredefined = true
                    ),
                    onApply = {},
                    onEdit = null,
                    onDelete = { deleteRequests++ }
                )
            }
        }

        composeRule.onNodeWithText("Daily plan")
            .performTouchInput { swipeLeft() }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            deleteRequests == 1
        }
    }
}
