package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorTagChipsRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun removeAllTagsActionIsShownAndInvokesItsCallback() {
        var requested = false
        composeRule.setContent {
            NexNoteTheme {
                TagChipsEditorRow(
                    tags = listOf(
                        Tag(name = "work", noteCount = 1, createdDate = 0L, lastUpdatedDate = 0L)
                    ),
                    selectedTag = null,
                    onTagClick = {},
                    onClearSelection = {},
                    onClearAllTags = { requested = true },
                    isPinned = false,
                    onTogglePin = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Remove all tags")
            .assertIsDisplayed()
            .performClick()

        assertTrue(requested)
    }
}
