package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.roundToInt

// Distance from the FAB centre to each item centre.
private val RADIUS_DP = 140.dp
private val EXTENDED_RADIUS_DP = 168.dp

// Diameter of the circular icon background.
private val ICON_SIZE_DP = 48.dp
private val EXTENDED_ICON_SIZE_DP = 44.dp

// Width of the item Column (icon + label). 80 dp fits icon-only items while
// leaving comfortable visual separation for items at 25°–37° apart on a 140 dp radius.
private val ITEM_WIDTH_DP = 80.dp
private val EXTENDED_ITEM_WIDTH_DP = 64.dp

private const val EXTENDED_LAYOUT_ITEM_THRESHOLD = 5

// Taps within this radius of the FAB centre are treated as dismiss actions.
private val NEUTRAL_RADIUS_DP = 40.dp

// Maximum distance from an item centre at which a tap is attributed to that
// item. A generous value (70 dp) ensures any tap within the arc region
// activates the nearest item rather than falling through to dismiss.
private val MAX_HIT_RADIUS_DP = 70.dp

/**
 * Full-screen radial menu overlay.
 *
 * Items are activated by tapping anywhere near their position. A single
 * [pointerInput] gesture handler covers the entire screen and maps each tap to
 * the nearest item by Euclidean distance, completely eliminating overlapping
 * rectangular hit areas. Tapping near the centre or far from all items calls
 * [onDismiss].
 *
 * @param state       Current open/centre state (arc angles, FAB position).
 * @param items       Items to render around the centre point.
 * @param onItemClick Invoked with the tapped item index.
 * @param onDismiss   Invoked when the user taps outside any item.
 */
@Composable
fun RadialMenu(
    state: RadialMenuState,
    items: List<RadialMenuItem>,
    onItemClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen || items.isEmpty()) return

    val density = LocalDensity.current

    // Scale from 0 → 1 on open for a pop-in entrance animation.
    var targetScale by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) { targetScale = 1f }
    val animScale by animateFloatAsState(
        targetValue   = targetScale,
        animationSpec = tween(durationMillis = 150),
        label         = "radialMenuScale"
    )

    val useExtendedLayout = items.size >= EXTENDED_LAYOUT_ITEM_THRESHOLD
    val radiusDp          = if (useExtendedLayout) EXTENDED_RADIUS_DP else RADIUS_DP
    val iconSizeDp        = if (useExtendedLayout) EXTENDED_ICON_SIZE_DP else ICON_SIZE_DP
    val itemWidthDp       = if (useExtendedLayout) EXTENDED_ITEM_WIDTH_DP else ITEM_WIDTH_DP

    val radiusPx        = with(density) { radiusDp.toPx() }
    val iconSizePx      = with(density) { iconSizeDp.toPx() }
    val itemWidthPx     = with(density) { itemWidthDp.toPx() }
    val neutralRadiusPx = with(density) { NEUTRAL_RADIUS_DP.toPx() }
    val maxHitRadiusPx  = with(density) { MAX_HIT_RADIUS_DP.toPx() }

    val surface   = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(modifier.fillMaxSize()) {

        // ── Scrim ─────────────────────────────────────────────────────────────
        // A semi-transparent overlay dims the content behind the menu.
        // The previous dashed guide circle is intentionally removed: it was a
        // legacy visual from when the menu could open at any screen position.
        // The menu is now corner-anchored and the scrim alone provides
        // sufficient visual context.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.30f * animScale))
        }

        // ── Single gesture handler covering the full screen ───────────────────
        // Maps each tap to the nearest item via findItemHit().
        // One detector instead of per-item clickable modifiers completely
        // eliminates overlapping rectangular hit areas and ensures every tap
        // in the arc region activates exactly the right item.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state, items) {
                    detectTapGestures { tapOffset ->
                        val hitIndex = findItemHit(
                            tap             = tapOffset,
                            center          = state.center,
                            itemCount       = items.size,
                            radiusPx        = radiusPx,
                            arcStartDeg     = state.arcStartDeg,
                            arcEndDeg       = state.arcEndDeg,
                            neutralRadiusPx = neutralRadiusPx,
                            maxHitRadiusPx  = maxHitRadiusPx
                        )
                        if (hitIndex >= 0 && items[hitIndex].enabled) {
                            onItemClick(hitIndex)
                        } else {
                            onDismiss()
                        }
                    }
                }
        )

        // ── Item visuals ──────────────────────────────────────────────────────
        // Rendered on top of the gesture detector for visual layering; touch
        // events are handled entirely by the detector above.
        items.forEachIndexed { index, item ->
            val offset = itemOffset(
                center      = state.center,
                index       = index,
                itemCount   = items.size,
                radiusPx    = radiusPx,
                arcStartDeg = state.arcStartDeg,
                arcEndDeg   = state.arcEndDeg
            )

            // Column centred horizontally on the offset point; the icon circle
            // centre is vertically aligned with that point.
            val enabledAlpha = if (item.enabled) 1f else 0.38f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(itemWidthDp)
                    .offset {
                        IntOffset(
                            x = (offset.x - itemWidthPx / 2).roundToInt(),
                            y = (offset.y - iconSizePx / 2).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        scaleX = animScale
                        scaleY = animScale
                        alpha  = animScale * enabledAlpha
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                            pivotFractionX = 0.5f,
                            pivotFractionY = iconSizePx / 2 / (iconSizePx + 28f)
                        )
                    }
            ) {
                // Icon circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(iconSizeDp)
                        .clip(CircleShape)
                        .background(surface.copy(alpha = 0.92f))
                ) {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.contentDescription.takeIf { it.isNotBlank() },
                        tint               = onSurface,
                        modifier           = Modifier.size(24.dp)
                    )
                }

                // Label chip below the icon — only rendered when the label is
                // non-empty. Omitting it for icon-only items prevents chip
                // overlap when multiple items sit close together on a narrow arc.
                if (item.label.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .background(
                                surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text      = item.label,
                            color     = onSurface,
                            fontSize  = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis,
                            softWrap  = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns the index of the radial menu item under [tap], or −1 (dismiss).
 *
 * A tap within [neutralRadiusPx] of [center] always returns −1 so that touching
 * near the FAB anchor dismisses the menu rather than triggering an item.
 *
 * Outside the neutral zone the function finds the item whose centre position
 * (computed by [itemOffset]) is closest to [tap] by Euclidean distance. The
 * nearest item is returned if that distance is within [maxHitRadiusPx]; otherwise
 * −1 is returned (the tap was too far from all items).
 *
 * Because every screen point is unambiguously attributed to at most one item,
 * this approach has no overlapping hit areas and no missed taps at boundaries.
 */
private fun findItemHit(
    tap: Offset,
    center: Offset,
    itemCount: Int,
    radiusPx: Float,
    arcStartDeg: Float,
    arcEndDeg: Float,
    neutralRadiusPx: Float,
    maxHitRadiusPx: Float
): Int {
    if (itemCount == 0) return -1

    val distFromCenter = hypot(tap.x - center.x, tap.y - center.y)
    if (distFromCenter < neutralRadiusPx) return -1

    var nearestIndex = -1
    var nearestDist  = Float.MAX_VALUE

    for (index in 0 until itemCount) {
        val pos  = itemOffset(center, index, itemCount, radiusPx, arcStartDeg, arcEndDeg)
        val dist = hypot(tap.x - pos.x, tap.y - pos.y)
        if (dist < nearestDist) {
            nearestDist  = dist
            nearestIndex = index
        }
    }

    return if (nearestDist <= maxHitRadiusPx) nearestIndex else -1
}
