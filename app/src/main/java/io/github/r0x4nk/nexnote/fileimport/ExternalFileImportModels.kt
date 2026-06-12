package io.github.r0x4nk.nexnote.fileimport

data class ExternalFileOpenRequest(
    val requestId: Long,
    val noteId: Long
)

internal sealed interface ExternalFileImportResult {
    data object Ignored : ExternalFileImportResult
    data class Imported(val noteId: Long) : ExternalFileImportResult
    data class Failed(val message: String) : ExternalFileImportResult
}
