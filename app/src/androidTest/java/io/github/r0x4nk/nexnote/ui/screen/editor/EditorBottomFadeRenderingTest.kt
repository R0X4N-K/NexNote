package io.github.r0x4nk.nexnote.ui.screen.editor

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val EDITOR_FADE_TEST_HOST_TAG = "editor_fade_test_host"

@RunWith(AndroidJUnit4::class)
class EditorBottomFadeRenderingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editModeDrawsBottomFade() {
        composeEditorContentMode(showPreview = false)

        composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG).assertIsDisplayed()
        assertTrue(bottomBandContainsFadePixels())
    }

    @Test
    fun previewModeDrawsBottomFade() {
        composeEditorContentMode(showPreview = true)

        composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG).assertIsDisplayed()
        assertTrue(bottomBandContainsFadePixels())
    }

    @Test
    fun editModeKeepsFadeWhenKeyboardToolbarIsVisible() {
        lateinit var editorState: EditorScreenState

        composeEditorContentMode(
            showPreview = false,
            keyboardToolbarVisible = true,
            onStateReady = { editorState = it }
        )

        composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG).assertIsDisplayed()
        assertTrue(editorState.bottomFadeHeightPx > 0)
    }

    @Test
    fun scrollableEditContentExtendsBehindBottomFade() {
        lateinit var editorState: EditorScreenState
        val longContent = List(80) { index -> "Scrollable editor line $index" }.joinToString("\n")

        composeEditorContentMode(
            showPreview = false,
            content = longContent,
            onStateReady = { editorState = it }
        )
        composeRule.waitUntil { editorState.contentScrollState.maxValue > 0 }

        val hostBottom = composeRule.onNodeWithTag(EDITOR_FADE_TEST_HOST_TAG)
            .getUnclippedBoundsInRoot().bottom.value
        val fieldBottom = composeRule.onNodeWithTag(EDITOR_CONTENT_FIELD_TAG)
            .getUnclippedBoundsInRoot().bottom.value

        assertEquals(8f, hostBottom - fieldBottom, 1f)
    }

    private fun composeEditorContentMode(
        showPreview: Boolean,
        content: String = "",
        keyboardToolbarVisible: Boolean = false,
        onStateReady: (EditorScreenState) -> Unit = {}
    ) {
        val noteBackground = Color.White
        val uiState = EditorUiState(
            content = content,
            showPreview = showPreview,
            contentVersion = 1
        )
        composeRule.setContent {
            NexNoteTheme {
                Column(
                    modifier = Modifier
                        .size(width = 360.dp, height = 420.dp)
                        .background(noteBackground)
                        .testTag(EDITOR_FADE_TEST_HOST_TAG)
                ) {
                    val state = rememberEditorScreenState(EditorMode.NewNote())
                    LaunchedEffect(state) {
                        state.syncedContentVersion = uiState.contentVersion
                        state.setContentFieldValue(TextFieldValue(uiState.content))
                    }
                    SideEffect { onStateReady(state) }
                    EditorContentModeBox(
                        uiState = uiState,
                        noteBackground = noteBackground,
                        imageFileProvider = { File("unused") },
                        vaultImageByteProvider = null,
                        noteLinkTargets = emptyList(),
                        state = state,
                        keyboardToolbarVisible = keyboardToolbarVisible,
                        onTogglePreview = {},
                        onContentEdited = {},
                        onContentSelectionChange = {},
                        onNoteLinkAutocompleteSelected = { _, _ -> },
                        onPreviewNoteLinkClick = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun bottomBandContainsFadePixels(): Boolean {
        val bitmap = composeRule.onNodeWithTag(EDITOR_FADE_TEST_HOST_TAG)
            .captureToImage()
            .asAndroidBitmap()
        return bottomBandContainsNonBackgroundPixels(
            bitmap = bitmap,
            backgroundArgb = Color.White.toArgb()
        )
    }
}

private fun bottomBandContainsNonBackgroundPixels(
    bitmap: Bitmap,
    backgroundArgb: Int
): Boolean {
    var changedPixels = 0
    val startY = (bitmap.height * 0.86f).toInt().coerceIn(0, bitmap.height - 1)

    for (y in startY until bitmap.height) {
        for (x in 0 until bitmap.width step 3) {
            if (colorDistance(bitmap.getPixel(x, y), backgroundArgb) > 8) {
                changedPixels += 1
                if (changedPixels > 40) return true
            }
        }
    }

    return false
}

private fun colorDistance(first: Int, second: Int): Int =
    kotlin.math.abs(android.graphics.Color.red(first) - android.graphics.Color.red(second)) +
        kotlin.math.abs(android.graphics.Color.green(first) - android.graphics.Color.green(second)) +
        kotlin.math.abs(android.graphics.Color.blue(first) - android.graphics.Color.blue(second))
