package io.github.r0x4nk.nexnote.ui.screen.editor

/**
 * Returns the stored-file paths owned by a note that [content] no longer
 * references.
 *
 * Membership is decided with a plain substring test: a path that still appears
 * anywhere in the note — link, image, code block or plain text — is treated as
 * referenced. Pruning may therefore retain a payload that is no longer visible
 * as a card, but it never deletes a payload the note still quotes. That
 * conservative bias is deliberate because deletion is irreversible.
 */
internal fun unreferencedStoredPaths(content: String, ownedPaths: List<String>): List<String> =
    ownedPaths.filter { path -> path.isNotBlank() && !content.contains(path) }
