package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.r0x4nk.nexnote.ui.common.EditorMotion

/**
 * Horizontal inset for the leading/trailing controls. The container itself is
 * edge-to-edge: this only keeps the first and last ripple from touching the
 * rounded screen corners.
 */
private val EditorToolbarHorizontalPadding = 4.dp

internal val EditorKeyboardToolbarMinHeight = 44.dp

private val EditorToolbarButtonSize = EditorKeyboardToolbarMinHeight

private val EditorToolbarIconSize = 21.dp

private val EditorToolbarHeadingTextSize = 17.sp

private val EditorToolbarSeparatorHeight = 22.dp

/**
 * Compact editor actions anchored above the IME while editing.
 *
 * The toolbar is a single, edge-to-edge surface attached to the keyboard —
 * deliberately not a floating panel, because it belongs to the writing surface
 * rather than hovering over it. History controls stay pinned to the left while
 * the Markdown formatting tools scroll independently on the right.
 *
 * The link and heading choosers are hosted as modal bottom sheets by the parent
 * (see [EditorLinkSheet] and [EditorHeadingSheet]) so the toolbar only needs to
 * report which chooser the user opened.
 *
 * The button order follows research-based priorities for mobile Markdown
 * note-taking: bold → heading → bullets → checkbox → link → italic → numbered
 * → inline code → code block → quote → image → horizontal rule → strikethrough.
 */
@Composable
internal fun EditorKeyboardToolbar(
    visible: Boolean,
    isTemplateMode: Boolean,
    canInsertAttachments: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    linkMenuExpanded: Boolean,
    onOpenLinkMenu: () -> Unit,
    headingMenuExpanded: Boolean,
    onOpenHeadingMenu: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertAttachment: () -> Unit,
    onInsertChecklist: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleInlineCode: () -> Unit,
    onInsertCodeBlock: () -> Unit,
    onToggleQuote: () -> Unit,
    onToggleUnorderedList: () -> Unit,
    onToggleOrderedList: () -> Unit,
    onInsertHorizontalRule: () -> Unit,
    modifier: Modifier = Modifier,
    onHeightChanged: (Int) -> Unit = {}
) {
    val onHeightChangedState = rememberUpdatedState(onHeightChanged)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = EditorMotion.IME_TOOLBAR_ENTER_MS, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = EditorMotion.IME_TOOLBAR_ENTER_FADE_MS)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = EditorMotion.IME_TOOLBAR_EXIT_MS, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = EditorMotion.IME_TOOLBAR_EXIT_FADE_MS)),
        modifier = modifier
    ) {
        DisposableEffect(Unit) {
            onDispose { onHeightChangedState.value(0) }
        }

        // A solid, full-width surface keeps the bar visually attached to the
        // keyboard instead of reading as a detached floating panel.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> onHeightChangedState.value(size.height) },
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EditorToolbarHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorToolbarHistoryActions(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo
                )
                EditorToolbarSeparator()
                EditorToolbarScrollableActions(
                    isTemplateMode = isTemplateMode,
                    canInsertAttachments = canInsertAttachments,
                    linkMenuExpanded = linkMenuExpanded,
                    onOpenLinkMenu = onOpenLinkMenu,
                    headingMenuExpanded = headingMenuExpanded,
                    onOpenHeadingMenu = onOpenHeadingMenu,
                    onInsertAttachment = onInsertAttachment,
                    onInsertChecklist = onInsertChecklist,
                    onIndent = onIndent,
                    onOutdent = onOutdent,
                    onToggleBold = onToggleBold,
                    onToggleItalic = onToggleItalic,
                    onToggleStrikethrough = onToggleStrikethrough,
                    onToggleInlineCode = onToggleInlineCode,
                    onInsertCodeBlock = onInsertCodeBlock,
                    onToggleQuote = onToggleQuote,
                    onToggleUnorderedList = onToggleUnorderedList,
                    onToggleOrderedList = onToggleOrderedList,
                    onInsertHorizontalRule = onInsertHorizontalRule,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Fixed editing-history controls. Keeping this row outside the scrollable
 * action strip lets undo and redo stay reachable while formatting tools move.
 */
@Composable
private fun EditorToolbarHistoryActions(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EditorToolbarIcon(
            onClick = onUndo,
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = stringResource(R.string.common_undo),
            enabled = canUndo
        )
        EditorToolbarIcon(
            onClick = onRedo,
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = stringResource(R.string.editor_redo),
            enabled = canRedo
        )
    }
}

/**
 * Thin material separator between the pinned history controls and the
 * scrollable formatting tools. It replaces the previous floating-pill groups.
 */
@Composable
private fun EditorToolbarSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(EditorToolbarSeparatorHeight)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * Scrollable Markdown tool strip. History controls are deliberately kept out
 * of this row so they remain pinned to the left edge.
 */
@Composable
private fun EditorToolbarScrollableActions(
    isTemplateMode: Boolean,
    canInsertAttachments: Boolean,
    linkMenuExpanded: Boolean,
    onOpenLinkMenu: () -> Unit,
    headingMenuExpanded: Boolean,
    onOpenHeadingMenu: () -> Unit,
    onInsertAttachment: () -> Unit,
    onInsertChecklist: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleInlineCode: () -> Unit,
    onInsertCodeBlock: () -> Unit,
    onToggleQuote: () -> Unit,
    onToggleUnorderedList: () -> Unit,
    onToggleOrderedList: () -> Unit,
    onInsertHorizontalRule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Order chosen from research: high-frequency actions first.
        EditorToolbarIcon(onToggleBold, Icons.Default.FormatBold, stringResource(R.string.editor_bold))
        EditorToolbarHeadingButton(
            expanded = headingMenuExpanded,
            onClick = onOpenHeadingMenu
        )
        EditorToolbarIcon(
            onClick = onToggleUnorderedList,
            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
            contentDescription = stringResource(R.string.editor_bulleted_list)
        )
        EditorToolbarIcon(
            onInsertChecklist,
            Icons.Default.CheckBox,
            stringResource(R.string.editor_checklist)
        )
        EditorToolbarIcon(
            onIndent,
            Icons.AutoMirrored.Filled.FormatIndentIncrease,
            stringResource(R.string.editor_indent)
        )
        EditorToolbarIcon(
            onOutdent,
            Icons.AutoMirrored.Filled.FormatIndentDecrease,
            stringResource(R.string.editor_outdent)
        )
        EditorToolbarLinkButton(
            expanded = linkMenuExpanded,
            onClick = onOpenLinkMenu
        )
        EditorToolbarIcon(onToggleItalic, Icons.Default.FormatItalic, stringResource(R.string.editor_italic))
        EditorToolbarIcon(
            onClick = onToggleOrderedList,
            imageVector = Icons.Default.FormatListNumbered,
            contentDescription = stringResource(R.string.editor_numbered_list)
        )
        EditorToolbarIcon(
            onToggleInlineCode,
            Icons.Default.Code,
            stringResource(R.string.editor_inline_code)
        )
        EditorToolbarIcon(
            onInsertCodeBlock,
            Icons.Default.DataObject,
            stringResource(R.string.editor_code_block)
        )
        EditorToolbarIcon(
            onToggleQuote,
            Icons.Default.FormatQuote,
            stringResource(R.string.editor_quote)
        )
        if (!isTemplateMode && canInsertAttachments) {
            EditorToolbarIcon(onInsertAttachment, Icons.Default.AttachFile, stringResource(R.string.attachment_add))
        }
        EditorToolbarIcon(
            onInsertHorizontalRule,
            Icons.Default.HorizontalRule,
            stringResource(R.string.editor_horizontal_rule)
        )
        EditorToolbarIcon(
            onToggleStrikethrough,
            Icons.Default.FormatStrikethrough,
            stringResource(R.string.editor_strikethrough)
        )
    }
}

