package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Runtime geometry for the radial menu overlay.
 *
 * The state is deliberately small and immutable so [RadialMenuOverlay] can
 * close or reopen the menu by replacing the whole value, while pure geometry
 * helpers such as [itemOffset] remain easy to test.
 *
 * @param arcStartDeg Starting angle of the item arc, measured clockwise from 12 o'clock
 *                    (0 = up, 90 = right, 180 = down, 270 = left).
 * @param arcEndDeg   Ending angle of the item arc (same convention).
 *                    When start == 0 and end == 360 the items are spread around the full
 *                    circle — this is the default "no constraint" behaviour.
 */
data class RadialMenuState(
    val isOpen: Boolean = false,
    val center: Offset = Offset.Zero,
    val selectedIndex: Int = -1,
    val arcStartDeg: Float = 0f,
    val arcEndDeg: Float = 360f
)

/**
 * Returns the index of the radial menu item under the current finger position.
 *
 * Items are evenly spaced starting at 12 o'clock, rotating clockwise. Each item
 * is centred in its sector (half-sector shift): with 4 items, item 0 occupies
 * [−45°, +45°] from the top rather than [0°, 90°].
 *
 * [currentIndex] (default −1) enables angular hysteresis: once an item is
 * selected, the finger must travel [HYSTERESIS_DEG] into the neighbouring sector
 * before the selection changes, preventing flickering at boundaries.
 *
 * The neutral zone (distance < [neutralRadiusPx]) returns −1 (no selection).
 */
fun calculateSelectedIndex(
    center: Offset,
    current: Offset,
    itemCount: Int,
    neutralRadiusPx: Float,
    currentIndex: Int = -1
): Int {
    if (itemCount == 0) return -1
    val dx = current.x - center.x
    val dy = current.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance < neutralRadiusPx) return -1

    val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
    val sectorSize = 360.0 / itemCount
    // Half-sector shift so each item is centred in its selection arc.
    val normalized = (angleDeg + 90.0 + sectorSize / 2.0 + 360.0) % 360.0
    val rawIndex = (normalized / sectorSize).toInt() % itemCount

    // Hysteresis: only commit to a new sector after HYSTERESIS_DEG into its arc.
    if (currentIndex >= 0 && rawIndex != currentIndex) {
        val positionInSector = normalized - rawIndex * sectorSize
        if (positionInSector < HYSTERESIS_DEG) return currentIndex
    }

    return rawIndex
}

private const val HYSTERESIS_DEG = 8.0

/**
 * Returns the screen-coordinate position of item [index] relative to [center].
 *
 * Angles use a clockwise convention starting at 12 o'clock:
 *   0° = up, 90° = right, 180° = down, 270° = left.
 *
 * [arcStartDeg] and [arcEndDeg] constrain the distribution arc. Items are placed
 * at equal angular intervals within [arcStartDeg, arcEndDeg]:
 *   - Full circle (defaults 0°–360°): step = 360/itemCount so items never overlap.
 *   - Partial arc: step = arcSpread/(itemCount−1) so endpoints are exactly at the
 *     arc boundaries. A single item is centred in the arc.
 *
 * Default values (arcStartDeg=0, arcEndDeg=360) reproduce the original full-circle
 * behaviour, keeping all existing tests and call sites valid.
 */
fun itemOffset(
    center: Offset,
    index: Int,
    itemCount: Int,
    radiusPx: Float,
    arcStartDeg: Float = 0f,
    arcEndDeg: Float = 360f
): Offset {
    val arcSpread = ((arcEndDeg - arcStartDeg + 360f) % 360f).let {
        if (it < 0.01f) 360f else it   // treat wrap-to-zero as full circle
    }
    val isFullCircle = arcSpread >= 359.9f

    val screenAngleDeg = when {
        itemCount <= 1  -> arcStartDeg + arcSpread / 2f          // centre single item in arc
        isFullCircle    -> arcStartDeg + (arcSpread / itemCount) * index
        else            -> arcStartDeg + (arcSpread / (itemCount - 1)) * index
    }

    // Convert screen angle (0=up, CW) to standard math angle (0=right, CCW in screen space).
    val mathAngleRad = Math.toRadians((screenAngleDeg - 90.0))
    return Offset(
        x = center.x + cos(mathAngleRad).toFloat() * radiusPx,
        y = center.y + sin(mathAngleRad).toFloat() * radiusPx
    )
}
