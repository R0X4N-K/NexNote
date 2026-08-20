package io.github.r0x4nk.nexnote.data.repository

import androidx.room.Room
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.r0x4nk.nexnote.data.db.NexNoteDatabase
import io.github.r0x4nk.nexnote.data.db.entity.NoteEntity
import io.github.r0x4nk.nexnote.data.db.entity.NoteTagCrossRef
import io.github.r0x4nk.nexnote.data.db.entity.TagEntity
import io.github.r0x4nk.nexnote.data.local.InternalNoteImageStorage
import io.github.r0x4nk.nexnote.data.local.VaultImageFileEncryptionResult
import io.github.r0x4nk.nexnote.data.local.VaultImageFileRewrapBackup
import io.github.r0x4nk.nexnote.data.local.VaultImageFileStorage
import io.github.r0x4nk.nexnote.data.security.VaultFieldCipher
import io.github.r0x4nk.nexnote.data.security.VaultKeyDeriver
import io.github.r0x4nk.nexnote.data.security.VaultPinHasher
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.DuplicateVaultNoteResult
import io.github.r0x4nk.nexnote.domain.repository.MoveNoteToVaultResult
import io.github.r0x4nk.nexnote.domain.repository.NoteImageStorage
import io.github.r0x4nk.nexnote.domain.repository.VaultLockedException
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.testing.NoOpNoteImageStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
    fun duplicateVaultNote_createsEncryptedActiveCopyWithoutNormalTagIndex() = runTest {
        val sourceId = repository.saveVaultNote(
            Note(
                title = "Secret title",
                content = "Secret body with #vaulttag",
                creationDate = 1_000L,
                isPinned = true,
                backgroundColor = 0xFF112233.toInt(),
                isPreviewMode = true
            )
        )

        val result = repository.duplicateVaultNote(sourceId)

        assertTrue(result is DuplicateVaultNoteResult.Success)
        val duplicateId = (result as DuplicateVaultNoteResult.Success).noteId
        assertNotEquals(sourceId, duplicateId)
        assertNull(db.noteDao().getNoteById(duplicateId))
        assertTrue(db.tagDao().getCrossRefsForNote(duplicateId).isEmpty())

        val raw = db.noteDao().getVaultNoteById(duplicateId)
        assertTrue(raw?.isInVault == true)
        assertNotEquals("Secret title", raw?.title)
        assertNotEquals("Secret body with #vaulttag", raw?.content)
        assertTrue(cipher.isEncryptedPayload(raw?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(raw?.content.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(raw?.imagePathsRaw.orEmpty()))

        val duplicate = repository.getVaultNoteById(duplicateId)
        assertEquals("Secret title", duplicate?.title)
        assertEquals("Secret body with #vaulttag", duplicate?.content)
        assertEquals(1_000L, duplicate?.creationDate)
        assertTrue(duplicate?.isPinned == true)
        assertTrue(duplicate?.isPreviewMode == true)
        assertEquals(0xFF112233.toInt(), duplicate?.backgroundColor)
        assertTrue(repository.getVaultNoteById(sourceId)?.isInVault == true)
    }

    @Test
    fun vaultNoteLinkCandidates_areVaultScopedAndEmptyWhenLocked() = runTest {
        val vaultId = repository.saveVaultNote(
            Note(title = "Private link target", content = "Private body")
        )
        val trashedVaultId = repository.saveVaultNote(
            Note(title = "Trashed private target", content = "Trashed body")
        )
        repository.moveVaultNoteToTrash(trashedVaultId)
        db.noteDao().insertNote(
            NoteEntity(
                title = "Normal outside target",
                content = "Visible body",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L
            )
        )

        assertEquals(
            listOf(vaultId to "Private link target"),
            repository.vaultNoteLinkCandidates.first().map { it.id to it.title }
        )

        keyProvider.lock()

        assertTrue(repository.vaultNoteLinkCandidates.first().isEmpty())
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
        val realImageStorage = InternalNoteImageStorage(
            filesDir = imageRoot,
            processImage = { inputStreamProvider, destination ->
                val input = inputStreamProvider() ?: throw IOException("Missing source")
                input.use { source ->
                    destination.outputStream().use { output -> source.copyTo(output) }
                }
            }
        )
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
        val realImageStorage = InternalNoteImageStorage(
            filesDir = imageRoot,
            processImage = { inputStreamProvider, destination ->
                val input = inputStreamProvider() ?: throw IOException("Missing source")
                input.use { source ->
                    destination.outputStream().use { output -> source.copyTo(output) }
                }
            }
        )
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
        val realImageStorage = InternalNoteImageStorage(
            filesDir = imageRoot,
            processImage = { inputStreamProvider, destination ->
                val input = inputStreamProvider() ?: throw IOException("Missing source")
                input.use { source ->
                    destination.outputStream().use { output -> source.copyTo(output) }
                }
            }
        )
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
    fun restoreVaultNoteFromTrash_restoresVaultNoteWithoutExposingItInNormalLists() = runTest {
        val noteId = repository.saveVaultNote(
            Note(
                title = "Restored private title",
                content = "Restored private body",
                imagePaths = listOf("images/restored-private.jpg"),
                creationDate = 1_000L,
                isPinned = true,
                backgroundColor = 0x11223344,
                isPreviewMode = true
            )
        )
        assertTrue(repository.moveVaultNoteToTrash(noteId))
        val trashedVault = db.noteDao().getAllVaultNotesForWipeOnce().single()
        assertTrue(trashedVault.isDeleted)

        val restored = repository.restoreVaultNoteFromTrash(noteId)

        assertTrue(restored)
        assertTrue(repository.vaultTrashedNotes.first().isEmpty())
        assertTrue(db.noteDao().getDeletedNotes().first().isEmpty())
        assertNull(db.noteDao().getNoteById(noteId))

        val activeRaw = db.noteDao().getVaultNoteById(noteId)
        assertEquals(trashedVault.title, activeRaw?.title)
        assertEquals(trashedVault.content, activeRaw?.content)
        assertEquals(trashedVault.imagePathsRaw, activeRaw?.imagePathsRaw)
        assertFalse(activeRaw?.isDeleted == true)
        assertNull(activeRaw?.deletedDate)
        assertTrue(cipher.isEncryptedPayload(activeRaw?.title.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(activeRaw?.content.orEmpty()))
        assertTrue(cipher.isEncryptedPayload(activeRaw?.imagePathsRaw.orEmpty()))

        val decrypted = repository.getVaultNoteById(noteId)
        assertEquals("Restored private title", decrypted?.title)
        assertEquals("Restored private body", decrypted?.content)
        assertEquals(listOf("images/restored-private.jpg"), decrypted?.imagePaths)
        assertTrue(decrypted?.isInVault == true)
        assertFalse(decrypted?.isDeleted == true)
        assertTrue(decrypted?.isPinned == true)
        assertEquals(0x11223344, decrypted?.backgroundColor)
        assertTrue(decrypted?.isPreviewMode == true)
    }

    @Test
    fun restoreVaultNoteFromTrash_requiresUnlockedVault() = runTest {
        val noteId = repository.saveVaultNote(
            Note(title = "Locked restore", content = "Hidden", creationDate = 1_000L)
        )
        assertTrue(repository.moveVaultNoteToTrash(noteId))
        val encryptedBefore = db.noteDao().getAllVaultNotesForWipeOnce().single()
        keyProvider.lock()

        val result = runCatching { repository.restoreVaultNoteFromTrash(noteId) }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        val encryptedAfter = db.noteDao().getAllVaultNotesForWipeOnce().single()
        assertEquals(encryptedBefore.title, encryptedAfter.title)
        assertEquals(encryptedBefore.content, encryptedAfter.content)
        assertTrue(encryptedAfter.isDeleted)
        assertEquals(encryptedBefore.deletedDate, encryptedAfter.deletedDate)
        assertNull(db.noteDao().getVaultNoteById(noteId))
        assertTrue(db.noteDao().getDeletedNotes().first().isEmpty())
    }

    @Test
    fun restoreVaultNoteFromTrash_ignoresNormalTrashedNotes() = runTest {
        val normalId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal trashed title",
                content = "Visible trashed body",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L,
                isDeleted = true,
                deletedDate = 2_000L
            )
        )

        val restored = repository.restoreVaultNoteFromTrash(normalId)

        assertFalse(restored)
        val normal = db.noteDao().getNoteById(normalId)
        assertEquals("Normal trashed title", normal?.title)
        assertTrue(normal?.isDeleted == true)
        assertEquals(2_000L, normal?.deletedDate)
        assertEquals(
            listOf(normalId),
            db.noteDao().getDeletedNotes().first().map { it.id }
        )
        assertTrue(db.noteDao().getAllVaultNotesForWipeOnce().isEmpty())
    }

    @Test
    fun deleteVaultNotePermanently_hardDeletesTrashedVaultNoteAndImagesWhenUnlocked() = runTest {
        val noteId = repository.saveVaultNote(
            Note(
                title = "Delete private title",
                content = "Delete private body",
                imagePaths = listOf(
                    "images/private-delete.jpg",
                    "images/private-delete.jpg",
                    "images/private-delete-2.jpg"
                ),
                creationDate = 1_000L
            )
        )
        assertTrue(repository.moveVaultNoteToTrash(noteId))

        val deleted = repository.deleteVaultNotePermanently(noteId)

        assertTrue(deleted)
        assertNull(db.noteDao().getDeletedVaultNoteById(noteId))
        assertNull(db.noteDao().getVaultNoteById(noteId))
        assertNull(db.noteDao().getNoteById(noteId))
        assertTrue(repository.vaultTrashedNotes.first().isEmpty())
        assertTrue(db.noteDao().getDeletedNotes().first().isEmpty())
        assertEquals(
            listOf("images/private-delete-2.jpg", "images/private-delete.jpg"),
            imageStorage.deletedPaths.sorted()
        )
    }

    @Test
    fun deleteVaultNotePermanently_requiresUnlockedVault() = runTest {
        val noteId = repository.saveVaultNote(
            Note(
                title = "Locked permanent delete",
                content = "Hidden",
                imagePaths = listOf("images/locked-delete.jpg"),
                creationDate = 1_000L
            )
        )
        assertTrue(repository.moveVaultNoteToTrash(noteId))
        val encryptedBefore = db.noteDao().getDeletedVaultNoteById(noteId)
        keyProvider.lock()

        val result = runCatching { repository.deleteVaultNotePermanently(noteId) }

        assertTrue(result.exceptionOrNull() is VaultLockedException)
        val encryptedAfter = db.noteDao().getDeletedVaultNoteById(noteId)
        assertEquals(encryptedBefore?.title, encryptedAfter?.title)
        assertEquals(encryptedBefore?.content, encryptedAfter?.content)
        assertEquals(encryptedBefore?.imagePathsRaw, encryptedAfter?.imagePathsRaw)
        assertTrue(encryptedAfter?.isDeleted == true)
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun deleteVaultNotePermanently_ignoresNormalTrashAndActiveVaultNotes() = runTest {
        val activeVaultId = repository.saveVaultNote(
            Note(title = "Active vault", content = "Hidden", creationDate = 1_000L)
        )
        val normalId = db.noteDao().insertNote(
            NoteEntity(
                title = "Normal trashed title",
                content = "Visible trashed body",
                imagePathsRaw = "images/normal-trash.jpg",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L,
                isDeleted = true,
                deletedDate = 2_000L
            )
        )

        val activeVaultDeleted = repository.deleteVaultNotePermanently(activeVaultId)
        val normalDeleted = repository.deleteVaultNotePermanently(normalId)

        assertFalse(activeVaultDeleted)
        assertFalse(normalDeleted)
        assertTrue(db.noteDao().getVaultNoteById(activeVaultId)?.isInVault == true)
        assertEquals(
            listOf(normalId),
            db.noteDao().getDeletedNotes().first().map { it.id }
        )
        assertTrue(imageStorage.deletedPaths.isEmpty())
    }

    @Test
    fun vaultTrashedNotes_decryptsOnlySoftDeletedVaultNotesWhenUnlocked() = runTest {
        val olderVaultId = repository.saveVaultNote(
            Note(
                title = "Older trash",
                content = "Older private body",
                imagePaths = listOf("images/older-trash.jpg"),
                creationDate = 1_000L
            )
        )
        val newerVaultId = repository.saveVaultNote(
            Note(
                title = "Newer trash",
                content = "Newer private body",
                imagePaths = listOf("images/newer-trash.jpg"),
                creationDate = 2_000L
            )
        )
        val activeVaultId = repository.saveVaultNote(
            Note(title = "Active vault", content = "Still active", creationDate = 3_000L)
        )
        db.noteDao().insertNote(
            NoteEntity(
                title = "Normal trashed",
                content = "Visible trash",
                creationDate = 1_000L,
                lastModifiedDate = 1_000L,
                isDeleted = true,
                deletedDate = 3_000L
            )
        )

        val olderEncrypted = db.noteDao().getVaultNoteById(olderVaultId)!!
        val newerEncrypted = db.noteDao().getVaultNoteById(newerVaultId)!!
        db.noteDao().updateNote(olderEncrypted.copy(isDeleted = true, deletedDate = 1_000L))
        db.noteDao().updateNote(newerEncrypted.copy(isDeleted = true, deletedDate = 2_000L))

        val trashedVaultNotes = repository.vaultTrashedNotes.first()

        assertEquals(listOf(newerVaultId, olderVaultId), trashedVaultNotes.map { it.id })
        assertEquals(listOf("Newer trash", "Older trash"), trashedVaultNotes.map { it.title })
        assertEquals(
            listOf("Newer private body", "Older private body"),
            trashedVaultNotes.map { it.content }
        )
        assertEquals(
            listOf(listOf("images/newer-trash.jpg"), listOf("images/older-trash.jpg")),
            trashedVaultNotes.map { it.imagePaths }
        )
        assertTrue(trashedVaultNotes.all { it.isInVault && it.isDeleted })
        assertEquals(listOf(activeVaultId), repository.vaultNotes.first().map { it.id })
        assertEquals(
            listOf("Normal trashed"),
            db.noteDao().getDeletedNotes().first().map { it.title }
        )

        val rawTrashed = db.noteDao()
            .getAllVaultNotesForWipeOnce()
            .filter { it.isDeleted }
        rawTrashed.forEach { entity ->
            assertTrue(cipher.isEncryptedPayload(entity.title))
            assertTrue(cipher.isEncryptedPayload(entity.content))
            assertTrue(cipher.isEncryptedPayload(entity.imagePathsRaw))
        }
    }

    @Test
    fun vaultTrashedNotes_returnsEmptyWhenVaultIsLocked() = runTest {
        val noteId = repository.saveVaultNote(
            Note(title = "Locked trashed vault", content = "Hidden", creationDate = 1_000L)
        )
        val encrypted = db.noteDao().getVaultNoteById(noteId)!!
        db.noteDao().updateNote(encrypted.copy(isDeleted = true, deletedDate = 1_000L))

        keyProvider.lock()

        assertTrue(repository.vaultTrashedNotes.first().isEmpty())
        assertTrue(db.noteDao().getDeletedNotes().first().isEmpty())
        val raw = db.noteDao().getAllVaultNotesForWipeOnce().single()
        assertTrue(raw.isDeleted)
        assertTrue(cipher.isEncryptedPayload(raw.title))
        assertTrue(cipher.isEncryptedPayload(raw.content))
    }

    @Test
    fun vaultReadSurfaces_skipCorruptedRowsWithoutFailingUnlockedFlows() = runTest {
        val goodActiveId = repository.saveVaultNote(
            Note(title = "Good active", content = "Readable active", creationDate = 1_000L)
        )
        val corruptActiveId = repository.saveVaultNote(
            Note(title = "Corrupt active", content = "Unreadable active", creationDate = 2_000L)
        )
        val goodTrashId = repository.saveVaultNote(
            Note(title = "Good trash", content = "Readable trash", creationDate = 3_000L)
        )
        val corruptTrashId = repository.saveVaultNote(
            Note(title = "Corrupt trash", content = "Unreadable trash", creationDate = 4_000L)
        )

        val corruptActive = db.noteDao().getVaultNoteById(corruptActiveId)!!
        db.noteDao().updateNote(corruptActive.copy(title = "not-a-vault-envelope"))

        val goodTrash = db.noteDao().getVaultNoteById(goodTrashId)!!
        db.noteDao().updateNote(goodTrash.copy(isDeleted = true, deletedDate = 1_000L))
        val corruptTrash = db.noteDao().getVaultNoteById(corruptTrashId)!!
        db.noteDao().updateNote(
            corruptTrash.copy(
                content = "not-a-vault-envelope",
                isDeleted = true,
                deletedDate = 2_000L
            )
        )

        val activeNotes = repository.vaultNotes.first()
        val trashedNotes = repository.vaultTrashedNotes.first()
        val linkCandidates = repository.vaultNoteLinkCandidates.first()

        assertEquals(listOf(goodActiveId), activeNotes.map { it.id })
        assertEquals(listOf("Good active"), activeNotes.map { it.title })
        assertEquals(listOf(goodTrashId), trashedNotes.map { it.id })
        assertEquals(listOf("Good trash"), trashedNotes.map { it.title })
        assertEquals(listOf(goodActiveId), linkCandidates.map { it.id })
        assertNull(repository.getVaultNoteById(corruptActiveId))

        val rawRows = db.noteDao().getAllVaultNotesForWipeOnce()
        assertEquals(4, rawRows.size)
        assertTrue(rawRows.any { it.id == corruptActiveId && it.title == "not-a-vault-envelope" })
        assertTrue(rawRows.any { it.id == corruptTrashId && it.content == "not-a-vault-envelope" })
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

        repository.rewrapAndCommitForTest(keyProvider, newKey)

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
    fun rewrapAllVaultNotesWith_reencryptsTrashedVaultNoteAndItsPhysicalImage() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-rewrap-trash-image").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val fileStorage = VaultImageFileStorage(imageStorage = realImageStorage)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher,
            vaultImageFileStorage = fileStorage
        )

        try {
            val relativePath = "images/note_1_img_200.jpg"
            val originalBytes = byteArrayOf(11, 22, 33, 44)
            realImageStorage.getImageFile(relativePath).apply {
                parentFile?.mkdirs()
                writeBytes(originalBytes)
            }
            val noteId = physicalRepository.saveVaultNote(
                Note(
                    title = "Trashed secret",
                    content = "Trashed body",
                    imagePaths = listOf(relativePath),
                    creationDate = 1_000L
                )
            )
            assertTrue(physicalRepository.moveVaultNoteToTrash(noteId))
            val newKey: SecretKey = SecretKeySpec(
                ByteArray(32) { index -> (index + 31).toByte() },
                "AES"
            )

            physicalRepository.rewrapAndCommitForTest(keyProvider, newKey)
            keyProvider.replaceKey(newKey)

            val trashed = physicalRepository.vaultTrashedNotes.first().single()
            assertEquals("Trashed secret", trashed.title)
            assertEquals("Trashed body", trashed.content)
            assertEquals(listOf(relativePath), trashed.imagePaths)
            val decryptedImage = physicalRepository.decryptVaultImageBytes(relativePath)
            assertTrue(decryptedImage?.contentEquals(originalBytes) == true)
            decryptedImage?.fill(0)
            assertTrue(physicalRepository.restoreVaultNoteFromTrash(noteId))
            assertEquals("Trashed secret", physicalRepository.getVaultNoteById(noteId)?.title)
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun changePin_withRealRoomAndFiles_preservesActiveTrashImagesAndRestore() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "vault-change-pin-integration").apply {
            deleteRecursively()
            mkdirs()
        }
        val dataStoreFile = File(root, "vault.preferences_pb")
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile }
        )
        val realImageStorage = InternalNoteImageStorage(filesDir = root)
        val vaultRepository = VaultRepositoryImpl(
            dataStore = dataStore,
            pinHasher = VaultPinHasher(iterations = 1_000),
            keyDeriver = VaultKeyDeriver(iterations = 1_000)
        )
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = vaultRepository,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        vaultRepository.bindNoteMaintenance(physicalRepository, physicalRepository)

        try {
            vaultRepository.configurePin("1111".toCharArray())
            assertTrue(vaultRepository.unlockWithPin("1111".toCharArray()))
            val activePath = "images/note_1_img_active.jpg"
            val trashPath = "images/note_2_img_trash.jpg"
            val activeBytes = byteArrayOf(1, 3, 5, 7)
            val trashBytes = byteArrayOf(2, 4, 6, 8)
            realImageStorage.getImageFile(activePath).apply {
                parentFile?.mkdirs()
                writeBytes(activeBytes)
            }
            realImageStorage.getImageFile(trashPath).writeBytes(trashBytes)
            val activeId = physicalRepository.saveVaultNote(
                Note(
                    title = "Active after PIN change",
                    content = "Active body",
                    imagePaths = listOf(activePath),
                    creationDate = 1_000L
                )
            )
            val trashId = physicalRepository.saveVaultNote(
                Note(
                    title = "Trash after PIN change",
                    content = "Trash body",
                    imagePaths = listOf(trashPath),
                    creationDate = 2_000L
                )
            )
            assertTrue(physicalRepository.moveVaultNoteToTrash(trashId))

            val result = vaultRepository.changePin(
                currentPin = "1111".toCharArray(),
                newPin = "2222".toCharArray()
            )

            assertEquals(ChangeVaultPinResult.Success, result)
            vaultRepository.lock()
            assertFalse(vaultRepository.unlockWithPin("1111".toCharArray()))
            assertTrue(vaultRepository.unlockWithPin("2222".toCharArray()))
            assertEquals(
                "Active after PIN change",
                physicalRepository.getVaultNoteById(activeId)?.title
            )
            assertEquals(
                "Trash after PIN change",
                physicalRepository.vaultTrashedNotes.first().single().title
            )
            val activeAfter = physicalRepository.decryptVaultImageBytes(activePath)
            val trashAfter = physicalRepository.decryptVaultImageBytes(trashPath)
            assertTrue(activeAfter?.contentEquals(activeBytes) == true)
            assertTrue(trashAfter?.contentEquals(trashBytes) == true)
            activeAfter?.fill(0)
            trashAfter?.fill(0)
            assertTrue(physicalRepository.restoreVaultNoteFromTrash(trashId))
            assertEquals(
                "Trash after PIN change",
                physicalRepository.getVaultNoteById(trashId)?.title
            )
            assertTrue(
                root.walkTopDown().none { file -> file.name.contains(".rekey-old-") }
            )
        } finally {
            dataStoreScope.cancel()
            root.deleteRecursively()
        }
    }

    @Test
    fun rewrapAllVaultNotesWith_roomFailureRollsBackRowsAndImagesToOldKey() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "vault-rewrap-room-failure").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = root)
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        val firstPath = "images/room_failure_first.jpg"
        val secondPath = "images/room_failure_second.jpg"
        val firstBytes = byteArrayOf(9, 8, 7, 6)
        val secondBytes = byteArrayOf(6, 7, 8, 9)

        try {
            realImageStorage.getImageFile(firstPath).apply {
                parentFile?.mkdirs()
                writeBytes(firstBytes)
            }
            realImageStorage.getImageFile(secondPath).writeBytes(secondBytes)
            val firstId = physicalRepository.saveVaultNote(
                Note(title = "First old key", imagePaths = listOf(firstPath))
            )
            val secondId = physicalRepository.saveVaultNote(
                Note(title = "Second old key", imagePaths = listOf(secondPath))
            )
            db.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_vault_rewrap
                BEFORE UPDATE ON notes
                WHEN OLD.id = $secondId
                BEGIN
                    SELECT RAISE(ABORT, 'forced rewrap update failure');
                END
                """.trimIndent()
            )
            val newKey = SecretKeySpec(ByteArray(32) { (it + 41).toByte() }, "AES")

            val result = runCatching {
                physicalRepository.rewrapAndCommitForTest(keyProvider, newKey)
            }

            assertTrue(result.isFailure)
            assertEquals("First old key", physicalRepository.getVaultNoteById(firstId)?.title)
            assertEquals("Second old key", physicalRepository.getVaultNoteById(secondId)?.title)
            val firstAfter = physicalRepository.decryptVaultImageBytes(firstPath)
            val secondAfter = physicalRepository.decryptVaultImageBytes(secondPath)
            assertTrue(firstAfter?.contentEquals(firstBytes) == true)
            assertTrue(secondAfter?.contentEquals(secondBytes) == true)
            firstAfter?.fill(0)
            secondAfter?.fill(0)
            assertTrue(root.walkTopDown().none { it.name.contains(".rekey-old-") })
        } finally {
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS fail_vault_rewrap")
            root.deleteRecursively()
        }
    }

    @Test
    fun rewrapAllVaultNotesWith_imageFailureRollsBackEarlierImageAndRoomRows() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "vault-rewrap-image-failure").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = root)
        val setupRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        val firstPath = "images/io_failure_first.jpg"
        val failingPath = "images/io_failure_second.jpg"
        val firstBytes = byteArrayOf(12, 13, 14)
        val secondBytes = byteArrayOf(21, 22, 23)

        try {
            realImageStorage.getImageFile(firstPath).apply {
                parentFile?.mkdirs()
                writeBytes(firstBytes)
            }
            realImageStorage.getImageFile(failingPath).writeBytes(secondBytes)
            val firstId = setupRepository.saveVaultNote(
                Note(title = "First I/O old key", imagePaths = listOf(firstPath))
            )
            val secondId = setupRepository.saveVaultNote(
                Note(title = "Second I/O old key", imagePaths = listOf(failingPath))
            )
            val failingRepository = VaultNoteRepositoryImpl(
                database = db,
                dao = db.noteDao(),
                tagDao = db.tagDao(),
                keyProvider = keyProvider,
                imageStorage = realImageStorage,
                fieldCipher = cipher,
                vaultImageFileStorage = RewrapFailingVaultImageFileStorage(
                    imageStorage = realImageStorage,
                    failPath = failingPath
                )
            )
            val newKey = SecretKeySpec(ByteArray(32) { (it + 51).toByte() }, "AES")

            val result = runCatching {
                failingRepository.rewrapAndCommitForTest(keyProvider, newKey)
            }

            assertTrue(result.isFailure)
            assertEquals("First I/O old key", setupRepository.getVaultNoteById(firstId)?.title)
            assertEquals("Second I/O old key", setupRepository.getVaultNoteById(secondId)?.title)
            val firstAfter = setupRepository.decryptVaultImageBytes(firstPath)
            val secondAfter = setupRepository.decryptVaultImageBytes(failingPath)
            assertTrue(firstAfter?.contentEquals(firstBytes) == true)
            assertTrue(secondAfter?.contentEquals(secondBytes) == true)
            firstAfter?.fill(0)
            secondAfter?.fill(0)
            assertTrue(root.walkTopDown().none { it.name.contains(".rekey-old-") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rewrapAllVaultNotesWith_cancellationRollsBackImageAndRows() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "vault-rewrap-cancellation").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(filesDir = root)
        val setupRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        val path = "images/cancel_rewrap.jpg"
        val originalBytes = byteArrayOf(31, 32, 33)

        try {
            realImageStorage.getImageFile(path).apply {
                parentFile?.mkdirs()
                writeBytes(originalBytes)
            }
            val noteId = setupRepository.saveVaultNote(
                Note(title = "Cancellation old key", imagePaths = listOf(path))
            )
            val pausingStorage = PausingRewrapVaultImageFileStorage(
                imageStorage = realImageStorage,
                pausePath = path
            )
            val cancellingRepository = VaultNoteRepositoryImpl(
                database = db,
                dao = db.noteDao(),
                tagDao = db.tagDao(),
                keyProvider = keyProvider,
                imageStorage = realImageStorage,
                fieldCipher = cipher,
                vaultImageFileStorage = pausingStorage
            )
            val newKey = SecretKeySpec(ByteArray(32) { (it + 61).toByte() }, "AES")
            val rewrapJob = async(Dispatchers.Default) {
                cancellingRepository.rewrapAndCommitForTest(keyProvider, newKey)
            }

            pausingStorage.replacementFinished.await()
            rewrapJob.cancel()
            pausingStorage.allowReturn.complete(Unit)
            val cancellation = runCatching { rewrapJob.await() }.exceptionOrNull()

            assertTrue(cancellation is kotlinx.coroutines.CancellationException)
            assertTrue(rewrapJob.isCancelled)
            assertEquals(1, pausingStorage.rollbackCount)
            assertEquals(1, pausingStorage.backupDeletionCount)
            assertEquals("Cancellation old key", setupRepository.getVaultNoteById(noteId)?.title)
            val imageAfter = setupRepository.decryptVaultImageBytes(path)
            assertTrue(imageAfter?.contentEquals(originalBytes) == true)
            imageAfter?.fill(0)
            assertTrue(root.walkTopDown().none { it.name.contains(".rekey-old-") })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun changePin_dataStoreFailureRollsBackRealRoomRowsAndPhysicalImage() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "vault-change-pin-datastore-failure").apply {
            deleteRecursively()
            mkdirs()
        }
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val delegate = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(root, "vault.preferences_pb") }
        )
        val dataStore = FailingPreferencesDataStore(delegate)
        val realImageStorage = InternalNoteImageStorage(filesDir = root)
        val vaultRepository = VaultRepositoryImpl(
            dataStore = dataStore,
            pinHasher = VaultPinHasher(iterations = 1_000),
            keyDeriver = VaultKeyDeriver(iterations = 1_000)
        )
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = vaultRepository,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        vaultRepository.bindNoteMaintenance(physicalRepository, physicalRepository)
        val path = "images/datastore_failure.jpg"
        val originalBytes = byteArrayOf(71, 72, 73)

        try {
            vaultRepository.configurePin("1111".toCharArray())
            assertTrue(vaultRepository.unlockWithPin("1111".toCharArray()))
            realImageStorage.getImageFile(path).apply {
                parentFile?.mkdirs()
                writeBytes(originalBytes)
            }
            val noteId = physicalRepository.saveVaultNote(
                Note(title = "DataStore old key", imagePaths = listOf(path))
            )
            dataStore.failNextUpdate = true

            val result = vaultRepository.changePin(
                currentPin = "1111".toCharArray(),
                newPin = "2222".toCharArray()
            )

            assertEquals(ChangeVaultPinResult.RewrapFailed, result)
            assertEquals("DataStore old key", physicalRepository.getVaultNoteById(noteId)?.title)
            val imageAfter = physicalRepository.decryptVaultImageBytes(path)
            assertTrue(imageAfter?.contentEquals(originalBytes) == true)
            imageAfter?.fill(0)
            vaultRepository.lock()
            assertTrue(vaultRepository.unlockWithPin("1111".toCharArray()))
            assertFalse(vaultRepository.unlockWithPin("2222".toCharArray()))
            assertTrue(root.walkTopDown().none { it.name.contains(".rekey-old-") })
        } finally {
            dataStoreScope.cancel()
            root.deleteRecursively()
        }
    }

    @Test
    fun rewrapAllVaultNotesWith_failsWhenLocked() = runTest {
        val noteId = repository.saveVaultNote(
            Note(title = "Locked title", content = "Locked body", creationDate = 1_000L)
        )
        val before = db.noteDao().getVaultNoteById(noteId)
        keyProvider.lock()
        val newKey: SecretKey = SecretKeySpec(ByteArray(32) { idx -> (idx + 7).toByte() }, "AES")

        val result = runCatching { repository.rewrapAndCommitForTest(keyProvider, newKey) }

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

        repository.rewrapAndCommitForTest(keyProvider, newKey)

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
        assertTrue(db.noteDao().getAllVaultNotesForWipeOnce().isEmpty())
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
        assertTrue(db.noteDao().getAllVaultNotesForWipeOnce().isNotEmpty())
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
    fun wipeAllVaultNotes_removesCorruptedRowAndStillCleansDecryptableImages() = runTest {
        val goodId = repository.saveVaultNote(
            Note(
                title = "Good vault",
                content = "Good body",
                imagePaths = listOf("images/good.jpg"),
                creationDate = 1_000L
            )
        )
        val corruptId = repository.saveVaultNote(
            Note(
                title = "Corrupt vault",
                content = "Corrupt body",
                imagePaths = listOf("images/corrupt.jpg"),
                creationDate = 2_000L
            )
        )
        // Corrupt the encrypted image-path payload so it can no longer be
        // decrypted: reset must still drop the row instead of failing.
        val corruptEntity = db.noteDao().getVaultNoteById(corruptId)!!
        db.noteDao().updateNote(corruptEntity.copy(imagePathsRaw = "not-a-vault-envelope"))

        val removed = repository.wipeAllVaultNotes()

        assertEquals(2, removed)
        assertTrue(db.noteDao().getAllVaultNotesForWipeOnce().isEmpty())
        assertNull(db.noteDao().getNoteById(goodId))
        assertNull(db.noteDao().getNoteById(corruptId))
        // The decryptable row's image is cleaned; the corrupted row's image
        // path is unknown, so it is skipped (best-effort cleanup).
        assertEquals(listOf("images/good.jpg"), imageStorage.deletedPaths)
    }

    @Test
    fun deleteVaultNotePermanently_deletesCorruptedTrashedRowWhenUnlocked() = runTest {
        val noteId = repository.saveVaultNote(
            Note(
                title = "Corrupt trashed",
                content = "Body",
                imagePaths = listOf("images/corrupt-trash.jpg"),
                creationDate = 1_000L
            )
        )
        val entity = db.noteDao().getVaultNoteById(noteId)!!
        // Soft-delete it and corrupt its encrypted image-path payload.
        db.noteDao().updateNote(
            entity.copy(
                isDeleted = true,
                deletedDate = 2_000L,
                imagePathsRaw = "not-a-vault-envelope"
            )
        )

        val deleted = repository.deleteVaultNotePermanently(noteId)

        assertTrue(deleted)
        assertNull(db.noteDao().getDeletedVaultNoteById(noteId))
        assertNull(db.noteDao().getNoteById(noteId))
        // Image path could not be decrypted, so no file cleanup is attempted.
        assertTrue(imageStorage.deletedPaths.isEmpty())
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
    fun saveVaultNote_rollsBackDatabaseUpdateWhenImageEncryptionFails() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-save-rollback").apply {
            deleteRecursively()
            mkdirs()
        }
        val relativePath = "images/note_1_img_save_rollback.jpg"
        val realImageStorage = InternalNoteImageStorage(filesDir = imageRoot)
        val setupRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        val failingRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher,
            vaultImageFileStorage = FailingVaultImageFileStorage(
                imageStorage = realImageStorage,
                failPath = relativePath
            )
        )

        try {
            val noteId = setupRepository.saveVaultNote(
                Note(
                    title = "Original Vault title",
                    content = "Original Vault body",
                    creationDate = 1_000L
                )
            )
            val plainBytes = byteArrayOf(13, 21, 34, 55)
            val imageFile = realImageStorage.getImageFile(relativePath)
            imageFile.parentFile?.mkdirs()
            imageFile.writeBytes(plainBytes)

            val result = runCatching {
                failingRepository.saveVaultNote(
                    Note(
                        id = noteId,
                        title = "Updated Vault title",
                        content = "Updated Vault body",
                        imagePaths = listOf(relativePath),
                        creationDate = 1_000L
                    )
                )
            }

            assertTrue(result.exceptionOrNull() is IOException)
            val decrypted = setupRepository.getVaultNoteById(noteId)
            assertEquals("Original Vault title", decrypted?.title)
            assertEquals("Original Vault body", decrypted?.content)
            assertTrue(decrypted?.imagePaths?.isEmpty() == true)
            assertTrue(
                "The failed image path must not be committed to the Vault row.",
                imageFile.readBytes().contentEquals(plainBytes)
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

    @Test
    fun duplicateVaultNote_copiesVaultImageFileAndRewritesMarkdownPath() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-duplicate-images").apply {
            deleteRecursively()
            mkdirs()
        }
        val realImageStorage = InternalNoteImageStorage(
            filesDir = imageRoot,
            processImage = { inputStreamProvider, destination ->
                val input = inputStreamProvider() ?: throw IOException("Missing source")
                input.use { source ->
                    destination.outputStream().use { output -> source.copyTo(output) }
                }
            }
        )
        val physicalRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )

        try {
            val sourcePath = "images/note_1_img_duplicated.jpg"
            val plainBytes = byteArrayOf(7, 6, 5, 4, 3)
            val sourceFile = realImageStorage.getImageFile(sourcePath)
            sourceFile.parentFile?.mkdirs()
            sourceFile.writeBytes(plainBytes)
            val sourceId = physicalRepository.saveVaultNote(
                Note(
                    title = "With image",
                    content = "Before\n![image]($sourcePath)\nAfter #vaulttag",
                    imagePaths = listOf(sourcePath),
                    creationDate = 1_000L
                )
            )

            val result = physicalRepository.duplicateVaultNote(sourceId)

            assertTrue(result is DuplicateVaultNoteResult.Success)
            val duplicateId = (result as DuplicateVaultNoteResult.Success).noteId
            val duplicate = physicalRepository.getVaultNoteById(duplicateId)
            val duplicatePath = duplicate?.imagePaths?.single()
                ?: error("Duplicate Vault note should reference a copied image.")

            assertNotEquals(sourcePath, duplicatePath)
            assertEquals(
                "Before\n![image]($duplicatePath)\nAfter #vaulttag",
                duplicate?.content
            )
            assertTrue(db.tagDao().getCrossRefsForNote(duplicateId).isEmpty())
            val duplicateFile = realImageStorage.getImageFile(duplicatePath)
            assertTrue(duplicateFile.exists())
            assertFalse(plainBytes.contentEquals(duplicateFile.readBytes()))
            assertTrue(
                plainBytes.contentEquals(
                    physicalRepository.decryptVaultImageBytes(duplicatePath)!!
                )
            )
            assertTrue(
                plainBytes.contentEquals(physicalRepository.decryptVaultImageBytes(sourcePath)!!)
            )
        } finally {
            imageRoot.deleteRecursively()
        }
    }

    @Test
    fun duplicateVaultNote_deletesCopiedImageWhenEncryptionFails() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val imageRoot = File(context.cacheDir, "vault-note-repository-duplicate-rollback").apply {
            deleteRecursively()
            mkdirs()
        }
        val duplicatePath = "images/note_2_img_7.jpg"
        val realImageStorage = InternalNoteImageStorage(
            filesDir = imageRoot,
            currentTimeMillis = { 7L },
            processImage = { inputStreamProvider, destination ->
                val input = inputStreamProvider() ?: throw IOException("Missing source")
                input.use { source ->
                    destination.outputStream().use { output -> source.copyTo(output) }
                }
            }
        )
        val setupRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher
        )
        val failingRepository = VaultNoteRepositoryImpl(
            database = db,
            dao = db.noteDao(),
            tagDao = db.tagDao(),
            keyProvider = keyProvider,
            imageStorage = realImageStorage,
            fieldCipher = cipher,
            vaultImageFileStorage = FailingVaultImageFileStorage(
                imageStorage = realImageStorage,
                failPath = duplicatePath
            )
        )

        try {
            val sourcePath = "images/note_1_img_duplicate_rollback_source.jpg"
            val plainBytes = byteArrayOf(10, 20, 30, 40)
            val sourceFile = realImageStorage.getImageFile(sourcePath)
            sourceFile.parentFile?.mkdirs()
            sourceFile.writeBytes(plainBytes)
            val sourceId = setupRepository.saveVaultNote(
                Note(
                    title = "Rollback source",
                    content = "Before\n![image]($sourcePath)\nAfter #vaulttag",
                    imagePaths = listOf(sourcePath),
                    creationDate = 1_000L
                )
            )

            val result = failingRepository.duplicateVaultNote(sourceId)

            assertEquals(DuplicateVaultNoteResult.Failed, result)
            assertEquals(
                listOf(sourceId),
                db.noteDao().getAllVaultNotesForWipeOnce().map { it.id }
            )
            assertFalse(realImageStorage.getImageFile(duplicatePath).exists())
            assertTrue(
                plainBytes.contentEquals(failingRepository.decryptVaultImageBytes(sourcePath)!!)
            )
            assertTrue(db.tagDao().getCrossRefsForNote(sourceId).isEmpty())
        } finally {
            imageRoot.deleteRecursively()
        }
    }
}

private fun testVaultKey(): SecretKey =
    SecretKeySpec(ByteArray(32) { index ->
        (index + 1).toByte()
    }, "AES")

private fun NexNoteDatabase.countRows(tableName: String): Int {
    val cursor = openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tableName")
    return try {
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    } finally {
        cursor.close()
    }
}

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

private class RewrapFailingVaultImageFileStorage(
    imageStorage: NoteImageStorage,
    private val failPath: String
) : VaultImageFileStorage(imageStorage = imageStorage) {
    override suspend fun rewrapInPlace(
        relativePath: String,
        currentKey: SecretKey,
        newKey: SecretKey,
        onBackupCreated: (VaultImageFileRewrapBackup) -> Unit
    ): VaultImageFileRewrapBackup? {
        if (relativePath == failPath) {
            throw IOException("Forced Vault image rewrap failure.")
        }
        return super.rewrapInPlace(relativePath, currentKey, newKey, onBackupCreated)
    }
}

private class PausingRewrapVaultImageFileStorage(
    imageStorage: NoteImageStorage,
    private val pausePath: String
) : VaultImageFileStorage(imageStorage = imageStorage) {
    val replacementFinished = CompletableDeferred<Unit>()
    val allowReturn = CompletableDeferred<Unit>()
    @Volatile
    var rollbackCount = 0
        private set
    @Volatile
    var backupDeletionCount = 0
        private set

    override suspend fun rewrapInPlace(
        relativePath: String,
        currentKey: SecretKey,
        newKey: SecretKey,
        onBackupCreated: (VaultImageFileRewrapBackup) -> Unit
    ): VaultImageFileRewrapBackup? {
        val backup = super.rewrapInPlace(
            relativePath,
            currentKey,
            newKey,
            onBackupCreated
        )
        if (relativePath == pausePath) {
            replacementFinished.complete(Unit)
            allowReturn.await()
        }
        return backup
    }

    override suspend fun rollbackRewrap(backup: VaultImageFileRewrapBackup) {
        rollbackCount += 1
        super.rollbackRewrap(backup)
    }

    override suspend fun commitRewrap(backup: VaultImageFileRewrapBackup) {
        backupDeletionCount += 1
        super.commitRewrap(backup)
    }
}

private class FailingPreferencesDataStore(
    private val delegate: DataStore<Preferences>
) : DataStore<Preferences> {
    var failNextUpdate: Boolean = false

    override val data: Flow<Preferences>
        get() = delegate.data

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences {
        if (failNextUpdate) {
            failNextUpdate = false
            throw IOException("Forced DataStore commit failure.")
        }
        return delegate.updateData(transform)
    }
}

private class TestVaultKeyProvider : VaultUnlockedKeyProvider {
    private val key = MutableStateFlow<SecretKey?>(testVaultKey())
    override val unlockedVaultKey: StateFlow<SecretKey?> = key

    fun lock() {
        key.value = null
    }

    fun replaceKey(newKey: SecretKey) {
        key.value = newKey
    }

    override suspend fun <T> withUnlockedVaultKey(block: suspend (SecretKey) -> T): T? {
        val unlockedKey = key.value ?: return null
        return block(unlockedKey)
    }
}

private suspend fun VaultNoteRepositoryImpl.rewrapAndCommitForTest(
    keyProvider: VaultUnlockedKeyProvider,
    newKey: SecretKey
) {
    val transaction = keyProvider.withUnlockedVaultKey { currentKey ->
        rewrapAllVaultNotesWith(currentKey, newKey)
    } ?: throw VaultLockedException()
    transaction.commit()
}

private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(1_000L) {
        while (!condition()) {
            delay(10L)
        }
    }
}
