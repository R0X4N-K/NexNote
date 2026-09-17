package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
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
    fun editModeDrawsContinuousFadeWithoutOpaqueBottomBand() {
        composeEditorContentMode(showPreview = false)

        composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG).assertIsDisplayed()
        assertContentFieldReachesHostBottom()
        assertContinuousFade(Color.White)
    }

    @Test
    fun editTextRendersInsideFadeInsteadOfStoppingAboveIt() {
        var density = 1f
        val longContent = List(80) { index -> "Fade overlay line $index" }.joinToString("\n")

        composeEditorContentMode(
            showPreview = false,
            content = longContent,
            onDensityReady = { density = it }
        )

        val hostBounds = composeRule.onNodeWithTag(EDITOR_FADE_TEST_HOST_TAG)
            .getUnclippedBoundsInRoot()
        val fadeBounds = composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG)
            .getUnclippedBoundsInRoot()
        val bitmap = composeRule.onNodeWithTag(EDITOR_FADE_TEST_HOST_TAG)
            .captureToImage()
            .asAndroidBitmap()
        val fadeTopPx = ((fadeBounds.top.value - hostBounds.top.value) * density).toInt()
        val fadeBottomPx = ((fadeBounds.bottom.value - hostBounds.top.value) * density).toInt()
        val scanBottomPx = fadeTopPx + ((fadeBottomPx - fadeTopPx) * 0.6f).toInt()
        val backgroundSampleX = (3 * density).toInt().coerceIn(0, bitmap.width - 1)
        val textScanStartX = (12 * density).toInt().coerceIn(0, bitmap.width - 1)
        val textScanEndX = (300 * density).toInt().coerceIn(textScanStartX + 1, bitmap.width)
        var textPixelsInsideFade = 0

        for (y in fadeTopPx until scanBottomPx) {
            val rowBackground = bitmap.getPixel(backgroundSampleX, y)
            for (x in textScanStartX until textScanEndX) {
                if (colorDistance(bitmap.getPixel(x, y), rowBackground) > 24) {
                    textPixelsInsideFade += 1
                }
            }
        }

        assertTrue(
            "Edit text must continue into the fade instead of ending in an empty band",
            textPixelsInsideFade > 40
        )
    }

    @Test
    fun editAndPreviewUseTheSameBottomFade() {
        val showPreview = composeEditorContentMode(showPreview = false)
        val editFade = captureFadeCenterColumn()

        composeRule.runOnIdle { showPreview.value = true }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG).assertIsDisplayed()
        val previewFade = captureFadeCenterColumn()

        assertEquals(editFade.size, previewFade.size)
        editFade.indices.forEach { row ->
            assertTrue(
                "Edit and Preview differ at fade row $row",
                colorDistance(editFade[row], previewFade[row]) <= 2
            )
        }
    }

    @Test
    fun editModeHidesFadeAboveVisibleKeyboardToolbar() {
        lateinit var editorState: EditorScreenState

        composeEditorContentMode(
            showPreview = false,
            keyboardToolbarVisible = true,
            onStateReady = { editorState = it }
        )

        composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG).assertDoesNotExist()
        assertContentFieldBottomOffset(EditorKeyboardToolbarMinHeight.value)
        assertEquals(0, editorState.bottomFadeHeightPx)
    }

    @Test
    fun scrollableEditContentKeepsLastLineAboveFade() {
        lateinit var editorState: EditorScreenState
        var density = 1f
        val longContent = List(80) { index -> "Scrollable editor line $index" }.joinToString("\n")

        composeEditorContentMode(
            showPreview = false,
            content = longContent,
            onStateReady = { editorState = it },
            onDensityReady = { density = it }
        )
        composeRule.waitUntil {
            editorState.contentScrollState.maxValue > 0 && editorState.textLayoutResult != null
        }

        composeRule.runOnIdle {
            editorState.contentScrollState.dispatchRawDelta(
                editorState.contentScrollState.maxValue.toFloat()
            )
        }
        composeRule.waitUntil {
            editorState.contentScrollState.value == editorState.contentScrollState.maxValue
        }

        assertContentFieldReachesHostBottom()

        val fieldTopDp = composeRule.onNodeWithTag(EDITOR_CONTENT_FIELD_TAG)
            .getUnclippedBoundsInRoot().top.value
        val fadeTopDp = composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG)
            .getUnclippedBoundsInRoot().top.value
        val lastLineBottomPx = editorState.textLayoutResult!!
            .getCursorRect(longContent.length)
            .bottom
        val lastLineBottomInRootPx = fieldTopDp * density +
            lastLineBottomPx - editorState.contentScrollState.value
        val fadeTopInRootPx = fadeTopDp * density

        assertTrue(
            "Last line bottom $lastLineBottomInRootPx must remain above fade top $fadeTopInRootPx",
            lastLineBottomInRootPx <= fadeTopInRootPx + density
        )
    }

    @Test
    fun customNoteBackgroundKeepsContinuousFadeInLightTheme() {
        val noteBackground = Color(0xFFFFE8A3)

        composeEditorContentMode(
            showPreview = false,
            noteBackground = noteBackground,
            darkTheme = false
        )

        assertContentFieldReachesHostBottom()
        assertContinuousFade(noteBackground)
    }

    @Test
    fun customNoteBackgroundKeepsContinuousFadeInDarkTheme() {
        val noteBackground = Color(0xFF263238)

        composeEditorContentMode(
            showPreview = false,
            noteBackground = noteBackground,
            darkTheme = true
        )

        assertContentFieldReachesHostBottom()
        assertContinuousFade(noteBackground)
    }

    private fun composeEditorContentMode(
        showPreview: Boolean,
        content: String = "",
        keyboardToolbarVisible: Boolean = false,
        noteBackground: Color = Color.White,
        darkTheme: Boolean = false,
        onStateReady: (EditorScreenState) -> Unit = {},
        onDensityReady: (Float) -> Unit = {}
    ): MutableState<Boolean> {
        val showPreviewState = mutableStateOf(showPreview)

        composeRule.setContent {
            NexNoteTheme(darkTheme = darkTheme) {
                Column(
                    modifier = Modifier
                        .size(width = 360.dp, height = 420.dp)
                        .background(noteBackground)
                        .testTag(EDITOR_FADE_TEST_HOST_TAG)
                ) {
                    val state = rememberEditorScreenState(EditorMode.NewNote())
                    val density = LocalDensity.current.density
                    val uiState = EditorUiState(
                        content = content,
                        showPreview = showPreviewState.value,
                        contentVersion = 1
                    )
                    LaunchedEffect(state, content) {
                        state.syncedContentVersion = uiState.contentVersion
                        state.setContentFieldValue(TextFieldValue(uiState.content))
                    }
                    SideEffect {
                        onStateReady(state)
                        onDensityReady(density)
                    }
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
        return showPreviewState
    }

    private fun assertContentFieldReachesHostBottom() {
        assertContentFieldBottomOffset(0f)
    }

    private fun assertContentFieldBottomOffset(expectedOffsetDp: Float) {
        val hostBottom = composeRule.onNodeWithTag(EDITOR_FADE_TEST_HOST_TAG)
            .getUnclippedBoundsInRoot().bottom.value
        val fieldBottom = composeRule.onNodeWithTag(EDITOR_CONTENT_FIELD_TAG)
            .getUnclippedBoundsInRoot().bottom.value

        assertEquals(expectedOffsetDp, hostBottom - fieldBottom, 1f)
    }

    private fun assertContinuousFade(noteBackground: Color) {
        val column = captureFadeCenterColumn()
        val backgroundArgb = noteBackground.toArgb()
        val uniqueColors = column.toSet().size
        val bottomDistance = colorDistance(column.last(), backgroundArgb)
        var maxAdjacentDistance = 0
        for (row in 1 until column.size) {
            maxAdjacentDistance = maxOf(
                maxAdjacentDistance,
                colorDistance(column[row - 1], column[row])
            )
        }

        assertTrue("Fade should contain multiple intermediate colors", uniqueColors >= 6)
        assertTrue("Fade bottom should settle away from the plain background", bottomDistance > 4)
        assertTrue("Fade should not contain an abrupt color step", maxAdjacentDistance <= 12)
    }

    private fun captureFadeCenterColumn(): IntArray {
        val bitmap = composeRule.onNodeWithTag(EDITOR_BOTTOM_FADE_TAG)
            .captureToImage()
            .asAndroidBitmap()
        val centerX = bitmap.width / 2
        return IntArray(bitmap.height) { y -> bitmap.getPixel(centerX, y) }
    }
}

private fun colorDistance(first: Int, second: Int): Int =
    kotlin.math.abs(android.graphics.Color.red(first) - android.graphics.Color.red(second)) +
        kotlin.math.abs(android.graphics.Color.green(first) - android.graphics.Color.green(second)) +
        kotlin.math.abs(android.graphics.Color.blue(first) - android.graphics.Color.blue(second))
