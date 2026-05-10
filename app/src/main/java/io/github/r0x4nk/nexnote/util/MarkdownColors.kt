package io.github.r0x4nk.nexnote.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware colors that the Markdown renderer applies to inline elements
 * whose look must follow the active light / dark theme.
 *
 * Bundling them keeps the parser signature stable as new themable surfaces are
 * added (highlight, mention, callout, …) without forcing every entry point to
 * grow another parameter, and gives the parse cache a single hashable value to
 * key on so two notes parsed under opposite themes never collide.
 *
 * The renderer reads these directly inside [SpanStyle][androidx.compose.ui.text.SpanStyle]
 * blocks built off-thread; all instances must therefore be safe to share
 * across threads. [Color] is a value class wrapping a [ULong], so this is
 * naturally true for any value pulled from a Material theme.
 */
@Immutable
data class MarkdownColors(
    val linkColor: Color,
    val inlineCodeBackground: Color,
    val inlineCodeForeground: Color
) {
    companion object {
        /**
         * Neutral fallback used by surfaces that don't have a [MaterialTheme]
         * available — for example plain-text extraction paths and tests.
         *
         * The inline-code colors are chosen so that the resulting span stays
         * legible on both light and dark backgrounds even when the caller does
         * not override them.
         */
        val Unspecified = MarkdownColors(
            linkColor = Color.Unspecified,
            inlineCodeBackground = Color(0x33808080),
            inlineCodeForeground = Color.Unspecified
        )
    }
}
