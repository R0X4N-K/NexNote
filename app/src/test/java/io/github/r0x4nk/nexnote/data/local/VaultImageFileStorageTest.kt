package io.github.r0x4nk.nexnote.data.local

import io.github.r0x4nk.nexnote.data.security.VaultDecryptionException
import io.github.r0x4nk.nexnote.data.security.VaultEncryptionException
import io.github.r0x4nk.nexnote.data.security.VaultFileCipher
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import java.io.File
import java.io.InputStream
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultImageFileStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fileCipher = VaultFileCipher()
    private val key = SecretKeySpec(
        ByteArray(KEY_SIZE_BYTES) { index -> (index + 1).toByte() },
        KEY_ALGORITHM
    )
    private val differentKey = SecretKeySpec(
        ByteArray(KEY_SIZE_BYTES) { index -> (index + 65).toByte() },
        KEY_ALGORITHM
    )
    private val invalidKey = SecretKeySpec(byteArrayOf(1, 2, 3), KEY_ALGORITHM)

    @Test
    fun `encryptInPlace writes encrypted envelope and decryptToByteArray restores bytes`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_1_img_100.jpg"
        val plainBytes = "RAW-PRIVATE-IMAGE-BYTES".toByteArray(Charsets.UTF_8)
        val file = writeImage(imageStorage, relativePath, plainBytes)

        val result = vaultImageStorage.encryptInPlace(relativePath, key)

        val storedBytes = file.readBytes()
        assertEquals(VaultImageFileEncryptionResult.Encrypted, result)
        assertTrue(fileCipher.isEncryptedPayload(storedBytes))
        assertFalse(String(storedBytes, Charsets.UTF_8).contains("RAW-PRIVATE-IMAGE-BYTES"))
        assertEquals(listOf("note_1_img_100.jpg"), file.parentFile?.list()?.toList())

        val decrypted = vaultImageStorage.decryptToByteArray(relativePath, key)

        assertTrue(decrypted is VaultImageFileDecryptionResult.Decrypted)
        assertArrayEquals(
            plainBytes,
            (decrypted as VaultImageFileDecryptionResult.Decrypted).bytes
        )
    }

    @Test
    fun `decryptInPlace restores encrypted envelope to plaintext file bytes`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_rollback_img_100.jpg"
        val plainBytes = "ROLLBACK-IMAGE-BYTES".toByteArray(Charsets.UTF_8)
        val file = writeImage(imageStorage, relativePath, plainBytes)
        vaultImageStorage.encryptInPlace(relativePath, key)
        assertTrue(fileCipher.isEncryptedPayload(file.readBytes()))

        val result = vaultImageStorage.decryptInPlace(relativePath, key)

        assertEquals(VaultImageFileRestoreResult.Restored, result)
        assertArrayEquals(plainBytes, file.readBytes())
    }

    @Test
    fun `encryptInPlace is idempotent for already encrypted files`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_2_img_100.jpg"
        val encryptedBytes = fileCipher.encryptToByteArray(
            "already encrypted".toByteArray(Charsets.UTF_8),
            key
        )
        val file = writeImage(imageStorage, relativePath, encryptedBytes)

        val result = vaultImageStorage.encryptInPlace(relativePath, key)

        assertEquals(VaultImageFileEncryptionResult.AlreadyEncrypted, result)
        assertArrayEquals(encryptedBytes, file.readBytes())
    }

    @Test
    fun `encryptInPlace and decryptToByteArray report missing files`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/missing.jpg"

        assertEquals(
            VaultImageFileEncryptionResult.Missing,
            vaultImageStorage.encryptInPlace(relativePath, key)
        )
        assertEquals(
            VaultImageFileDecryptionResult.Missing,
            vaultImageStorage.decryptToByteArray(relativePath, key)
        )
    }

    @Test
    fun `encryptInPlace leaves the original file untouched when encryption fails`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_3_img_100.jpg"
        val plainBytes = "plain image".toByteArray(Charsets.UTF_8)
        val file = writeImage(imageStorage, relativePath, plainBytes)

        val result = runCatching {
            vaultImageStorage.encryptInPlace(relativePath, invalidKey)
        }

        assertTrue(result.exceptionOrNull() is VaultEncryptionException)
        assertArrayEquals(plainBytes, file.readBytes())
        assertEquals(listOf("note_3_img_100.jpg"), file.parentFile?.list()?.toList())
    }

    @Test
    fun `decryptToByteArray rejects a different key without rewriting the file`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_4_img_100.jpg"
        val file = writeImage(
            imageStorage,
            relativePath,
            "private image".toByteArray(Charsets.UTF_8)
        )
        vaultImageStorage.encryptInPlace(relativePath, key)
        val encryptedBefore = file.readBytes()

        val result = runCatching {
            vaultImageStorage.decryptToByteArray(relativePath, differentKey)
        }

        assertTrue(result.exceptionOrNull() is VaultDecryptionException)
        assertArrayEquals(encryptedBefore, file.readBytes())
    }

    @Test
    fun `rewrapInPlace keeps encrypted backup and writes ciphertext for new key`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_rewrap.jpg"
        val plainBytes = "REWRAP-PRIVATE-IMAGE".toByteArray(Charsets.UTF_8)
        val file = writeImage(imageStorage, relativePath, plainBytes)
        vaultImageStorage.encryptInPlace(relativePath, key)
        val oldCiphertext = file.readBytes()

        val backup = vaultImageStorage.rewrapInPlace(relativePath, key, differentKey)

        assertTrue(backup != null)
        assertTrue(backup!!.backupFile.isFile)
        assertArrayEquals(oldCiphertext, backup.backupFile.readBytes())
        assertFalse(String(backup.backupFile.readBytes()).contains("REWRAP-PRIVATE-IMAGE"))
        val decrypted = vaultImageStorage.decryptToByteArray(relativePath, differentKey)
        assertArrayEquals(
            plainBytes,
            (decrypted as VaultImageFileDecryptionResult.Decrypted).bytes
        )
        assertTrue(
            runCatching { vaultImageStorage.decryptToByteArray(relativePath, key) }
                .exceptionOrNull() is VaultDecryptionException
        )

        vaultImageStorage.commitRewrap(backup)

        assertFalse(backup.backupFile.exists())
    }

    @Test
    fun `rollbackRewrap restores exact old ciphertext and removes backup on commit`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_rewrap_rollback.jpg"
        val plainBytes = "ROLLBACK-PRIVATE-IMAGE".toByteArray(Charsets.UTF_8)
        val file = writeImage(imageStorage, relativePath, plainBytes)
        vaultImageStorage.encryptInPlace(relativePath, key)
        val oldCiphertext = file.readBytes()
        val backup = vaultImageStorage.rewrapInPlace(relativePath, key, differentKey)!!

        vaultImageStorage.rollbackRewrap(backup)
        vaultImageStorage.commitRewrap(backup)

        assertArrayEquals(oldCiphertext, file.readBytes())
        assertFalse(backup.backupFile.exists())
        val decrypted = vaultImageStorage.decryptToByteArray(relativePath, key)
        assertArrayEquals(
            plainBytes,
            (decrypted as VaultImageFileDecryptionResult.Decrypted).bytes
        )
    }

    @Test
    fun `rewrapInPlace failure leaves old ciphertext and no backup`() = runTest {
        val imageStorage = TestNoteImageStorage(tempFolder.root)
        val vaultImageStorage = VaultImageFileStorage(imageStorage, fileCipher)
        val relativePath = "images/note_rewrap_failure.jpg"
        val file = writeImage(
            imageStorage,
            relativePath,
            "OLD-CIPHERTEXT-MUST-SURVIVE".toByteArray(Charsets.UTF_8)
        )
        vaultImageStorage.encryptInPlace(relativePath, key)
        val oldCiphertext = file.readBytes()

        val result = runCatching {
            vaultImageStorage.rewrapInPlace(relativePath, differentKey, key)
        }

        assertTrue(result.exceptionOrNull() is VaultDecryptionException)
        assertArrayEquals(oldCiphertext, file.readBytes())
        assertTrue(file.parentFile?.listFiles()?.none { it.name.contains(".rekey-old-") } == true)
    }

    private fun writeImage(
        imageStorage: NoteImageStorage,
        relativePath: String,
        bytes: ByteArray
    ): File {
        val file = imageStorage.getImageFile(relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        return file
    }

    private class TestNoteImageStorage(
        private val root: File
    ) : NoteImageStorage {
        override suspend fun copyImageToInternal(
            noteId: Long,
            openInputStream: () -> InputStream?
        ): String = error("Not needed by VaultImageFileStorageTest")

        override suspend fun deleteImage(relativePath: String): Boolean =
            getImageFile(relativePath).delete()

        override fun getImageFile(relativePath: String): File =
            File(root, relativePath)
    }

    companion object {
        private const val KEY_SIZE_BYTES = 32
        private const val KEY_ALGORITHM = "AES"
    }
}
