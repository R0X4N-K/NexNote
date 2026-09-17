package io.github.r0x4nk.nexnote.ui.common

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.ui.Modifier

/**
 * Placement and fade animation for note cards inside a lazy collection.
 *
 * Reordering a collection (pin, unpin, sort change, folder expansion) glides
 * with a calm spring while add/remove transitions fade asymmetrically. The
 * list's own `clipToBounds` keeps the small spring overshoot from drawing over
 * app chrome such as the top bar or the tag filter row.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
internal fun LazyItemScope.animateNoteItem(): Modifier =
    Modifier.animateItem(
        fadeInSpec = NoteMotion.itemFadeIn,
        placementSpec = NoteMotion.itemPlacementSpring,
        fadeOutSpec = NoteMotion.itemFadeOut
    )

/** Placement-animated modifier for staggered-grid items. */
@SuppressLint("ModifierFactoryExtensionFunction")
internal fun LazyStaggeredGridItemScope.animateNoteItem(): Modifier =
    Modifier.animateItem(
        fadeInSpec = NoteMotion.itemFadeIn,
        placementSpec = NoteMotion.itemPlacementSpring,
        fadeOutSpec = NoteMotion.itemFadeOut
    )
