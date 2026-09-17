package io.github.r0x4nk.nexnote.ui.screen.export

import io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import io.github.r0x4nk.nexnote.util.referencedAttachments
import io.github.r0x4nk.nexnote.util.MarkdownBlock
import io.github.r0x4nk.nexnote.util.MarkdownColors
import io.github.r0x4nk.nexnote.util.MarkdownParser
import io.github.r0x4nk.nexnote.util.copyBounded
import io.github.r0x4nk.nexnote.util.rewriteMappedPaths
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Portable bundles contain only live Markdown references, not files retained solely for undo. */
internal object AttachmentBundleExporter {
    fun referencedFiles(notes: List<Note>): List<NoteAttachment> = notes.flatMap { note ->
        MarkdownParser.parseBlocks(note.content, MarkdownColors.Unspecified).flatMap { block ->
            if (block is MarkdownBlock.ImageBlock) {
                listOf(NoteAttachment(block.path, block.path.substringAfterLast('/')))
            } else block.referencedAttachments()
        }.let { references ->
            val owned = note.imagePaths.toHashSet()
            references.filter { it.path in owned }
        }
    }.distinctBy { it.path }

    fun needsBundle(notes: List<Note>): Boolean = referencedFiles(notes).any { NoteAttachment.isAttachmentPath(it.path) }

    fun write(
        destination: File,
        document: File,
        documentName: String,
        notes: List<Note>,
        fileProvider: (String) -> File,
        checkCancellation: () -> Unit = {}
    ) {
        require(notes.none { it.isInVault }) { "Vault notes cannot be exported" }
        val files = referencedFiles(notes)
        val pathMap = files.mapIndexed { index, attachment ->
            attachment.path to "attachments/${index + 1}_${attachment.exportFileName()}"
        }.toMap()
        try {
            ZipOutputStream(destination.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(documentName))
                if (documentName.endsWith(".md")) {
                    zip.write(document.readText().rewriteMappedPaths(pathMap).toByteArray(Charsets.UTF_8))
                } else document.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                files.forEach { attachment ->
                    checkCancellation()
                    val source = fileProvider(attachment.path)
                    source.inputStream().buffered().use { input ->
                        input.mark(64)
                        val header = ByteArray(64)
                        val count = input.read(header)
                        if (count > 0 && VaultEncryptedFile.isEncoded(header.copyOf(count))) {
                            throw IOException("Encrypted files cannot be exported")
                        }
                        input.reset()
                        zip.putNextEntry(ZipEntry(requireNotNull(pathMap[attachment.path])))
                        copyBounded(input, zip,
                            if (NoteAttachment.isAttachmentPath(attachment.path)) Long.MAX_VALUE else 64L * 1024 * 1024,
                            checkCancellation)
                        zip.closeEntry()
                    }
                }
            }
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    private fun NoteAttachment.exportFileName(): String {
        val suffix = ".$extension"
        val sanitized = displayName.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").trim('.')
        val stem = if (sanitized.endsWith(suffix, ignoreCase = true)) sanitized.dropLast(suffix.length) else sanitized
        return stem.ifBlank { "attachment" }.take(120 - suffix.length) + suffix
    }

}
