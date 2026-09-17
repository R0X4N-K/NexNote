package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RestyledComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun informationStyleShowsTagsAndHasNoMarkdownBadge() {
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(title = "Plan", content = "Body #work #home"),
                    onClick = {}, onTrash = {},
                    noteCardStyle = NoteCardStyle.TITLE_INFORMATION
                )
            }
        }

        compose.onNodeWithText("#home #work").assertIsDisplayed()
        compose.onNodeWithText("MD").assertDoesNotExist()
    }

    @Test
    fun previewStyleShowsTheBodyButNotTheMetadataFooter() {
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(title = "Plan", content = "Body #work"),
                    onClick = {}, onTrash = {}
                )
            }
        }

        compose.onNodeWithText("Body #work").assertIsDisplayed()
        compose.onNodeWithText("#work").assertDoesNotExist()
    }

    @Test
    fun titleOnlyStyleShowsOnlyTheTitle() {
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(title = "Plan", content = "Body preview text"),
                    onClick = {}, onTrash = {},
                    noteCardStyle = NoteCardStyle.TITLE_ONLY
                )
            }
        }

        compose.onNodeWithText("Plan").assertIsDisplayed()
        compose.onNodeWithText("Body preview text").assertDoesNotExist()
    }

    @Test
    fun untitledNoteKeepsTheWholeBodyInThePreviewWithoutBoldingIt() {
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(title = "", content = "First line\nSecond line body"),
                    onClick = {}, onTrash = {}
                )
            }
        }

        compose.onNodeWithText("Untitled note").assertIsDisplayed()
        compose.onNodeWithText("First line\nSecond line body").assertIsDisplayed()
    }

    @Test
    fun cardLongPressSelectsAndTapOpensTheNote() {
        var opens = 0
        var selections = 0
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(title = "Weekend plans", content = "A note to keep"),
                    onClick = { opens++ }, onTrash = {},
                    onLongPress = { selections++ }
                )
            }
        }
        compose.onNodeWithText("Weekend plans").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1, selections); assertEquals(0, opens) }
        compose.onNodeWithText("Weekend plans").performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test
    fun dismissTargetDoesNotSelectTagOrNeighbour() {
        var selected = 0
        var removed = 0
        var neighbour = 0
        compose.setContent {
            NexNoteTheme(fontScale = 1.15f) {
                Column(Modifier.padding(12.dp)) {
                    TagChip("weekend", { selected++ }, dismissible = true, onDismiss = { removed++ })
                    TagChip("ideas", { neighbour++ }, compact = true)
                }
            }
        }
        compose.onNodeWithContentDescription("Remove #weekend filter")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle {
            assertEquals(1, removed); assertEquals(0, selected); assertEquals(0, neighbour)
        }
        compose.onNodeWithText("#weekend").performClick()
        compose.onNodeWithText("#ideas").performClick()
        compose.runOnIdle {
            assertEquals(1, removed); assertEquals(1, selected); assertEquals(1, neighbour)
        }
    }

    @Test
    fun noteSwipesStillPinAndTrashExactlyOnce() {
        var pins = 0
        var deletes = 0
        val note = mutableStateOf(Note(title = "Swipe note", content = "Original gestures"))
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = note.value,
                    onClick = {},
                    onPin = {
                        pins++
                        note.value = note.value.copy(isPinned = !note.value.isPinned)
                    },
                    onTrash = { deletes++ },
                    modifier = Modifier.testTag("swipe_note")
                )
            }
        }
        compose.onNodeWithTag("swipe_note").performTouchInput { swipeRight() }
        compose.waitUntil(3_000) { pins == 1 }
        compose.waitForIdle()
        compose.onNodeWithTag("swipe_note").performTouchInput { swipeLeft() }
        compose.waitUntil(3_000) { deletes == 1 }
        compose.runOnIdle { assertEquals(1, pins); assertEquals(1, deletes) }
    }

    @Test
    fun selectionModeStillDisablesSwipeActions() {
        var actions = 0
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(title = "Selected note"),
                    onClick = {}, onPin = { actions++ }, onTrash = { actions++ },
                    selectionMode = true, selected = true,
                    modifier = Modifier.testTag("selected_note")
                )
            }
        }
        compose.onNodeWithTag("selected_note").performTouchInput { swipeLeft(); swipeRight() }
        compose.runOnIdle { assertEquals(0, actions) }
    }
}
