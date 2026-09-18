package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Rule
import org.junit.Test

class NoteCardMarkdownRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun legacySharedNoteRendersMarkdownInPreviewCard() {
        assertLegacyNoteRenders(NoteCardStyle.TITLE_AND_PREVIEW)
    }

    @Test
    fun legacySharedNoteRendersMarkdownInInformationCard() {
        assertLegacyNoteRenders(NoteCardStyle.TITLE_INFORMATION)
    }

    private fun assertLegacyNoteRenders(style: NoteCardStyle) {
        compose.setContent {
            NexNoteTheme {
                NoteCard(
                    note = Note(
                        id = 1,
                        title = "**Shared note**",
                        content = "# Heading\n**Bold** and [Docs](https://example.com)\n- [x] Done",
                        isMarkdown = false
                    ),
                    noteCardStyle = style,
                    onClick = {},
                    onTrash = {}
                )
            }
        }
        compose.onNodeWithText("Shared note", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Heading\nBold and Docs\n☑ Done", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
