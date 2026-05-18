package io.github.r0x4nk.nexnote.ui.component

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class MarkdownPreviewVaultImageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun vaultImageByteProvider_rendersImageWithoutUsingFileProvider() {
        val requestedPaths = CopyOnWriteArrayList<String>()
        val fileProviderCalls = AtomicInteger(0)
        val imageBytes = createPngBytes()

        composeRule.setContent {
            NexNoteTheme {
                MarkdownPreview(
                    markdown = "![Vault image](images/vault-note.png)",
                    lazyListState = rememberLazyListState(),
                    imageFileProvider = {
                        fileProviderCalls.incrementAndGet()
                        File("unused")
                    },
                    vaultImageByteProvider = { relativePath ->
                        requestedPaths += relativePath
                        imageBytes
                    }
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Vault image")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Vault image").assertIsDisplayed()
        assertTrue(requestedPaths.contains("images/vault-note.png"))
        assertEquals(0, fileProviderCalls.get())
    }

    @Test
    fun vaultImageByteProvider_nullResultShowsMissingImagePlaceholder() {
        composeRule.setContent {
            NexNoteTheme {
                MarkdownPreview(
                    markdown = "![Vault image](images/missing-vault-note.png)",
                    lazyListState = rememberLazyListState(),
                    vaultImageByteProvider = { null }
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Immagine non trovata")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText("Immagine non trovata").assertIsDisplayed()
    }
}

private fun createPngBytes(): ByteArray {
    val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.rgb(42, 96, 180))
    return ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    }
}
