package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.domain.model.NoteAttachment
import java.io.File
import java.io.IOException
import java.util.UUID

/** Small durable markers identify only imports, so recovery never scans document contents. */
internal class AttachmentImportJournal(
    private val filesDir: File,
    private val session: String = PROCESS_SESSION
) {
    private val directory get() = File(filesDir, "attachment-imports")

    fun begin(path: String): File {
        require(NoteAttachment.isAttachmentPath(path))
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Cannot create import journal")
        }
        val marker = File(directory, "$session--${path.substringAfterLast('/')}")
        marker.outputStream().use { it.fd.sync() }
        return marker
    }

    /**
     * null means the manifest cannot currently be read (locked/corrupt Vault): retain the file.
     * Current-session imports are never recovered, including files awaiting an editor save retry.
     * A failed cleanup retains its marker so the next startup retries it.
     */
    suspend fun recover(ownedPaths: suspend (Long) -> Set<String>?) {
        val manifests = mutableMapOf<Long, Set<String>?>()
        directory.listFiles()?.forEach { marker ->
            if (marker.name.startsWith("$session--")) return@forEach
            val name = marker.name.substringAfter("--", "")
            val path = "${NoteAttachment.DIRECTORY}/$name"
            if (!NoteAttachment.isAttachmentPath(path)) return@forEach
            val owner = name.removePrefix("note_").substringBefore('_').toLongOrNull() ?: return@forEach
            val target = File(filesDir, path)
            val partial = File(target.parentFile, "${target.name}.part")
            if (partial.exists() && !partial.delete()) return@forEach
            if (!target.exists()) {
                marker.delete()
                return@forEach
            }
            if (!manifests.containsKey(owner)) manifests[owner] = ownedPaths(owner)
            val owned = manifests[owner] ?: return@forEach
            if (path in owned || target.delete()) marker.delete()
        }
    }

    companion object {
        private val PROCESS_SESSION = UUID.randomUUID().toString()
    }
}
