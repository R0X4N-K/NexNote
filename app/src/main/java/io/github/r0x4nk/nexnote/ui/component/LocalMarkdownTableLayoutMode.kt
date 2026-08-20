package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode

/** Supplies the table layout mode to Markdown previews in the current composition. */
internal val LocalMarkdownTableLayoutMode = staticCompositionLocalOf {
    TableLayoutMode.FIT_SCREEN
}
