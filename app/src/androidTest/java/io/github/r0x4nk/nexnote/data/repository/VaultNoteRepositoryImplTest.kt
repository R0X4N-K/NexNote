package io.github.r0x4nk.nexnote.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.data.local.InternalNoteImageStorage
import io.github.r0x4nk.nexnote.data.local.VaultImageFileEncryptionResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileStorage
import io.github.r0x4nk.nexnote.data.security.VaultFieldCipher
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.VaultLockedException
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@RunWith(AndroidJUnit4::class)
class VaultNoteRepositoryImplTest {

    private lateinit var db: NexNoteDatabase
    private lateinit var keyProvider: TestVaultKeyProvider
    private lateinit var repository: VaultNoteRepositoryImpl
    private lateinit var imageStorage: NoOpNoteImageStorage
    private val cipher = VaultFieldCipher()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, NexNoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keyProvider = TestVaultKeyProvider()
        imageStorage = NoOpNoteImageStorage()
        repository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = imageStorage,
            fieldCipher = cipher
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveVaultNote_encryptsSensitiveFields_andDecryptsWhenUnlocked() = runTest {
        val note = Note(
            title = "Secret title",
            content = "Secret body",
            imagePaths = listOf("images/private.jpg"),
            creationDate = 1_000L
        )

        val id = repository.saveVaultNote(note)

        val raw = db.noteDao().getVaultNoteById(id)
        assertTrue(raw?.isInVault == true)
        assertNotEquals("Secret title", raw?.title)
        assertNotEquals("Secret body", raw?.content)
        assertNotEquals("images/private.jpg", raw?.imagePathsRaw)
        assertTrue(cipher.isEncryptedPayload(raw?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(raw?.content.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(raw?.imagePathsRaw.orEmpty()))
        assertNull(db.noteDao().getNoteById(id))

        val decrypted = repository.getVaultNoteById(id)
        assertEquals("Secret title", decrypted?.title)
        assertEquals("Secret body", decrypted?.content)
        assertEquals(listOf("images/private.jpg"), decrypted?.imagePaths)
        assertTrue(decrypted?.isInVault == true)
    }

    @Test
    fun vaultNotes_returnsEmptyWhenLocked() = runTest {
        val id = repository.saveVaultNote(Note(title = "Hidden", content = "Body"))
        assertEquals(listOf(id), repository.vaultNotes.first().map { it.id })

        keyProvider.lock()

        assertTrue(repository.vaultNotes.first().isEmpty())
        assertNull(repository.getVaultNoteById(id))
    }

    @Test
    fun saveVaultNote_failsWhenLocked() = runTest {
        keyProvider.lock()

        val result = runCatching {
            repository.saveVaultNote(Note(title = "Nope"))
        }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        assertTrue(db.noteDao().getAllVaultNotes().first().isEmpty())
    }

    @Test
    fun moveNormalNoteToVault_encryptsNoteAndRemovesNormalTagIndex() = runTest {
        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Visible title",
                content = "Body with #secret",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )
        db.tagDao().insertTag(TagEntity(name = "secret", createdDate = 1_000L, lastUpdatedDate = 1_000L))
        db.tagDao().insertCrossRef(NoteTagCrossRef(noteId = noteId, tagName = "secret"))

        val moved = repository.moveNormalNoteToVault(noteId)

        assertEquals(MoveNoteToVaultResult.Success, moved)
        assertNull(db.noteDao().getNoteById(noteId))
        assertTrue(db.tagDao().getCrossRefsForNote(noteId).isEmpty())

        val raw = db.noteDao().getVaultNoteById(noteId)
        assertTrue(raw?.isInVault == true)
        assertNotEquals("Visible title", raw?.title)
        assertNotEquals("Body with #secret", raw?.content)
        assertTrue(cipher.isEncryptedPayload(raw?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(raw?.content.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(raw?.imagePathsRaw.orEmpty()))

        val decrypted = repository.getVaultNoteById(noteId)
        assertEquals("Visible title", decrypted?.title)
        assertEquals("Body with #secret", decrypted?.content)
        assertTrue(decrypted?.imagePaths?.isEmpty() == true)
    }

    @Test
    fun moveNormalNoteToVault_encryptsPhysicalImageFilesInPlaceAndMovesNote() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-move-auto-encrypt").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_150.jpg"
            val plainBytes = byteArrayOf(42, 41, 40, 39, 38)
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(plainBytes)

            val noteId = db.noteDao().insertNote(
                NoteEntity(
                    title = "Visible image title",
                    content = "Body with #image",
                    imagePathsRaw = relativePath,
                    creationDate = 1_000L,
                    lastModifiedDate = 1_000L
                )
            )
            db.tagDao().insertTag(
                TagEntity(name = "image", createdDate = 1_000L, lastUpdatedDate = 1_000L)
            )
            db.tagDao().insertCrossRef(NoteTagCrossRef(noteId = noteId, tagName = "image"))

            val moved = physicalRepository.moveNormalNoteToVault(noteId)

            assertEquals(MoveNoteToVaultResult.Success, moved)
            assertNull(db.noteDao().getNoteById(noteId))
            assertTrue(db.tagDao().getCrossRefsForNote(noteId).isEmpty())

            val raw = db.noteDao().getVaultNoteById(noteId)
            assertTrue(raw?.isInVault == true)
            assertNotEquals("Visible image title", raw?.title)
            assertNotEquals("Body with #image", raw?.content)
            assertNotEquals(relativePath, raw?.imagePathsRaw)
            assertTrue(cipher.isEncryptedPayload(raw?.title.orEmpty()))
            assertTrue(cipher.isEncryptedPayload(raw?.content.orEmpty()))
            assertTrue(cipher.isEncryptedPayload(raw?.imagePathsRaw.orEmpty()))

            val protectedBytes = imageFile.readBytes()
            assertFalse(protectedBytes.contentEquals(plainBytes))
            assertTrue(
                "Encrypted image bytes should match Vault file envelope.",
                io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile.isEncoded(protectedBytes)
            )
            assertTrue(
                physicalRepository.decryptVaultImageBytes(relativePath)
                    ?.contentEquals(plainBytes) == true
            )

            val decrypted = physicalRepository.getVaultNoteById(noteId)
            assertEquals("Visible image title", decrypted?.title)
            assertEquals("Body with #image", decrypted?.content)
            assertEquals(listOf(relativePath), decrypted?.imagePaths)
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun moveNormalNoteToVault_rollsBackImageEncryptionWhenMoveFails() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-move-rollback").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val firstPath = "images/note_1_img_rollback_first.jpg"
        val secondPath = "images/note_1_img_rollback_second.jpg"
        val failingImageStorage = FailingVaultImageFileStorage(
            imageStorage = realImageStorage,
            failPath = secondPath
        )
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher,
            vaultImageFileStorage = failingImageStorage
        )

        try {
            val firstPlainBytes = byteArrayOf(1, 3, 5, 7)
            val secondPlainBytes = byteArrayOf(2, 4, 6, 8)
            val firstFile = realImageStorage.getImageFile(firstPath)
            val secondFile = realImageStorage.getImageFile(secondPath)
            firstFile.parentFile?.mkdirs()
            secondFile.parentFile?.mkdirs()
            firstFile.writeBytes(firstPlainBytes)
            secondFile.writeBytes(secondPlainBytes)

            val noteId = db.noteDao().insertNote(
                NoteEntity(
                    title = "Visible rollback title",
                    content = "Body with #rollback",
                    imagePathsRaw = listOf(firstPath, secondPath).joinToString("\n"),
                    creationDate = 1_000L,
                    lastModifiedDate = 1_000L
                )
            )
            db.tagDao().insertTag(
                TagEntity(name = "rollback", createdDate = 1_000L, lastUpdatedDate = 1_000L)
            )
            db.tagDao().insertCrossRef(NoteTagCrossRef(noteId = noteId, tagName = "rollback"))

            val result = runCatching { physicalRepository.moveNormalNoteToVault(noteId) }

            assertTrue(result.exceptionOrNull() is IOException)
            val normal = db.noteDao().getNoteById(noteId)
            assertEquals("Visible rollback title", normal?.title)
            assertEquals("Body with #rollback", normal?.content)
            assertEquals(listOf(firstPath, secondPath).joinToString("\n"), normal?.imagePathsRaw)
            assertFalse(normal?.isInVault == true)
            assertNull(db.noteDao().getVaultNoteById(noteId))
            assertEquals(
                listOf("rollback"),
                db.tagDao().getCrossRefsForNote(noteId).map { it.tagName }
            )
            assertTrue(firstFile.readBytes().contentEquals(firstPlainBytes))
            assertTrue(secondFile.readBytes().contentEquals(secondPlainBytes))
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun moveNormalNoteToVault_requiresUnlockedVault() = runTest {
        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Still normal",
                content = "Still visible",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )
        keyProvider.lock()

        val result = runCatching { repository.moveNormalNoteToVault(noteId) }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        val normal = db.noteDao().getNoteById(noteId)
        assertEquals("Still normal", normal?.title)
        assertFalse(normal?.isInVault == true)
        assertNull(db.noteDao().getVaultNoteById(noteId))
    }

    @Test
    fun moveNormalNoteToVault_ignoresDeletedNotes() = runTest {
        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Deleted",
                content = "Not movable from normal list",
                isDeleted = true,
                deletedDate = 2_000L,
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        val moved = repository.moveNormalNoteToVault(noteId)

        assertEquals(MoveNoteToVaultResult.NotFound, moved)
        assertTrue(db.noteDao().getNoteById(noteId)?.isDeleted == true)
        assertNull(db.noteDao().getVaultNoteById(noteId))
    }

    @Test
    fun removeNoteFromVault_decryptsNoteAndRestoresNormalTagIndex() = runTest {
        val noteId = repository.saveVaultNote(
            Note(
                title = "Restored title",
                content = "Body with #restored",
                imagePaths = listOf("images/private.jpg"),
                creationDate = 1_000L,
                isPinned = true,
                backgroundColor = 0x11223344,
                isPreviewMode = true
            )
        )

        val removed = repository.removeNoteFromVault(noteId)

        assertTrue(removed)
        assertNull(db.noteDao().getVaultNoteById(noteId))
        assertTrue(repository.vaultNotes.first().isEmpty())

        val normal = db.noteDao().getNoteById(noteId)
        assertEquals("Restored title", normal?.title)
        assertEquals("Body with #restored", normal?.content)
        assertEquals("images/private.jpg", normal?.imagePathsRaw)
        assertFalse(normal?.isInVault == true)
        assertTrue(normal?.isPinned == true)
        assertEquals(0x11223344, normal?.backgroundColor)
        assertTrue(normal?.isPreviewMode == true)
        assertFalse(cipher.isEncryptedPayload(normal?.title.orEmpty()))
        assertFalse(cipher.isEncryptedPayload(normal?.content.orEmpty()))
        assertFalse(cipher.isEncryptedPayload(normal?.imagePathsRaw.orEmpty()))
        assertEquals(listOf(noteId), db.noteDao().getAllNotes().first().map { it.id })
        assertEquals(
            listOf("restored"),
            db.tagDao().getCrossRefsForNote(noteId).map { it.tagName }
        )
    }

    @Test
    fun removeNoteFromVault_restoresPhysicalImageFilesToPlaintext() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-remove-restore").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_remove.jpg"
            val plainBytes = byteArrayOf(90, 91, 92, 93)
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(plainBytes)

            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "Vault image title",
                    content = "Body with #image",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )
            assertTrue(
                "Vault save must encrypt the image file before removal.",
                io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile.isEncoded(
                    imageFile.readBytes()
                )
            )

            val removed = physicalRepository.removeNoteFromVault(noteId)

            assertTrue(removed)
            assertNull(db.noteDao().getVaultNoteById(noteId))
            val normal = db.noteDao().getNoteById(noteId)
            assertEquals("Vault image title", normal?.title)
            assertEquals("Body with #image", normal?.content)
            assertEquals(relativePath, normal?.imagePathsRaw)
            assertFalse(normal?.isInVault == true)
            assertTrue(imageFile.readBytes().contentEquals(plainBytes))
            assertFalse(
                "A normal note image file must not remain wrapped as a Vault file.",
                io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile.isEncoded(
                    imageFile.readBytes()
                )
            )
            assertEquals(
                listOf("image"),
                db.tagDao().getCrossRefsForNote(noteId).map { it.tagName }
            )
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun removeNoteFromVault_requiresUnlockedVault() = runTest {
        val noteId = repository.saveVaultNote(Note(title = "Locked", content = "Hidden"))
        val encryptedBefore = db.noteDao().getVaultNoteById(noteId)
        keyProvider.lock()

        val result = runCatching { repository.removeNoteFromVault(noteId) }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        assertNull(db.noteDao().getNoteById(noteId))
        val encryptedAfter = db.noteDao().getVaultNoteById(noteId)
        assertEquals(encryptedBefore?.title, encryptedAfter?.title)
        assertEquals(encryptedBefore?.content, encryptedAfter?.content)
        assertTrue(cipher.isEncryptedPayload(encryptedAfter?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(encryptedAfter?.content.orEmpty()))
    }

    @Test
    fun moveVaultNoteToTrash_softDeletesVaultNoteWithoutExposingItInNormalTrash() = runTest {
        val noteId = repository.saveVaultNote(
            Note(
                title = "Private trash title",
                content = "Private trash body",
                imagePaths = listOf("images/private-trash.jpg"),
                creationDate = 1_000L
            )
        )
        val encryptedBefore = db.noteDao().getVaultNoteById(noteId)!!

        val moved = repository.moveVaultNoteToTrash(noteId)

        assertTrue(moved)
        assertNull(db.noteDao().getVaultNoteById(noteId))
        assertTrue(repository.vaultNotes.first().isEmpty())
        assertTrue(db.noteDao().getDeletedNotes().first().isEmpty())
        assertNull(db.noteDao().getNoteById(noteId))

        val trashedVault = db.noteDao().getAllVaultNotesForWipeOnce().single()
        assertEquals(noteId, trashedVault.id)
        assertTrue(trashedVault.isInVault)
        assertTrue(trashedVault.isDeleted)
        assertTrue(trashedVault.deletedDate != null)
        assertEquals(encryptedBefore.title, trashedVault.title)
        assertEquals(encryptedBefore.content, trashedVault.content)
        assertEquals(encryptedBefore.imagePathsRaw, trashedVault.imagePathsRaw)
        assertTrue(cipher.isEncryptedPayload(trashedVault.title))
        assertTrue(cipher.isEncryptedPayload(trashedVault.content))
        assertTrue(cipher.isEncryptedPayload(trashedVault.imagePathsRaw))
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun moveVaultNoteToTrash_requiresUnlockedVault() = runTest {
        val noteId = repository.saveVaultNote(Note(title = "Locked trash", content = "Hidden"))
        val encryptedBefore = db.noteDao().getVaultNoteById(noteId)
        keyProvider.lock()

        val result = runCatching { repository.moveVaultNoteToTrash(noteId) }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        val encryptedAfter = db.noteDao().getVaultNoteById(noteId)
        assertEquals(encryptedBefore?.title, encryptedAfter?.title)
        assertFalse(encryptedAfter?.isDeleted == true)
        assertNull(encryptedAfter?.deletedDate)
        assertTrue(db.noteDao().getDeletedNotes().first().isEmpty())
    }

    @Test
    fun moveVaultNoteToTrash_ignoresNormalNotes() = runTest {
        val normalId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal title",
                content = "Normal body",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        val moved = repository.moveVaultNoteToTrash(normalId)

        assertFalse(moved)
        val normal = db.noteDao().getNoteById(normalId)
        assertEquals("Normal title", normal?.title)
        assertFalse(normal?.isDeleted == true)
        assertTrue(db.noteDao().getAllVaultNotesForWipeOnce().isEmpty())
    }

    @Test
    fun rewrapAllVaultNotesWith_reencryptsEveryActiveVaultNoteUsingNewKey() = runTest {
        val firstId = repository.saveVaultNote(
            Note(
                title = "First title",
                content = "First body",
                imagePaths = listOf("images/first.jpg"),
                creationDate = 1_000L
            )
        )
        val secondId = repository.saveVaultNote(
            Note(
                title = "Second title",
                content = "Second body",
                imagePaths = listOf("images/second.jpg"),
                creationDate = 2_000L
            )
        )
        val firstBefore = db.noteDao().getVaultNoteById(firstId)
        val secondBefore = db.noteDao().getVaultNoteById(secondId)
        val firstModifiedBefore = firstBefore?.lastModifiedDate
        val secondModifiedBefore = secondBefore?.lastModifiedDate
        val newKey: SecretKey = SecretKeySpec(ByteArray(32) { idx -> (-(idx + 1)).toByte() }, "AES")

        repository.rewrapAllVaultNotesWith(newKey)

        val firstAfter = db.noteDao().getVaultNoteById(firstId)
        val secondAfter = db.noteDao().getVaultNoteById(secondId)
        assertNotEquals(firstBefore?.title, firstAfter?.title)
        assertNotEquals(firstBefore?.content, firstAfter?.content)
        assertNotEquals(firstBefore?.imagePathsRaw, firstAfter?.imagePathsRaw)
        assertNotEquals(secondBefore?.title, secondAfter?.title)
        assertNotEquals(secondBefore?.content, secondAfter?.content)
        assertNotEquals(secondBefore?.imagePathsRaw, secondAfter?.imagePathsRaw)
        assertTrue(cipher.isEncryptedPayload(firstAfter?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(firstAfter?.content.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(firstAfter?.imagePathsRaw.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(secondAfter?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(secondAfter?.content.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(secondAfter?.imagePathsRaw.orEmpty()))
        assertEquals(firstModifiedBefore, firstAfter?.lastModifiedDate)
        assertEquals(secondModifiedBefore, secondAfter?.lastModifiedDate)

        assertEquals("First title", cipher.decryptToString(firstAfter!!.title, newKey))
        assertEquals("First body", cipher.decryptToString(firstAfter.content, newKey))
        assertEquals("Second title", cipher.decryptToString(secondAfter!!.title, newKey))
        assertEquals("Second body", cipher.decryptToString(secondAfter.content, newKey))

        keyProvider.replaceKey(newKey)
        assertEquals(
            listOf("First title", "Second title").sorted(),
            repository.vaultNotes.first().map { it.title }.sorted()
        )
    }

    @Test
    fun rewrapAllVaultNotesWith_failsWhenLocked() = runTest {
        val noteId = repository.saveVaultNote(
            Note(title = "Locked title", content = "Locked body", creationDate = 1_000L)
        )
        val before = db.noteDao().getVaultNoteById(noteId)
        keyProvider.lock()
        val newKey: SecretKey = SecretKeySpec(ByteArray(32) { idx -> (idx + 7).toByte() }, "AES")

        val result = runCatching { repository.rewrapAllVaultNotesWith(newKey) }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        val after = db.noteDao().getVaultNoteById(noteId)
        assertEquals(before?.title, after?.title)
        assertEquals(before?.content, after?.content)
        assertEquals(before?.imagePathsRaw, after?.imagePathsRaw)
    }

    @Test
    fun rewrapAllVaultNotesWith_leavesNormalNotesUntouched() = runTest {
        val vaultId = repository.saveVaultNote(
            Note(title = "Vault title", content = "Vault body", creationDate = 1_000L)
        )
        val normalId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal title",
                content = "Normal body",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )
        val normalBefore = db.noteDao().getNoteById(normalId)
        val newKey: SecretKey = SecretKeySpec(ByteArray(32) { idx -> (idx + 13).toByte() }, "AES")

        repository.rewrapAllVaultNotesWith(newKey)

        val normalAfter = db.noteDao().getNoteById(normalId)
        assertEquals("Normal title", normalAfter?.title)
        assertEquals("Normal body", normalAfter?.content)
        assertEquals(normalBefore?.lastModifiedDate, normalAfter?.lastModifiedDate)
        val activeVaultIds = db.noteDao().getAllVaultNotes().first().map { it.id }
        assertEquals(listOf(vaultId), activeVaultIds)
    }

    @Test
    fun wipeAllVaultNotes_hardDeletesActiveAndTrashedVaultEntries_andImagesWhenUnlocked() = runTest {
        val activeVaultId = repository.saveVaultNote(
            Note(
                title = "Active vault",
                content = "Active body",
                imagePaths = listOf("images/active.jpg"),
                creationDate = 1_000L
            )
        )
        val trashedVaultId = repository.saveVaultNote(
            Note(
                title = "Trashed vault",
                content = "Trashed body",
                imagePaths = listOf("images/trashed.jpg"),
                creationDate = 2_000L
            )
        )
        // Soft-delete the second Vault note directly via DAO to simulate trashed
        // Vault entries: the wiper must clear them too.
        val trashedEntity = db.noteDao().getVaultNoteById(trashedVaultId)!!
        db.noteDao().updateNote(
            trashedEntity.copy(isDeleted = true, deletedDate = 3_000L)
        )

        val removed = repository.wipeAllVaultNotes()

        assertEquals(2, removed)
        assertEquals(
            listOf("images/active.jpg", "images/trashed.jpg"),
            imageStorage.deletedPaths.sorted()
        )
        assertNull(db.noteDao().getVaultNoteById(activeVaultId))
        assertEquals(emptyList<Long>(), db.noteDao().getAllVaultNotes().first().map { it.id })
        assertTrue(db.noteDao().getAllVaultNotesOnce().isEmpty())
        // Even after restoring the soft-deleted row's view, no Vault entries remain.
        assertNull(db.noteDao().getNoteById(activeVaultId))
        assertNull(db.noteDao().getNoteById(trashedVaultId))
    }

    @Test
    fun wipeAllVaultNotes_deletesPhysicalImageFileForVaultNote() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-physical-wipe").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_100.jpg"
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(byteArrayOf(1, 2, 3, 4))

            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "Vault with image",
                    content = "Body",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )

            assertTrue(imageFile.exists())
            assertNotEquals(relativePath, db.noteDao().getVaultNoteById(noteId)?.imagePathsRaw)

            val removed = physicalRepository.wipeAllVaultNotes()

            assertEquals(1, removed)
            assertFalse(imageFile.exists())
            assertNull(db.noteDao().getVaultNoteById(noteId))
            assertNull(db.noteDao().getNoteById(noteId))
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun wipeAllVaultNotes_requiresUnlockedVault() = runTest {
        repository.saveVaultNote(
            Note(
                title = "Locked vault",
                content = "Locked body",
                imagePaths = listOf("images/locked.jpg"),
                creationDate = 1_000L
            )
        )
        keyProvider.lock()

        val result = runCatching { repository.wipeAllVaultNotes() }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        assertTrue(db.noteDao().getAllVaultNotesOnce().isNotEmpty())
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun wipeAllVaultNotes_leavesNormalNotesUntouched() = runTest {
        val vaultId = repository.saveVaultNote(
            Note(title = "Vault title", content = "Vault body", creationDate = 1_000L)
        )
        val normalActiveId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal active",
                content = "Visible",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )
        db.noteDao().insertNote(
            NoteEntity(
                title = "Normal trashed",
                content = "Trashed visible",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L,
                isDeleted = true,
                deletedDate = 1_500L
            )
        )

        val removed = repository.wipeAllVaultNotes()

        assertEquals(1, removed)
        assertNull(db.noteDao().getVaultNoteById(vaultId))
        assertEquals("Normal active", db.noteDao().getNoteById(normalActiveId)?.title)
        // Trashed normal notes are not exposed by the Vault flow but must
        // still appear in the trash flow.
        val trashedTitles = db.noteDao().getDeletedNotes().first().map { it.title }
        assertTrue(trashedTitles.contains("Normal trashed"))
        assertFalse(trashedTitles.contains("Vault title"))
    }

    @Test
    fun wipeAllVaultNotes_returnsZeroWhenNoVaultEntriesExist() = runTest {
        db.noteDao().insertNote(
            NoteEntity(
                title = "Only normal",
                content = "Body",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        val removed = repository.wipeAllVaultNotes()

        assertEquals(0, removed)
        assertEquals(1, db.noteDao().getAllNotes().first().size)
    }

    @Test
    fun removeNoteFromVault_ignoresNormalNotes() = runTest {
        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Already normal",
                content = "Visible",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        val removed = repository.removeNoteFromVault(noteId)

        assertFalse(removed)
        assertEquals("Already normal", db.noteDao().getNoteById(noteId)?.title)
        assertNull(db.noteDao().getVaultNoteById(noteId))
    }

    @Test
    fun encryptImagesForVaultNote_encryptsPhysicalFilesInPlaceForActiveVaultNote() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-image-encrypt").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_200.jpg"
            val plainBytes = byteArrayOf(9, 8, 7, 6, 5)
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(plainBytes)

            val noteId = db.noteDao().insertNote(
                NoteEntity(
                    title = cipher.encryptToString("Vault with image", testVaultKey()),
                    content = cipher.encryptToString("Body", testVaultKey()),
                    imagePathsRaw = cipher.encryptToString(relativePath, testVaultKey()),
                    isInVault = true,
                    creationDate = 1_000L,
                    lastModifiedDate = 1_000L
                )
            )

            assertTrue(imageFile.readBytes().contentEquals(plainBytes))

            val result = physicalRepository.encryptImagesForVaultNote(noteId)

            assertEquals(VaultNoteImageEncryptionResult.Success, result)
            val protectedBytes = imageFile.readBytes()
            assertFalse(protectedBytes.contentEquals(plainBytes))
            assertTrue(
                "Encrypted image bytes should match Vault file envelope.",
                io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile.isEncoded(protectedBytes)
            )
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun encryptImagesForVaultNote_isIdempotentOnAlreadyEncryptedFiles() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-image-encrypt-idempotent").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_300.jpg"
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(byteArrayOf(1, 2, 3, 4))

            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "Vault with image",
                    content = "Body",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )

            val firstResult = physicalRepository.encryptImagesForVaultNote(noteId)
            val protectedBytesAfterFirst = imageFile.readBytes()
            assertEquals(VaultNoteImageEncryptionResult.Success, firstResult)

            val secondResult = physicalRepository.encryptImagesForVaultNote(noteId)
            val protectedBytesAfterSecond = imageFile.readBytes()
            assertEquals(VaultNoteImageEncryptionResult.Success, secondResult)
            assertTrue(
                "Second pass must not re-encrypt an already encrypted file.",
                protectedBytesAfterFirst.contentEquals(protectedBytesAfterSecond)
            )
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun encryptImagesForVaultNote_returnsNoImagesForVaultNoteWithoutImages() = runTest {
        val noteId = repository.saveVaultNote(
            Note(title = "Only text", content = "No images", creationDate = 1_000L)
        )

        val result = repository.encryptImagesForVaultNote(noteId)

        assertEquals(VaultNoteImageEncryptionResult.NoImages, result)
    }

    @Test
    fun encryptImagesForVaultNote_returnsNoteNotFoundForUnknownId() = runTest {
        val result = repository.encryptImagesForVaultNote(noteId = 999L)

        assertEquals(VaultNoteImageEncryptionResult.NoteNotFound, result)
    }

    @Test
    fun encryptImagesForVaultNote_doesNotMatchNormalNotes() = runTest {
        val normalId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal title",
                content = "Normal body",
                imagePathsRaw = "images/visible.jpg",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        val result = repository.encryptImagesForVaultNote(normalId)

        assertEquals(VaultNoteImageEncryptionResult.NoteNotFound, result)
        // Normal note row must remain untouched.
        val normal = db.noteDao().getNoteById(normalId)
        assertEquals("Normal title", normal?.title)
        assertEquals("images/visible.jpg", normal?.imagePathsRaw)
    }

    @Test
    fun encryptImagesForVaultNote_requiresUnlockedVault() = runTest {
        val noteId = repository.saveVaultNote(
            Note(title = "Locked", content = "Hidden", creationDate = 1_000L)
        )
        keyProvider.lock()

        val outcome = runCatching { repository.encryptImagesForVaultNote(noteId) }

        assertTrue(outcome.exceptionOrNull() is VaultLockedException)
    }

    @Test
    fun saveVaultNote_encryptsPhysicalImageFilesInPlaceForNewNote() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-save-auto-encrypt").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_400.jpg"
            val plainBytes = byteArrayOf(11, 22, 33, 44, 55)
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(plainBytes)

            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "Auto-encrypt title",
                    content = "Auto-encrypt body",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )

            val protectedBytes = imageFile.readBytes()
            assertFalse(protectedBytes.contentEquals(plainBytes))
            assertTrue(
                "Encrypted image bytes should match Vault file envelope.",
                io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile.isEncoded(protectedBytes)
            )
            // The stored image-path list is still recoverable through the
            // normal Vault decryption path and points to the same relative
            // file location.
            val decrypted = physicalRepository.getVaultNoteById(noteId)
            assertEquals(listOf(relativePath), decrypted?.imagePaths)
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun saveVaultNote_isIdempotentOnAlreadyEncryptedImageFiles() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-save-idempotent").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val relativePath = "images/note_1_img_500.jpg"
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(byteArrayOf(7, 7, 7, 7))

            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "Editable",
                    content = "First save",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )
            val encryptedAfterFirstSave = imageFile.readBytes()
            assertTrue(
                "First save must wrap the image file in a Vault envelope.",
                io.github.r0x4nk.nexnote.data.security.VaultEncryptedFile.isEncoded(
                    encryptedAfterFirstSave
                )
            )

            // Simulate an editor update that re-saves the same note with the
            // same image path: the file must not be re-encrypted.
            physicalRepository.saveVaultNote(
                Note(
                    id = noteId,
                    title = "Editable",
                    content = "Second save",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )

            val encryptedAfterSecondSave = imageFile.readBytes()
            assertTrue(
                "Second save must keep the existing envelope bytes unchanged.",
                encryptedAfterFirstSave.contentEquals(encryptedAfterSecondSave)
            )
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun saveVaultNote_doesNotTouchFilesystemForNotesWithoutImages() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-save-no-images").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "No images",
                    content = "Body",
                    creationDate = 1_000L
                )
            )

            // Saving a Vault note without image paths must not produce any
            // file under the image root.
            val filesUnderRoot = imageRoot.walkTopDown().filter { it.isFile }.toList()
            assertTrue(filesUnderRoot.isEmpty())
            assertEquals(
                "No images",
                physicalRepository.getVaultNoteById(noteId)?.title
            )
        } finally {
            imageRoot.deleteRecursively()
        }
    }
}

private fun testVaultKey(): SecretKey =
    SecretKeySpec(ByteArray(32) { index ->
        (index + 1).toByte()
    }, "AES")

private class FailingVaultImageFileStorage(
    imageStorage: NoteImageStorage,
    private val failPath: String
) : VaultImageFileStorage(imageStorage = imageStorage) {
    override suspend fun encryptInPlace(
        relativePath: String,
        key: SecretKey
    ): VaultImageFileEncryptionResult {
        if (relativePath == failPath) {
            throw IOException("Forced Vault image encryption failure.")
        }
        return super.encryptInPlace(relativePath, key)
    }
}

private class TestVaultKeyProvider : VaultUnlockedKeyProvider {
    private var key: SecretKey? = testVaultKey()

    fun lock() {
        key = null
    }

    fun replaceKey(newKey: SecretKey) {
        key = newKey
    }

    override suspend fun <T> withUnlockedVaultKey(block: suspend (SecretKey) -> T): T? {
        val unlockedKey = key ?: return null
        return block(unlockedKey)
    }
}
