package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditorKeyboardToolbarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun formattingMenusAndPinnedHistoryKeepTheirActionsOnCompactWidth() {
        val heading = mutableStateOf(false)
        val links = mutableStateOf(false)
        val actions = mutableListOf<String>()
        var measuredHeight = 0
        var expectedHeight = 0
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                expectedHeight = with(LocalDensity.current) { EditorKeyboardToolbarMinHeight.roundToPx() }
                NexNoteTheme {
                    Box(Modifier.width(320.dp)) {
                        EditorKeyboardToolbar(
                            visible = true,
                            isTemplateMode = false,
                            canInsertAttachments = true,
                            canUndo = true,
                            canRedo = false,
                            linkMenuExpanded = links.value,
                            onOpenLinkMenu = { links.value = true },
                            headingMenuExpanded = heading.value,
                            onOpenHeadingMenu = { heading.value = true },
                            onUndo = { actions += "undo" },
                            onRedo = { actions += "redo" },
                            onInsertAttachment = { actions += "attachment" },
                            onInsertChecklist = { actions += "checklist" },
                            onIndent = { actions += "indent" },
                            onOutdent = { actions += "outdent" },
                            onToggleBold = { actions += "bold" },
                            onToggleItalic = {},
                            onToggleStrikethrough = { actions += "strike" },
                            onToggleInlineCode = {},
                            onInsertCodeBlock = {},
                            onToggleQuote = {},
                            onToggleUnorderedList = {},
                            onToggleOrderedList = {},
                            onInsertHorizontalRule = {},
                            onHeightChanged = { measuredHeight = it }
                        )
                        if (heading.value) {
                            EditorHeadingSheet(
                                onDismissRequest = { heading.value = false },
                                onSelectLevel = { level -> actions += "H$level"; heading.value = false }
                            )
                        }
                        if (links.value) {
                            EditorLinkSheet(
                                onDismissRequest = { links.value = false },
                                onInsertWebLink = { actions += "web"; links.value = false },
                                onInsertNoteLink = { actions += "note"; links.value = false }
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Redo").assertIsNotEnabled()
        val undoBounds = compose.onNodeWithContentDescription("Undo").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Bold").performClick()
        val headingLabel = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.editor_heading_level)
        for (level in listOf(1, 6)) {
            compose.onNodeWithContentDescription(headingLabel).performScrollTo().performClick()
            compose.waitUntil { compose.onAllNodesWithText("H$level").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("H$level").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Checklist").performScrollTo().performClick()
        for (label in listOf("Web link", "Note link")) {
            compose.onNodeWithContentDescription("Insert link").performScrollTo().performClick()
            compose.waitUntil { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(label).performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Strikethrough").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Undo").assertIsDisplayed().performClick()
        assertEquals(undoBounds, compose.onNodeWithContentDescription("Undo").fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle {
            assertEquals(expectedHeight, measuredHeight)
            assertEquals(listOf("bold", "H1", "H6", "checklist", "web", "note", "strike", "undo"), actions)
        }
    }

    @Test
    fun indentAndOutdentButtonsInvokeTheirActions() {
        val actions = mutableListOf<String>()
        compose.setContent {
            NexNoteTheme {
                Box(Modifier.width(320.dp)) {
                    EditorKeyboardToolbar(
                        visible = true,
                        isTemplateMode = false,
                        canInsertAttachments = true,
                        canUndo = false,
                        canRedo = false,
                        linkMenuExpanded = false,
                        onOpenLinkMenu = {},
                        headingMenuExpanded = false,
                        onOpenHeadingMenu = {},
                        onUndo = {},
                        onRedo = {},
                        onInsertAttachment = {},
                        onInsertChecklist = {},
                        onIndent = { actions += "indent" },
                        onOutdent = { actions += "outdent" },
                        onToggleBold = {},
                        onToggleItalic = {},
                        onToggleStrikethrough = {},
                        onToggleInlineCode = {},
                        onInsertCodeBlock = {},
                        onToggleQuote = {},
                        onToggleUnorderedList = {},
                        onToggleOrderedList = {},
                        onInsertHorizontalRule = {}
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Increase indent").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Decrease indent").performScrollTo().performClick()

        compose.runOnIdle { assertEquals(listOf("indent", "outdent"), actions) }
    }
}
