package io.github.r0x4nk.nexnote.domain.model

import java.util.Locale

/** Metadata is independent of the payload, so future audio/video producers can use the same storage. */
data class NoteAttachment(val path: String, val displayName: String) {
    val extension: String get() = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val kind: AttachmentKind get() = AttachmentKind.fromExtension(extension)

    companion object {
        const val DIRECTORY = "images/attachments"
        private val pathPattern = Regex("images/attachments/note_[0-9]+_[a-f0-9-]+\\.[a-z0-9]{1,12}")
        fun isAttachmentPath(path: String): Boolean = pathPattern.matches(path)
        fun safeExtension(name: String): String = name.substringAfterLast('.', "")
            .lowercase(Locale.ROOT).takeIf { it.matches(Regex("[a-z0-9]{1,12}")) } ?: "bin"
    }
}

enum class AttachmentKind {
    PDF, DOCUMENT, SPREADSHEET, PRESENTATION, AUDIO, VIDEO, ARCHIVE, FILE;

    companion object {
        fun fromExtension(extension: String): AttachmentKind = when (extension.lowercase(Locale.ROOT)) {
            "pdf" -> PDF
            "doc", "docx", "odt", "rtf", "txt", "md" -> DOCUMENT
            "xls", "xlsx", "ods", "csv", "tsv" -> SPREADSHEET
            "ppt", "pptx", "odp" -> PRESENTATION
            "mp3", "m4a", "wav", "ogg", "opus", "flac", "aac" -> AUDIO
            "mp4", "webm", "mkv", "mov" -> VIDEO
            "zip", "7z", "rar", "gz", "tar" -> ARCHIVE
            else -> FILE
        }
    }
}
