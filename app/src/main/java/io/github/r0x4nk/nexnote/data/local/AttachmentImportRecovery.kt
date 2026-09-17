package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.data.db.NoteDao
import io.github.r0x4nk.nexnote.data.security.VaultFieldCipher
import io.github.r0x4nk.nexnote.data.security.VaultDecryptionException
import java.io.File
import javax.crypto.SecretKey

/** Runs at startup and unlock, never during normal note collection or rendering. */
internal class AttachmentImportRecovery(filesDir: File, private val dao: NoteDao) {
    private val journal = AttachmentImportJournal(filesDir)

    suspend fun run(key: SecretKey? = null) {
        journal.recover { id ->
            val note = dao.getNoteForAttachmentRecovery(id)
            when {
                note == null -> emptySet()
                !note.isInVault -> note.imagePathsRaw.lineSequence().filter(String::isNotBlank).toSet()
                key == null -> null
                else -> try {
                    VaultFieldCipher().decryptToString(note.imagePathsRaw, key)
                        .lineSequence().filter(String::isNotBlank).toSet()
                } catch (_: VaultDecryptionException) { null }
            }
        }
    }
}
