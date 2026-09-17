package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownAttachmentCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun galleryLight() = gallery(false)
    @Test fun galleryDark() = gallery(true)

    private fun gallery(dark: Boolean) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File.createTempFile("attachment-gallery-", ".bin", context.cacheDir).apply { writeBytes(ByteArray(1024)) }
        try {
            compose.setContent {
                io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme(darkTheme = dark) {
                    androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(24.dp)) {
                            androidx.compose.material3.Text("Project materials", style = MaterialTheme.typography.headlineMedium)
                            androidx.compose.material3.Text("Everything for the next review", Modifier.padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge)
                            listOf("Project brief.pdf", "Meeting notes.docx", "Budget 2026.xlsx", "Voice memo.m4a").forEach { name ->
                                MarkdownAttachmentCard(NoteAttachment("images/attachments/note_1_abcd.${name.substringAfterLast('.')}", name), { file }, false)
                            }
                        }
                    }
                }
            }
            val size = android.text.format.Formatter.formatShortFileSize(context, 1024)
            compose.waitUntil(5_000) { compose.onAllNodesWithText(size, substring = true).fetchSemanticsNodes().size == 4 }
            val output = java.io.File(context.getExternalFilesDir(null), "attachment-gallery-${if (dark) "dark" else "light"}.png")
            output.outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            // Preserve review captures after Gradle removes the isolated test application.
            val shell = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("cp ${output.absolutePath} /data/local/tmp/${output.name}")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(shell).use { it.readBytes() }
        } finally { file.delete() }
    }

    @Test fun vaultCardsExposeMetadataWithoutReadingOrDecryptingPayload() {
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MarkdownAttachmentCard(NoteAttachment("images/attachments/note_1_abcd.pdf", "Private report.pdf"),
                    fileProvider = { error("Vault preview must not read payload") }, isVault = true)
            }
        }
        compose.onNodeWithText("Private report.pdf").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun unknownAndMissingFilesRenderReadableCardsInLightTheme() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Column {
                    MarkdownAttachmentCard(NoteAttachment("images/attachments/note_1_abcd.xyz", "Research.xyz"), null, false)
                    MarkdownAttachmentCard(NoteAttachment("images/attachments/note_1_abcd.mp3", "Voice note.mp3"), null, false)
                }
            }
        }
        compose.onNodeWithText("Research.xyz").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Voice note.mp3").assertIsDisplayed()
    }
}
