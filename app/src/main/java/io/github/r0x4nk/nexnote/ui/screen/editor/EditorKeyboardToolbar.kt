package io.github.r0x4nk.nexnote.ui.screen.editor

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.r0x4nk.nexnote.ui.common.EditorMotion

/**
 * Base container alpha for the IME toolbar.
 *
 * The toolbar stays slightly translucent for scroll context, but opaque enough
 * to avoid noisy text popping through the controls.
 */
private const val EditorToolbarSurfaceAlpha = 0.94f

/**
 * Compact vertical padding keeps the rectangle visually attached to the IME
 * instead of turning it into a floating panel.
 */
private val EditorToolbarVerticalPadding = 0.dp

/**
 * Horizontal content padding only affects the tool row; the container itself
 * still spans the full screen width.
 */
private val EditorToolbarHorizontalPadding = 8.dp

private val EditorToolbarHistoryGap = 4.dp

internal val EditorKeyboardToolbarMinHeight = 44.dp

private val EditorToolbarButtonSize = EditorKeyboardToolbarMinHeight

private val EditorToolbarIconSize = 21.dp

private val EditorToolbarHeadingTextSize = 17.sp

/**
 * Compact editor actions anchored above the IME while editing.
 *
 * The toolbar keeps editing-history controls pinned to the left while
 * Markdown formatting tools scroll independently on the right.
 *
 * The link-type and heading-level choosers are exposed via the
 * [linkMenuExpanded]/[onLinkMenuExpandedChange] and
 * [headingMenuExpanded]/[onHeadingMenuExpandedChange] pairs so the parent can
 * keep the toolbar mounted while either dropdown is open. This prevents the
 * dropdowns' focusable popups — which collapse the IME for a moment — from
 * also tearing the toolbar (and the menu itself) down before the user can
 * pick an option.
 *
 * The button order follows research-based priorities for mobile Markdown
 * note-taking: bold → heading → bullets → checkbox → link → italic → numbered
 * → inline code → code block → quote → image → horizontal rule → strikethrough.
 */
@Composable
internal fun EditorKeyboardToolbar(
    visible: Boolean,
    isTemplateMode: Boolean,
    canInsertImages: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    linkMenuExpanded: Boolean,
    onLinkMenuExpandedChange: (Boolean) -> Unit,
    headingMenuExpanded: Boolean,
    onHeadingMenuExpandedChange: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertImage: () -> Unit,
    onInsertChecklist: () -> Unit,
    onSetHeadingLevel: (Int) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleInlineCode: () -> Unit,
    onInsertCodeBlock: () -> Unit,
    onToggleQuote: () -> Unit,
    onToggleUnorderedList: () -> Unit,
    onToggleOrderedList: () -> Unit,
    onInsertHorizontalRule: () -> Unit,
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit,
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

        // A plain translucent surface keeps the toolbar calm and avoids extra
        // decorative color around the outer edges.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> onHeightChangedState.value(size.height) },
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = EditorToolbarSurfaceAlpha
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = EditorToolbarHorizontalPadding,
                        vertical = EditorToolbarVerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorToolbarHistoryActions(
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo
                )
                Spacer(Modifier.width(EditorToolbarHistoryGap))
                EditorToolbarScrollableActions(
                    isTemplateMode = isTemplateMode,
                    canInsertImages = canInsertImages,
                    linkMenuExpanded = linkMenuExpanded,
                    onLinkMenuExpandedChange = onLinkMenuExpandedChange,
                    headingMenuExpanded = headingMenuExpanded,
                    onHeadingMenuExpandedChange = onHeadingMenuExpandedChange,
                    onInsertImage = onInsertImage,
                    onInsertChecklist = onInsertChecklist,
                    onSetHeadingLevel = onSetHeadingLevel,
                    onToggleBold = onToggleBold,
                    onToggleItalic = onToggleItalic,
                    onToggleStrikethrough = onToggleStrikethrough,
                    onToggleInlineCode = onToggleInlineCode,
                    onInsertCodeBlock = onInsertCodeBlock,
                    onToggleQuote = onToggleQuote,
                    onToggleUnorderedList = onToggleUnorderedList,
                    onToggleOrderedList = onToggleOrderedList,
                    onInsertHorizontalRule = onInsertHorizontalRule,
                    onInsertWebLink = onInsertWebLink,
                    onInsertNoteLink = onInsertNoteLink,
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
            contentDescription = "Undo",
            enabled = canUndo
        )
        EditorToolbarIcon(
            onClick = onRedo,
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            enabled = canRedo
        )
    }
}

/**
 * Scrollable Markdown tool strip. History controls are deliberately kept out
 * of this row so they remain pinned to the left edge.
 */
