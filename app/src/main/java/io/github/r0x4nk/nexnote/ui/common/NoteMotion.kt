package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Shared motion for note collections and their cards.
 *
 * Centralising these specs keeps list reordering, card state changes and
 * dismissals feeling like one system. The placement spring deliberately settles
 * with a slight bounce so a pin, unpin or sort change reads as movement instead
 * of a jump, while the asymmetric fades make insertions and removals feel
 * intentional and calm.
 */
internal object NoteMotion {
    const val ITEM_FADE_IN_MS = 220
    const val ITEM_FADE_OUT_MS = 180
    const val CARD_STATE_MS = 200
    const val SELECTION_MS = 150

    val cardStateEasing: Easing = FastOutSlowInEasing

    val itemPlacementSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val itemFadeIn = tween<Float>(
        durationMillis = ITEM_FADE_IN_MS,
        easing = LinearOutSlowInEasing
    )

    val itemFadeOut = tween<Float>(
        durationMillis = ITEM_FADE_OUT_MS,
        easing = FastOutLinearInEasing
    )
}
