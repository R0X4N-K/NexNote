package io.github.r0x4nk.nexnote.domain.model

/** Stored color identities shared by the editor and development note generator.
 * Rendering resolves these seeds against the active theme; never persist resolved colors. */
// Ordered by hue. null represents "no custom color" (theme default surface).
internal val NOTE_COLOR_PALETTE: List<Int?> = listOf(
    null,
    0xFFFFCDD2.toInt(), // Soft red
    0xFFFFE0B2.toInt(), // Soft orange
    0xFFFFF9C4.toInt(), // Soft yellow
    0xFFC8E6C9.toInt(), // Soft green
    0xFFBBDEFB.toInt(), // Soft blue
    0xFFE1BEE7.toInt(), // Soft purple
    0xFFD7CCC8.toInt(), // Warm grey
)
