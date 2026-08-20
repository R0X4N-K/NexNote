package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

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
