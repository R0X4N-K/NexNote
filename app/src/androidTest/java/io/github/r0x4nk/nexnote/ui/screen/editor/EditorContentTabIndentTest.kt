package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.requestFocus
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
class EditorContentTabIndentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabRequestsIndentAndShiftTabRequestsOutdent() {
        val fieldState = TextFieldState("- [ ] task")
        var indentRequests = 0
        var outdentRequests = 0

        composeRule.setContent {
            NexNoteTheme {
                ContentField(
                    textFieldState = fieldState,
                    scrollState = rememberScrollState(),
                    onContentEdited = {},
                    onIndent = { indentRequests++ },
                    onOutdent = { outdentRequests++ },
                    modifier = Modifier.testTag("content")
                )
            }
        }

        composeRule.onNodeWithTag("content").requestFocus()
        composeRule.onNodeWithTag("content").performKeyInput {
            keyDown(Key.Tab)
            keyUp(Key.Tab)
        }
        composeRule.onNodeWithTag("content").performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.Tab)
            keyUp(Key.Tab)
            keyUp(Key.ShiftLeft)
        }

        composeRule.runOnIdle {
            assertEquals(1, indentRequests)
            assertEquals(1, outdentRequests)
        }
    }
}
