package io.github.r0x4nk.nexnote.domain.model

/**
 * Domain model for a note, decoupled from the Room entity.
 * [imagePaths] is the legacy persisted asset manifest: images AND opaque attachments.
 * Paths are relative to filesDir. Keeping one manifest makes Vault, trash and undo
 * retain ownership of every payload without a database or encrypted-field migration.
 * [creationDate] is user-editable; [lastModifiedDate] is managed exclusively by the app.
 */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val isMarkdown: Boolean = true,
    val creationDate: Long = System.currentTimeMillis(),
    val lastModifiedDate: Long = System.currentTimeMillis(),
    val timezone: String = java.util.TimeZone.getDefault().id,
    val isDeleted: Boolean = false,
    val deletedDate: Long? = null,
    val isInVault: Boolean = false,
    val isPinned: Boolean = false,
    val imagePaths: List<String> = emptyList(),
    // Packed ARGB color chosen by the user for this note. NULL = no custom color.
    val backgroundColor: Int? = null,
    // True when the note was last viewed in Markdown preview mode.
    val isPreviewMode: Boolean = false
) {
    val charCount: Int get() = content.length
    val fileSizeBytes: Int get() = content.toByteArray(Charsets.UTF_8).size
}
