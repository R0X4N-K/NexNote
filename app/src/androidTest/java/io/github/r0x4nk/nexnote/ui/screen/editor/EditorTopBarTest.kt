package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noteMetadataIsRenderedAsTopBarSubtitle() {
        composeRule.setContent {
            NexNoteTheme {
                EditorTopBar(
                    isSaving = false,
                    title = "Preview note",
                    isTemplateMode = false,
                    isReadOnly = false,
                    containerColor = Color.White,
                    toolingState = EditorTopBarToolingState(hasCustomColor = false),
                    toolingActions = EditorTopBarToolingActions(onToggleColorPicker = {}),
                    searchState = NoteSearchState(),
                    searchFocusRequester = remember { FocusRequester() },
                    metadata = EditorNoteMetadata(
                        characterCount = 42,
                        lastModifiedDate = null,
                        creationDate = 1_735_689_600_000L
                    ),
                    onBack = {},
                    onSearchOpen = {},
                    onSearchClose = {},
                    onSearchQueryChange = {},
                    onSearchPrevious = {},
                    onSearchNext = {}
                )
            }
        }

        composeRule.onNodeWithTag(EDITOR_METADATA_BAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("42 chars", substring = true).assertIsDisplayed()
    }

    @Test
    fun noteOptionsOffersCreationDateEditing() {
        var dateEditRequested = false

        composeRule.setContent {
            NexNoteTheme {
                EditorTopBar(
                    isSaving = false,
                    title = "Editable note",
                    isTemplateMode = false,
                    isReadOnly = false,
                    containerColor = Color.White,
                    toolingState = EditorTopBarToolingState(hasCustomColor = false),
                    toolingActions = EditorTopBarToolingActions(onToggleColorPicker = {}),
                    searchState = NoteSearchState(),
                    searchFocusRequester = remember { FocusRequester() },
                    onBack = {},
                    onSearchOpen = {},
                    onSearchClose = {},
                    onSearchQueryChange = {},
                    onSearchPrevious = {},
                    onSearchNext = {},
                    onCreationDateEdit = { dateEditRequested = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Note options").performClick()
        composeRule.onNodeWithText("Edit creation date")
            .assertIsDisplayed()
            .performClick()

        assertTrue(dateEditRequested)
    }

    @Test
    fun noteOptionsCopyMenuOffersTextAndMarkdownChoices() {
        var copiedText = false
        var copiedMarkdown = false

        composeRule.setContent {
            NexNoteTheme {
                EditorTopBar(
                    isSaving = false,
                    title = "Preview note",
                    isTemplateMode = false,
                    isReadOnly = false,
                    containerColor = Color.White,
                    toolingState = EditorTopBarToolingState(hasCustomColor = false),
                    toolingActions = EditorTopBarToolingActions(onToggleColorPicker = {}),
                    searchState = NoteSearchState(),
                    searchFocusRequester = remember { FocusRequester() },
                    onBack = {},
                    onSearchOpen = {},
                    onSearchClose = {},
                    onSearchQueryChange = {},
                    onSearchPrevious = {},
                    onSearchNext = {},
                    onCopyNoteAsText = { copiedText = true },
                    onCopyNoteAsMarkdown = { copiedMarkdown = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Note options")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Copy note")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Copy as text")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithContentDescription("Note options").performClick()
        composeRule.onNodeWithText("Copy note").performClick()
        composeRule.onNodeWithText("Copy as Markdown")
            .assertIsDisplayed()
            .performClick()

        assertTrue(copiedText)
        assertTrue(copiedMarkdown)
    }
}
