package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.max

internal const val HOME_VAULT_PULL_IDLE_TEXT = "Pull to unlock Vault"
internal const val HOME_VAULT_PULL_READY_TEXT = "Release to unlock Vault"

private const val PULL_RESISTANCE_STRENGTH = 0.55f
private const val RESET_ANIMATION_MS = 180

private val VaultPullThreshold = 88.dp
private val VaultPullMaxReveal = 132.dp

internal data class HomeVaultPullIndicatorState(
    val progress: Float,
    val thresholdReached: Boolean,
    val text: String
)

internal fun homeVaultPullIndicatorState(
    pullOffsetPx: Float,
    thresholdPx: Float
): HomeVaultPullIndicatorState {
    val progress = if (thresholdPx <= 0f) {
        1f
    } else {
        (pullOffsetPx / thresholdPx).coerceIn(0f, 1f)
    }
    val thresholdReached = progress >= 1f

    return HomeVaultPullIndicatorState(
        progress = progress,
        thresholdReached = thresholdReached,
        text = if (thresholdReached) HOME_VAULT_PULL_READY_TEXT else HOME_VAULT_PULL_IDLE_TEXT
    )
}

internal fun calculateHomeVaultPullOffset(
    currentOffsetPx: Float,
    deltaPx: Float,
    maxOffsetPx: Float
): Float {
    if (maxOffsetPx <= 0f) return 0f

    if (deltaPx <= 0f) {
        return (currentOffsetPx + deltaPx).coerceIn(0f, maxOffsetPx)
    }

    val progress = (currentOffsetPx / maxOffsetPx).coerceIn(0f, 1f)
    val resistance = 1f - progress * PULL_RESISTANCE_STRENGTH
    return (currentOffsetPx + deltaPx * resistance).coerceIn(0f, maxOffsetPx)
}

internal fun shouldOpenVaultOnPullRelease(
    pullOffsetPx: Float,
    thresholdPx: Float
): Boolean = thresholdPx > 0f && pullOffsetPx >= thresholdPx

internal fun Modifier.homeVaultPullAccess(
    state: HomeVaultPullGestureState
): Modifier = nestedScroll(state.nestedScrollConnection)

@Composable
internal fun rememberHomeVaultPullGestureState(
    enabled: Boolean,
    onOpenVault: () -> Unit
): HomeVaultPullGestureState {
    val density = LocalDensity.current
    val enabledState = rememberUpdatedState(enabled)
    val onOpenVaultState = rememberUpdatedState(onOpenVault)
    val state = remember {
        HomeVaultPullGestureState(
            isEnabled = { enabledState.value },
            onOpenVault = { onOpenVaultState.value() }
        )
    }

    val thresholdPx = with(density) { VaultPullThreshold.toPx() }
    val maxRevealPx = with(density) { VaultPullMaxReveal.toPx() }
    SideEffect {
        state.updateBounds(
            thresholdPx = thresholdPx,
            maxOffsetPx = maxRevealPx
        )
    }

    return state
}

@Stable
internal class HomeVaultPullGestureState(
    private val isEnabled: () -> Boolean,
    private val onOpenVault: () -> Unit
) {
    private var thresholdPx: Float = 1f
    private var maxOffsetPx: Float = 1f
    private var isReleasing: Boolean = false

    var pullOffsetPx by mutableFloatStateOf(0f)
        private set

    val indicatorState: HomeVaultPullIndicatorState
        get() = homeVaultPullIndicatorState(
            pullOffsetPx = pullOffsetPx,
            thresholdPx = thresholdPx
        )

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!canHandleUserScroll(source)) return Offset.Zero

            return if (available.y < 0f && pullOffsetPx > 0f) {
                Offset(x = 0f, y = consumePullDelta(available.y))
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (!canHandleUserScroll(source)) return Offset.Zero

            return if (available.y > 0f) {
                Offset(x = 0f, y = consumePullDelta(available.y))
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (pullOffsetPx <= 0f) return Velocity.Zero

            releasePull()
            return Velocity(x = 0f, y = available.y)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (pullOffsetPx <= 0f) return Velocity.Zero

            releasePull()
            return available
        }
    }

    fun updateBounds(
        thresholdPx: Float,
        maxOffsetPx: Float
    ) {
        this.thresholdPx = thresholdPx.coerceAtLeast(1f)
        this.maxOffsetPx = max(maxOffsetPx, this.thresholdPx)
        pullOffsetPx = pullOffsetPx.coerceIn(0f, this.maxOffsetPx)

        if (!isEnabled()) {
            pullOffsetPx = 0f
        }
    }

    private fun canHandleUserScroll(source: NestedScrollSource): Boolean =
        isEnabled() && source == NestedScrollSource.UserInput

    private fun consumePullDelta(deltaPx: Float): Float {
        val previousOffset = pullOffsetPx
        pullOffsetPx = calculateHomeVaultPullOffset(
            currentOffsetPx = previousOffset,
            deltaPx = deltaPx,
            maxOffsetPx = maxOffsetPx
        )

        return if (deltaPx > 0f) {
            deltaPx
        } else {
            pullOffsetPx - previousOffset
        }
    }

    private suspend fun releasePull() {
        if (isReleasing) return

        isReleasing = true
        try {
            val openVault = isEnabled() && shouldOpenVaultOnPullRelease(
                pullOffsetPx = pullOffsetPx,
                thresholdPx = thresholdPx
            )

            if (openVault) {
                pullOffsetPx = 0f
                onOpenVault()
            } else {
                animateBackToIdle()
            }
        } finally {
            isReleasing = false
        }
    }

    private suspend fun animateBackToIdle() {
        val startOffset = pullOffsetPx
        if (startOffset <= 0f) return

        Animatable(startOffset).animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = RESET_ANIMATION_MS)
        ) {
            pullOffsetPx = value
        }
        pullOffsetPx = 0f
    }
}

@Composable
internal fun HomeVaultPullAccessIndicator(
    state: HomeVaultPullGestureState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val revealHeight = with(density) { state.pullOffsetPx.toDp() }
    val indicatorState = state.indicatorState

    if (revealHeight <= 0.dp) return

    VaultPullAccessIndicatorContent(
        indicatorState = indicatorState,
        height = revealHeight,
        modifier = modifier
    )
}

@Composable
private fun VaultPullAccessIndicatorContent(
    indicatorState: HomeVaultPullIndicatorState,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val emphasized = indicatorState.thresholdReached
    val idleContainerColor = colorScheme.surfaceContainerLow
    val readyContainerColor = colorScheme.primaryContainer.copy(alpha = 0.86f)
    val contentColor by animateColorAsState(
        targetValue = if (emphasized) {
            colorScheme.primary
        } else {
            colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        },
        label = "Vault pull content color"
    )
    val arrowRotation by animateFloatAsState(
        targetValue = if (emphasized) 180f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "Vault pull arrow rotation"
    )
    val contentAlpha = (indicatorState.progress * 1.4f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(idleContainerColor)
    ) {
        VaultPullBackgroundFill(
            progress = indicatorState.progress,
            color = readyContainerColor,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultPullLockIcon(contentColor)
            Spacer(Modifier.width(12.dp))
            Text(
                text = indicatorState.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrowRotation)
            )
        }
    }
}

@Composable
private fun VaultPullBackgroundFill(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(progress.coerceIn(0f, 1f))
            .background(color)
    )
}

@Composable
private fun VaultPullLockIcon(contentColor: Color) {
    Surface(
        shape = CircleShape,
        color = contentColor.copy(alpha = 0.14f),
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