@Composable
private fun EditorToolbarScrollableActions(
    isTemplateMode: Boolean,
    canInsertImages: Boolean,
    linkMenuExpanded: Boolean,
    onLinkMenuExpandedChange: (Boolean) -> Unit,
    headingMenuExpanded: Boolean,
    onHeadingMenuExpandedChange: (Boolean) -> Unit,
    onInsertImage: () -> Unit,
    onInsertChecklist: () -> Unit,
    onSetHeadingLevel: (Int) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleInlineCode: () -> Unit,
    onInsertCodeBlock: () -> Unit,
    onToggleQuote: () -> Unit,
    onToggleUnorderedList: () -> Unit,
    onToggleOrderedList: () -> Unit,
    onInsertHorizontalRule: () -> Unit,
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Order chosen from research: high-frequency actions first.
        EditorToolbarIcon(onToggleBold, Icons.Default.FormatBold, "Bold")
        EditorHeadingMenu(
            expanded = headingMenuExpanded,
            onExpandedChange = onHeadingMenuExpandedChange,
            onSelectLevel = onSetHeadingLevel
        )
        EditorToolbarIcon(
            onClick = onToggleUnorderedList,
            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
            contentDescription = "Bulleted list"
        )
        EditorToolbarIcon(onInsertChecklist, Icons.Default.CheckBox, "Checklist")
        EditorLinkMenu(
            expanded = linkMenuExpanded,
            onExpandedChange = onLinkMenuExpandedChange,
            onInsertWebLink = onInsertWebLink,
            onInsertNoteLink = onInsertNoteLink
        )
        EditorToolbarIcon(onToggleItalic, Icons.Default.FormatItalic, "Italic")
        EditorToolbarIcon(
            onClick = onToggleOrderedList,
            imageVector = Icons.Default.FormatListNumbered,
            contentDescription = "Numbered list"
        )
        EditorToolbarIcon(onToggleInlineCode, Icons.Default.Code, "Inline code")
        EditorToolbarIcon(onInsertCodeBlock, Icons.Default.DataObject, "Code block")
        EditorToolbarIcon(onToggleQuote, Icons.Default.FormatQuote, "Quote")
        if (!isTemplateMode && canInsertImages) {
            EditorToolbarIcon(onInsertImage, Icons.Default.Image, "Insert image")
        }
        EditorToolbarIcon(onInsertHorizontalRule, Icons.Default.HorizontalRule, "Horizontal rule")
        EditorToolbarIcon(onToggleStrikethrough, Icons.Default.FormatStrikethrough, "Strikethrough")
    }
}

/**
 * Inline link-type chooser anchored to the toolbar's link button.
 *
 * State is fully hoisted: the parent owns [expanded] so it can keep the toolbar
 * (and therefore this dropdown anchor) composed even when the IME briefly collapses
 * after the focusable popup grabs window focus.
 */
@Composable
private fun EditorLinkMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onInsertWebLink: () -> Unit,
    onInsertNoteLink: () -> Unit
) {
    Box {
        EditorToolbarIcon(
            onClick = { onExpandedChange(true) },
            imageVector = Icons.Default.Link,
            contentDescription = "Insert link",
            selected = expanded
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Web link") },
                onClick = {
                    onExpandedChange(false)
                    onInsertWebLink()
                }
            )
            DropdownMenuItem(
                text = { Text("Note link") },
                onClick = {
                    onExpandedChange(false)
                    onInsertNoteLink()
                }
            )
        }
    }
}

/**
 * Heading-level picker that replaces the previous opaque "T" / Title icon.
 *
 * The trigger uses a textual "H" (with a small caret) so the affordance is
 * immediately readable on the toolbar — the previous icon was easy to mistake
 * for plain text formatting. Tapping opens a menu offering H1 through H6 so
 * users can pick a specific level instead of cycling through them blindly.
 */
@Composable
private fun EditorHeadingMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectLevel: (Int) -> Unit
) {
    Box {
        HeadingTriggerButton(
            expanded = expanded,
            onClick = { onExpandedChange(true) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            HeadingLevels.forEach { level ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "H$level",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = headingPreviewSize(level)
                            )
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onSelectLevel(level)
                    }
                )
            }
        }
    }
}

private val HeadingLevels: IntRange = 1..6

private fun headingPreviewSize(level: Int) = when (level) {
    1 -> 22.sp
    2 -> 19.sp
    3 -> 17.sp
    4 -> 16.sp
    5 -> 15.sp
    else -> 14.sp
}

/**
 * Custom textual trigger for the heading menu. Renders a stylized "H" so the
 * affordance is unambiguous even at a glance.
 */
@Composable
private fun HeadingTriggerButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (expanded) colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (expanded) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(EditorToolbarButtonSize)
                .clip(CircleShape)
                .background(containerColor)
        ) {
            Box(
                modifier = Modifier.size(EditorToolbarIconSize),
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
        !enabled -> colorScheme.onSurface.copy(alpha = 0.24f)
        selected -> colorScheme.onPrimaryContainer
        else -> colorScheme.onSurfaceVariant
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(EditorToolbarButtonSize)
                .clip(CircleShape)
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
