package io.github.r0x4nk.nexnote.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val TAB_FADE_IN_DURATION_MS = 160
private const val TAB_FADE_OUT_DURATION_MS = 120
private const val BACK_STACK_SLIDE_DURATION_MS = 280
private const val BACK_STACK_FADE_IN_DURATION_MS = 220
private const val BACK_STACK_FADE_OUT_DURATION_MS = 200

internal fun tabEnterTransition(): EnterTransition =
    fadeIn(tween(TAB_FADE_IN_DURATION_MS))

internal fun tabExitTransition(): ExitTransition =
    fadeOut(tween(TAB_FADE_OUT_DURATION_MS))

internal fun forwardEnterTransition(): EnterTransition =
    slideInHorizontally(tween(BACK_STACK_SLIDE_DURATION_MS, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(BACK_STACK_FADE_IN_DURATION_MS))

internal fun backwardEnterTransition(): EnterTransition =
    slideInHorizontally(tween(BACK_STACK_SLIDE_DURATION_MS, easing = FastOutSlowInEasing)) { -it } +
        fadeIn(tween(BACK_STACK_FADE_IN_DURATION_MS))

internal fun forwardExitTransition(): ExitTransition =
    slideOutHorizontally(tween(BACK_STACK_SLIDE_DURATION_MS, easing = FastOutSlowInEasing)) { it } +
        fadeOut(tween(BACK_STACK_FADE_OUT_DURATION_MS))
