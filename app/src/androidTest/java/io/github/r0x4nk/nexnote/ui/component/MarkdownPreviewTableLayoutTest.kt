package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownPreviewTableLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun horizontalScrollMode_exposesScrollSemantics() {
        composeRule.setContent {
            NexNoteTheme {
                MarkdownPreview(
                    markdown = """
                        | First | Second | Third | Fourth |
                        | --- | --- | --- | --- |
                        | A | B | C | D |
                    """.trimIndent(),
                    lazyListState = rememberLazyListState(),
                    tableLayoutMode = TableLayoutMode.HORIZONTAL_SCROLL,
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag(MARKDOWN_TABLE_SCROLL_CONTAINER_TAG)
            .assert(hasScrollAction())
    }
}