/** Trigger for the link-type chooser hosted by [EditorLinkSheet]. */
@Composable
private fun EditorToolbarLinkButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    EditorToolbarIcon(
        onClick = onClick,
        imageVector = Icons.Default.Link,
        contentDescription = stringResource(R.string.editor_insert_link),
        selected = expanded
    )
}

/**
 * Trigger for the heading-level chooser hosted by [EditorHeadingSheet].
 *
 * The trigger uses a textual "H" so the affordance is immediately readable on
 * the toolbar — a plain "T" glyph was easy to mistake for text formatting.
 */
@Composable
private fun EditorToolbarHeadingButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (expanded) colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (expanded) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
    val headingDescription = stringResource(R.string.editor_heading_level)
    val expansionDescription = stringResource(if (expanded) R.string.menu_expanded else R.string.menu_collapsed)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(EditorToolbarButtonSize)
                .clip(MaterialTheme.shapes.medium)
                .background(containerColor)
                .semantics {
                    contentDescription = headingDescription
                    stateDescription = expansionDescription
                }
        ) {
            Box(
                modifier = Modifier.size(EditorToolbarButtonSize),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "H",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = EditorToolbarHeadingTextSize,
                        color = contentColor
                    )
                )
            }
        }
    }
}

@Composable
private fun EditorToolbarIcon(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (selected) colorScheme.primaryContainer else Color.Transparent
    val contentColor = when {
        !enabled -> colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> colorScheme.onPrimaryContainer
        else -> colorScheme.onSurfaceVariant
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(EditorToolbarButtonSize)
                .clip(MaterialTheme.shapes.medium)
                .background(containerColor)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(EditorToolbarIconSize)
            )
        }
    }
}
