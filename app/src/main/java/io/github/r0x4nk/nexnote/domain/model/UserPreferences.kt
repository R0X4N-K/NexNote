package io.github.r0x4nk.nexnote.domain.model

enum class ThemeMode { LIGHT, DARK, SYSTEM, TRUE_DARK }

enum class FontScale(val multiplier: Float) {
    SMALL(0.85f), NORMAL(1.0f), LARGE(1.15f)
}

/** Predefined accent color options for the app theme. */
enum class AccentColor { VIOLET, BLUE, GREEN, ORANGE, RED, TEAL }

/** Controls how much information is shown on each note card in list/grid views. */
enum class NoteCardStyle {
    /** Title and date only, the most compact option. */
    TITLE_ONLY,
    /** Title plus up to two lines of content preview (default). */
    TITLE_AND_PREVIEW,
    /** Title plus last-modified date prominently displayed. */
    TITLE_DATE
}
