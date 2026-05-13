package io.github.r0x4nk.nexnote.ui.common

/**
 * Canonical motion timings shared by the editor chrome and note-card dismissal.
 *
 * Keeping these values together makes small timing adjustments deliberate:
 * the editor panels, content mode transition, IME toolbar, and swipe-to-trash
 * collapse should feel related even though they live in different UI packages.
 */
internal object EditorMotion {
    const val PANEL_EXPAND_MS = 190
    const val PANEL_EXPAND_FADE_MS = 140
    const val PANEL_COLLAPSE_MS = 160
    const val PANEL_COLLAPSE_FADE_MS = 110

    const val CONTENT_LOADING_FADE_IN_MS = 120
    const val CONTENT_LOADING_FADE_OUT_MS = 90
    const val CONTENT_MODE_SLIDE_MS = 220
    const val CONTENT_MODE_FADE_IN_MS = 180
    const val CONTENT_MODE_FADE_OUT_MS = 160

    const val IME_TOOLBAR_ENTER_MS = 220
    const val IME_TOOLBAR_ENTER_FADE_MS = 180
    const val IME_TOOLBAR_EXIT_MS = 180
    const val IME_TOOLBAR_EXIT_FADE_MS = 140

    const val NOTE_CARD_EXIT_SHRINK_MS = 250
    const val NOTE_CARD_EXIT_FADE_MS = 200
    const val NOTE_CARD_TRASH_DELAY_MS = 280L
}
