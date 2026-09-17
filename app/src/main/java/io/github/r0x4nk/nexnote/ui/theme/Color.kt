package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct
import io.github.r0x4nk.nexnote.util.MarkdownColors

/** Stored ARGB values are identities, never rewritten when the theme changes. */
@Immutable
internal data class NoteColors(
    val container: Color,
    val onContainer: Color,
    val onContainerVariant: Color,
    val accent: Color,
    val outline: Color,
    val codeContainer: Color,
    val onCodeContainer: Color
)

private val LocalNoteColors = staticCompositionLocalOf<NoteColors?> { null }
private val LocalNoteBaseColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

/** Shared by cards, previews and parser warmup so their cache keys agree. */
@Composable
internal fun rememberContentMarkdownColors(): MarkdownColors {
    val scheme = MaterialTheme.colorScheme
    val note = LocalNoteColors.current
    return remember(scheme, note) {
        MarkdownColors(
            linkColor = note?.accent ?: scheme.primary,
            inlineCodeBackground = note?.codeContainer ?: scheme.surfaceContainerHigh,
            inlineCodeForeground = note?.onCodeContainer
                ?: readableColor(scheme.onSurface, scheme.surfaceContainerHigh)
        )
    }
}

internal fun resolveNoteColors(storedColor: Int?, scheme: ColorScheme, dark: Boolean): NoteColors {
    val container = if (storedColor == null) scheme.surfaceContainerLow else {
        val source = Hct.fromInt(Blend.harmonize(storedColor, scheme.primary.toArgb()))
        // Tone controls luminance independently from hue: no mixing pastels with black.
        Color(Hct.from(source.hue, source.chroma.coerceAtMost(36.0), if (dark) 24.0 else 94.0).toInt())
    }
    return NoteColors(
        container = container,
        onContainer = readableColor(scheme.onSurface, container, 7.0),
        onContainerVariant = readableColor(scheme.onSurfaceVariant, container),
        accent = readableColor(scheme.primary, container),
        outline = readableColor(scheme.outline, container, 3.0),
        codeContainer = scheme.surfaceContainerHigh,
        onCodeContainer = readableColor(scheme.onSurface, scheme.surfaceContainerHigh)
    )
}

@Composable
internal fun rememberNoteColors(storedColor: Int?): NoteColors {
    val scheme = LocalNoteBaseColorScheme.current ?: MaterialTheme.colorScheme
    val dark = LocalNexNoteDarkTheme.current
    return remember(storedColor, scheme, dark) { resolveNoteColors(storedColor, scheme, dark) }
}

/** WCAG contrast is evaluated on final opaque sRGB colors, not tone differences. */
internal fun contrastRatio(first: Color, second: Color): Double {
    val a = first.luminance().toDouble()
    val b = second.luminance().toDouble()
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}

internal fun readableColor(preferred: Color, background: Color, minimum: Double = 4.5): Color {
    if (contrastRatio(preferred, background) >= minimum) return preferred
    val hct = Hct.fromInt(preferred.toArgb())
    val towardLight = background.luminance() < 0.18f
    for (step in 1..100) {
        val tone = (hct.tone + if (towardLight) step else -step).coerceIn(0.0, 100.0)
        val candidate = Color(Hct.from(hct.hue, hct.chroma, tone).toInt())
        if (contrastRatio(candidate, background) >= minimum) return candidate
    }
    return if (towardLight) Color.White else Color.Black
}

/** Scope all note content, including Markdown and nested controls, to its actual surface. */
@Composable
internal fun NoteContentTheme(colors: NoteColors, content: @Composable () -> Unit) {
    val base = LocalNoteBaseColorScheme.current ?: MaterialTheme.colorScheme
    val scheme = remember(base, colors) {
        base.copy(
            surface = colors.container,
            onSurface = colors.onContainer,
            onSurfaceVariant = colors.onContainerVariant,
            primary = colors.accent,
            onPrimary = readableColor(base.onPrimary, colors.accent),
            outline = colors.outline
        )
    }
    CompositionLocalProvider(LocalNoteColors provides colors, LocalNoteBaseColorScheme provides base) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
