package io.github.r0x4nk.nexnote.domain.model

enum class ThemeMode { LIGHT, DARK, SYSTEM, TRUE_DARK }

enum class FontScale(val multiplier: Float) {
    SMALL(0.85f), NORMAL(1.0f), LARGE(1.15f)
}

/**
 * Typeface applied to the whole application typography.
 *
 * [SYSTEM] keeps the platform default (Roboto), while the other entries map to
 * bundled OFL font families resolved in the UI layer.
 */
enum class AppFont {
    SYSTEM,
    INTER,
    LORA,
    FIRA_CODE,
    JETBRAINS_MONO
}

/** Determines whether Markdown tables fit the viewport or retain readable column widths. */
enum class TableLayoutMode {
    FIT_SCREEN,
    HORIZONTAL_SCROLL
}

/** Predefined accent color options for the app theme. */
enum class AccentColor {
    VIOLET,
    BLUE,
    GREEN,
    ORANGE,
    RED,
    TEAL,
    SAGE,
    ROSE,
    AMBER
}

/** Controls how much information is shown on each note card in list/grid views. */
enum class NoteCardStyle {
    /** Title only, the most compact option. */
    TITLE_ONLY,
    /** Title plus up to three lines of content preview (default). */
    TITLE_AND_PREVIEW,
    /** Title, content preview, and the note metadata: date, tags, files and image count. */
    TITLE_INFORMATION
}

/** Configurable timeout options for Vault auto-lock on resume. */
enum class VaultAutoLockTimeout(val durationMillis: Long?) {
    IMMEDIATELY(0L),
    AFTER_1_MINUTE(60_000L),
    AFTER_5_MINUTES(5 * 60_000L),
    AFTER_15_MINUTES(15 * 60_000L),
    AFTER_30_MINUTES(30 * 60_000L),
    NEVER(null)
}
