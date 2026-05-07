package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import io.github.r0x4nk.nexnote.ui.component.MarkdownSourceRange

internal data class ContentScrollAnchor(
    val charOffset: Int,
    val viewportFraction: Float = CONTENT_SCROLL_ANCHOR_FRACTION
)

internal data class DirectPreviewWarmupKey(
    val noteId: Long,
    val contentVersion: Int,
    val contentHash: Int,
    val linkColorValue: ULong
)

@Stable
internal class EditorScreenState(
    val snackbarHostState: SnackbarHostState,
    val contentScrollState: ScrollState,
    val previewListState: LazyListState,
    val titleFocusRequester: FocusRequester,
    val contentFocusRequester: FocusRequester,
    val searchFocusRequester: FocusRequester,
    val contentTextFieldState: TextFieldState,
    initialContentAnimationsEnabled: Boolean,
    val contentFieldValueState: MutableState<TextFieldValue>,
    val pendingImageInsertionOffsetState: MutableState<Int?>,
    val pendingContentScrollAnchorState: MutableState<ContentScrollAnchor?>
) {
    var showDatePicker by mutableStateOf(false)
    var showColorPicker by mutableStateOf(false)
    var showNoteLinkPicker by mutableStateOf(false)

    /**
     * Whether the keyboard toolbar's link-type chooser dropdown is currently expanded.
     *
     * The state is hoisted here (instead of being local to the dropdown composable)
     * because Material3's [androidx.compose.material3.DropdownMenu] uses a focusable
     * popup that briefly steals window focus, which closes the IME. When the IME
     * collapses, the toolbar's [androidx.compose.animation.AnimatedVisibility] would
     * normally tear the dropdown down before the user can interact with it. Keeping
     * the flag at the screen level lets the layout keep the toolbar mounted while the
     * menu is open so the user can pick "Web link" or "Note link" without flicker.
     */
    var showLinkTypeMenu by mutableStateOf(false)
    var tagsVisible by mutableStateOf(true)
    var tagsPinned by mutableStateOf(false)
    var tagBarHiddenByKeyboard by mutableStateOf(false)
    var contentAnimationsEnabled by mutableStateOf(initialContentAnimationsEnabled)
    var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
    var highlightRange by mutableStateOf<IntRange?>(null)
    var noteSearch by mutableStateOf(NoteSearchState.Empty)
    var pendingTagScroll by mutableStateOf<TagSearchState?>(null)
    /** Source ranges for each parsed markdown block, updated when content changes. */
    var currentSourceRanges by mutableStateOf<List<MarkdownSourceRange>>(emptyList())
    var contentViewportHeightPx by mutableStateOf(0)
    var completedDirectPreviewWarmupKey by mutableStateOf<DirectPreviewWarmupKey?>(null)
    var syncedContentVersion by mutableStateOf(-1)
    var contentEditRevision by mutableStateOf(0)
    var hasPendingContentCommit by mutableStateOf(false)
    var isNoteLinkAutocompleteVisible by mutableStateOf(false)

    val isTagSearchScrolling = arrayOf(false)
    val isNoteSearchScrolling = arrayOf(false)
    val isRestoringContentScroll = arrayOf(false)

    var contentFieldValue: TextFieldValue
        get() = contentFieldValueState.value
        set(value) {
            contentFieldValueState.value = value
        }

    var pendingContentScrollAnchor: ContentScrollAnchor?
        get() = pendingContentScrollAnchorState.value
        set(value) {
            pendingContentScrollAnchorState.value = value
        }

    fun markContentEdited() {
        contentEditRevision += 1
        hasPendingContentCommit = true
    }

    fun markContentCommitted() {
        hasPendingContentCommit = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberEditorScreenState(
    noteId: Long,
    templateId: Long,
    editTemplateId: Long
): EditorScreenState = key(noteId, templateId, editTemplateId) {
    val snackbarHostState = remember { SnackbarHostState() }
    val contentScrollState = rememberScrollState()
    val previewListState = rememberLazyListState()
    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val contentTextFieldState = rememberTextFieldState()
    val contentFieldValueState = remember { mutableStateOf(TextFieldValue("")) }
    val pendingImageInsertionOffsetState = remember { mutableStateOf<Int?>(null) }
    val pendingContentScrollAnchorState = remember { mutableStateOf<ContentScrollAnchor?>(null) }

    remember {
        EditorScreenState(
            snackbarHostState = snackbarHostState,
            contentScrollState = contentScrollState,
            previewListState = previewListState,
            titleFocusRequester = titleFocusRequester,
            contentFocusRequester = contentFocusRequester,
            searchFocusRequester = searchFocusRequester,
            contentTextFieldState = contentTextFieldState,
            initialContentAnimationsEnabled = noteId == EditorViewModel.NO_ID,
            contentFieldValueState = contentFieldValueState,
            pendingImageInsertionOffsetState = pendingImageInsertionOffsetState,
            pendingContentScrollAnchorState = pendingContentScrollAnchorState
        )
    }
}
