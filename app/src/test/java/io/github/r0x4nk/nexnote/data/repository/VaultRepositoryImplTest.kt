package io.github.r0x4nk.nexnote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.github.r0x4nk.nexnote.data.security.ProtectVaultUnlockMaterialResult
import io.github.r0x4nk.nexnote.data.security.UnprotectVaultUnlockMaterialResult
import io.github.r0x4nk.nexnote.data.security.VaultAndroidCredentialProtectedMaterial
import io.github.r0x4nk.nexnote.data.security.VaultFieldCipher
import io.github.r0x4nk.nexnote.data.security.VaultKeyDeriver
import io.github.r0x4nk.nexnote.data.security.VaultPinHasher
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.domain.repository.ChangeVaultPinResult
import io.github.r0x4nk.nexnote.domain.repository.RefreshVaultAndroidCredentialProtectedMaterialResult
import io.github.r0x4nk.nexnote.domain.repository.ResetVaultResult
import io.github.r0x4nk.nexnote.domain.repository.UnlockVaultWithAndroidCredentialResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Base64
import javax.crypto.SecretKey

private object FailingVaultNoteRewrapper : VaultNoteRewrapper {
    override suspend fun rewrapAllVaultNotesWith(
        currentKey: SecretKey,
        newKey: SecretKey
    ): VaultNoteRewrapTransaction = error("No test rewrapper configured")
}

private object FailingVaultNoteWiper : VaultNoteWiper {
    override suspend fun wipeAllVaultNotes(): Int = error("No test wiper configured")
}

@OptIn(ExperimentalCoroutinesApi::class)
class VaultRepositoryImplTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun createTestDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("vault.preferences_pb") }
        )

    private fun createRepository(
        dataStore: DataStore<Preferences>,
        rewrapper: VaultNoteRewrapper? = null,
        wiper: VaultNoteWiper? = null,
        protectUnlockMaterial: ((ByteArray) -> ProtectVaultUnlockMaterialResult)? = null,
        unprotectUnlockMaterial: ((String) -> UnprotectVaultUnlockMaterialResult)? = null
    ): VaultRepositoryImpl {
        val repository = VaultRepositoryImpl(
            dataStore = dataStore,
            pinHasher = VaultPinHasher(iterations = TEST_ITERATIONS),
            keyDeriver = VaultKeyDeriver(iterations = TEST_ITERATIONS),
            protectUnlockMaterial = protectUnlockMaterial,
            unprotectUnlockMaterial = unprotectUnlockMaterial
        )
        repository.bindNoteMaintenance(
            rewrapper = rewrapper ?: FailingVaultNoteRewrapper,
            wiper = wiper ?: FailingVaultNoteWiper
        )
        return repository
    }

    @Test
    fun `initial state is not configured`() = testScope.runTest {
        val repository = createRepository(createTestDataStore())

        assertEquals(VaultState.NOT_CONFIGURED, repository.state.first())
    }

    @Test
    fun `configurePin stores a verifier without storing the pin in clear`() = testScope.runTest {
        val dataStore = createTestDataStore()
        val repository = createRepository(dataStore)

        repository.configurePin("1234".toCharArray())

        val prefs = dataStore.data.first()
        val storedStringValues = prefs.asMap().values.filterIsInstance<String>()

        assertEquals(VaultPinHasher.ALGORITHM, prefs[VaultRepositoryImpl.PIN_ALGORITHM_KEY])
        assertEquals(TEST_ITERATIONS, prefs[VaultRepositoryImpl.PIN_ITERATIONS_KEY])
        assertFalse(prefs[VaultRepositoryImpl.PIN_SALT_KEY].isNullOrBlank())
        assertFalse(prefs[VaultRepositoryImpl.PIN_HASH_KEY].isNullOrBlank())
        assertEquals(VaultKeyDeriver.ALGORITHM, prefs[VaultRepositoryImpl.KEY_DERIVATION_ALGORITHM_KEY])
        assertEquals(TEST_ITERATIONS, prefs[VaultRepositoryImpl.KEY_DERIVATION_ITERATIONS_KEY])
        assertFalse(prefs[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY].isNullOrBlank())
        assertEquals(
            VaultKeyDeriver.DEFAULT_KEY_LENGTH_BITS,
            prefs[VaultRepositoryImpl.KEY_DERIVATION_KEY_LENGTH_BITS_KEY]
        )
        assertFalse(storedStringValues.contains("1234"))
        assertEquals(VaultState.LOCKED, repository.state.first())
    }

    @Test
    fun `unlockWithPin unlocks only with the configured pin`() = testScope.runTest {
        val repository = createRepository(createTestDataStore())
        repository.configurePin("2468".toCharArray())

        assertFalse(repository.unlockWithPin("0000".toCharArray()))
        assertEquals(VaultState.LOCKED, repository.state.first())

        assertTrue(repository.unlockWithPin("2468".toCharArray()))
        assertEquals(VaultState.UNLOCKED, repository.state.first())
    }

    @Test
    fun `unlockWithPin stores Android credential protected material only after successful pin unlock`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                protectUnlockMaterial = protector::protect
            )
            repository.configurePin("2468".toCharArray())

            assertFalse(repository.unlockWithPin("0000".toCharArray()))
            assertEquals(0, protector.callCount)
            assertNull(
                dataStore.data.first()[
                    VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
                ]
            )

            assertTrue(repository.unlockWithPin("2468".toCharArray()))

            val storedEnvelope = dataStore.data.first()[
                VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
            ]
            val activeKeyBytes = repository.withUnlockedVaultKey { key -> key.encoded }

            assertEquals(1, protector.callCount)
            assertNotNull(storedEnvelope)
            assertTrue(repository.hasAndroidCredentialProtectedUnlockMaterial.first())
            assertEquals(protector.protectedMaterials.single().encode(), storedEnvelope)
            assertFalse(storedEnvelope!!.contains("2468"))
            assertNotNull(activeKeyBytes)
            assertTrue(protector.materials.single().contentEquals(activeKeyBytes))
        }

    @Test
    fun `hasAndroidCredentialProtectedUnlockMaterial reflects stored envelope presence`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                protectUnlockMaterial = protector::protect
            )

            assertFalse(repository.hasAndroidCredentialProtectedUnlockMaterial.first())

            repository.configurePin("2468".toCharArray())
            assertFalse(repository.hasAndroidCredentialProtectedUnlockMaterial.first())

            assertTrue(repository.unlockWithPin("2468".toCharArray()))
            assertTrue(repository.hasAndroidCredentialProtectedUnlockMaterial.first())
        }

    @Test
    fun `clearAndroidCredentialProtectedUnlockMaterial removes stored envelope only`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                protectUnlockMaterial = protector::protect
            )
            repository.configurePin("2468".toCharArray())
            assertTrue(repository.unlockWithPin("2468".toCharArray()))
            assertTrue(repository.hasAndroidCredentialProtectedUnlockMaterial.first())

            repository.clearAndroidCredentialProtectedUnlockMaterial()

            assertFalse(repository.hasAndroidCredentialProtectedUnlockMaterial.first())
            assertNull(
                dataStore.data.first()[
                    VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
                ]
            )
            assertEquals(VaultState.UNLOCKED, repository.state.first())
            assertTrue(repository.unlockWithPin("2468".toCharArray()))
        }

    @Test
    fun `unlocked key is available only while vault is unlocked`() = testScope.runTest {
        val repository = createRepository(createTestDataStore())
        repository.configurePin("2468".toCharArray())

        assertNull(repository.withUnlockedVaultKey { key -> key.algorithm })

        assertTrue(repository.unlockWithPin("2468".toCharArray()))
        assertEquals("AES", repository.withUnlockedVaultKey { key -> key.algorithm })

        repository.lock()

        assertNull(repository.withUnlockedVaultKey { key -> key.algorithm })
    }

    @Test
    fun `unlockWithAndroidCredential returns NoProtectedMaterial when envelope is absent`() =
        testScope.runTest {
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = createTestDataStore(),
                unprotectUnlockMaterial = protector::unprotect
            )
            repository.configurePin("2468".toCharArray())

            val result = repository.unlockWithAndroidCredential()

            assertSame(UnlockVaultWithAndroidCredentialResult.NoProtectedMaterial, result)
            assertEquals(0, protector.unprotectCallCount)
            assertEquals(VaultState.LOCKED, repository.state.first())
        }

    @Test
    fun `refreshAndroidCredentialProtectedUnlockMaterial returns VaultLocked when locked`() =
        testScope.runTest {
            val repository = createRepository(createTestDataStore())
            repository.configurePin("2468".toCharArray())

            val result = repository.refreshAndroidCredentialProtectedUnlockMaterial()

            assertSame(RefreshVaultAndroidCredentialProtectedMaterialResult.VaultLocked, result)
        }

    @Test
    fun `refreshAndroidCredentialProtectedUnlockMaterial rewrites envelope from unlocked key`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                protectUnlockMaterial = protector::protect
            )
            repository.configurePin("2468".toCharArray())
            assertTrue(repository.unlockWithPin("2468".toCharArray()))
            dataStore.edit { prefs ->
                prefs.remove(VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY)
            }
            assertFalse(repository.hasAndroidCredentialProtectedUnlockMaterial.first())

            val result = repository.refreshAndroidCredentialProtectedUnlockMaterial()

            val storedEnvelope = dataStore.data.first()[
                VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
            ]
            assertSame(RefreshVaultAndroidCredentialProtectedMaterialResult.Success, result)
            assertEquals(2, protector.callCount)
            assertNotNull(storedEnvelope)
            assertTrue(repository.hasAndroidCredentialProtectedUnlockMaterial.first())
        }

    @Test
    fun `unlockWithAndroidCredential unlocks with persisted protected material`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                protectUnlockMaterial = protector::protect,
                unprotectUnlockMaterial = protector::unprotect
            )
            repository.configurePin("2468".toCharArray())
            assertTrue(repository.unlockWithPin("2468".toCharArray()))
            val pinUnlockedKeyBytes = repository.withUnlockedVaultKey { key ->
                key.encoded.copyOf()
            }

            repository.lock()
            val result = repository.unlockWithAndroidCredential()

            val androidUnlockedKeyBytes = repository.withUnlockedVaultKey { key -> key.encoded }
            assertSame(UnlockVaultWithAndroidCredentialResult.Success, result)
            assertEquals(1, protector.unprotectCallCount)
            assertEquals(VaultState.UNLOCKED, repository.state.first())
            assertNotNull(pinUnlockedKeyBytes)
            assertNotNull(androidUnlockedKeyBytes)
            assertTrue(androidUnlockedKeyBytes!!.contentEquals(pinUnlockedKeyBytes!!))
        }

    @Test
    fun `unlockWithAndroidCredential clears invalidated protected material`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val protector = RecordingUnlockMaterialProtector(
                unprotectResult = UnprotectVaultUnlockMaterialResult.KeyInvalidated
            )
            val repository = createRepository(
                dataStore = dataStore,
                unprotectUnlockMaterial = protector::unprotect
            )
            repository.configurePin("2468".toCharArray())
            dataStore.edit { prefs ->
                prefs[VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY] =
                    protector.stubProtectedMaterial().encode()
            }
            assertTrue(repository.hasAndroidCredentialProtectedUnlockMaterial.first())

            val result = repository.unlockWithAndroidCredential()

            assertSame(UnlockVaultWithAndroidCredentialResult.KeyInvalidated, result)
            assertEquals(1, protector.unprotectCallCount)
            assertNull(
                dataStore.data.first()[
                    VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
                ]
            )
            assertFalse(repository.hasAndroidCredentialProtectedUnlockMaterial.first())
            assertEquals(VaultState.LOCKED, repository.state.first())
        }

    @Test
    fun `persisted key derivation params allow the same pin to reopen encrypted fields`() = testScope.runTest {
        val dataStore = createTestDataStore()
        val firstRepository = createRepository(dataStore)
        val cipher = VaultFieldCipher()
        val plaintext = "private vault field"

        firstRepository.configurePin("1357".toCharArray())
        assertTrue(firstRepository.unlockWithPin("1357".toCharArray()))
        val encrypted = firstRepository.withUnlockedVaultKey { key ->
            cipher.encryptToString(plaintext, key)
        }

        firstRepository.lock()
        val secondRepository = createRepository(dataStore)

        assertEquals(VaultState.LOCKED, secondRepository.state.first())
        assertNull(secondRepository.withUnlockedVaultKey { key -> key.algorithm })

        assertTrue(secondRepository.unlockWithPin("1357".toCharArray()))
        val decrypted = secondRepository.withUnlockedVaultKey { key ->
            cipher.decryptToString(encrypted.orEmpty(), key)
        }

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `lock returns a configured vault to locked state`() = testScope.runTest {
        val repository = createRepository(createTestDataStore())
        repository.configurePin("2468".toCharArray())
        assertTrue(repository.unlockWithPin("2468".toCharArray()))

        repository.lock()

        assertEquals(VaultState.LOCKED, repository.state.first())
    }

    @Test
    fun `unlockWithPin does not unlock an unconfigured vault`() = testScope.runTest {
        val repository = createRepository(createTestDataStore())

        assertFalse(repository.unlockWithPin("1234".toCharArray()))
        assertEquals(VaultState.NOT_CONFIGURED, repository.state.first())
    }

    @Test
    fun `changePin returns VaultNotConfigured when no pin is set`() = testScope.runTest {
        val repository = createRepository(createTestDataStore(), RecordingRewrapper())

        val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.VaultNotConfigured, result)
    }

    @Test
    fun `changePin returns VaultLocked when configured but locked`() = testScope.runTest {
        val rewrapper = RecordingRewrapper()
        val repository = createRepository(createTestDataStore(), rewrapper)
        repository.configurePin("1111".toCharArray())

        val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.VaultLocked, result)
        assertEquals(0, rewrapper.callCount)
    }

    @Test
    fun `changePin returns WrongCurrentPin when stored verifier does not match`() = testScope.runTest {
        val rewrapper = RecordingRewrapper()
        val repository = createRepository(createTestDataStore(), rewrapper)
        repository.configurePin("1111".toCharArray())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))

        val result = repository.changePin("9999".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.WrongCurrentPin, result)
        assertEquals(0, rewrapper.callCount)
        // Still unlocked under the old key, no PIN change happened.
        assertEquals(VaultState.UNLOCKED, repository.state.first())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
    }

    @Test
    fun `changePin returns InvalidNewPin when newPin is empty`() = testScope.runTest {
        val rewrapper = RecordingRewrapper()
        val repository = createRepository(createTestDataStore(), rewrapper)
        repository.configurePin("1111".toCharArray())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))

        val result = repository.changePin("1111".toCharArray(), CharArray(0))

        assertSame(ChangeVaultPinResult.InvalidNewPin, result)
        assertEquals(0, rewrapper.callCount)
    }

    @Test
    fun `changePin returns RewrapFailed when no rewrapper is wired`() = testScope.runTest {
        val repository = createRepository(createTestDataStore(), rewrapper = null)
        repository.configurePin("1111".toCharArray())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))

        val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.RewrapFailed, result)
        // Old PIN still works because nothing was persisted.
        repository.lock()
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
        assertFalse(repository.unlockWithPin("2222".toCharArray()))
    }

    @Test
    fun `changePin returns RewrapFailed when rewrapper throws and leaves state untouched`() = testScope.runTest {
        val dataStore = createTestDataStore()
        val failingRewrapper = RecordingRewrapper(throwOnRewrap = true)
        val repository = createRepository(dataStore, failingRewrapper)

        repository.configurePin("1111".toCharArray())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
        val originalKeyAlgorithm = repository.withUnlockedVaultKey { key -> key.algorithm }
        val originalPinHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]
        val originalKeySalt = dataStore.data.first()[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY]

        val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.RewrapFailed, result)
        assertEquals(1, failingRewrapper.callCount)
        // Persisted verifier/key params untouched.
        val prefs = dataStore.data.first()
        assertEquals(originalPinHash, prefs[VaultRepositoryImpl.PIN_HASH_KEY])
        assertEquals(originalKeySalt, prefs[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY])
        // In-memory key still the original one (Vault still unlocked).
        assertEquals(originalKeyAlgorithm, repository.withUnlockedVaultKey { key -> key.algorithm })
        assertEquals(VaultState.UNLOCKED, repository.state.first())
        // Old PIN still works after a relock; new PIN does not.
        repository.lock()
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
        assertFalse(repository.unlockWithPin("2222".toCharArray()))
    }

    @Test
    fun `changePin propagates cancellation from rewrap and keeps old pin usable`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val repository = createRepository(
                dataStore = dataStore,
                rewrapper = object : VaultNoteRewrapper {
                    override suspend fun rewrapAllVaultNotesWith(
                        currentKey: SecretKey,
                        newKey: SecretKey
                    ): VaultNoteRewrapTransaction {
                        throw CancellationException("cancel rewrap")
                    }
                }
            )
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            val originalHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]

            var thrown: Throwable? = null
            try {
                repository.changePin("1111".toCharArray(), "2222".toCharArray())
            } catch (error: Throwable) {
                thrown = error
            }

            assertTrue(thrown is CancellationException)
            assertEquals(
                originalHash,
                dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]
            )
            repository.lock()
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            assertFalse(repository.unlockWithPin("2222".toCharArray()))
        }

    @Test
    fun `changePin rewraps with new key, persists new verifier, and unlocks with new pin`() = testScope.runTest {
        val dataStore = createTestDataStore()
        val rewrapper = RecordingRewrapper()
        val repository = createRepository(dataStore, rewrapper)

        repository.configurePin("1111".toCharArray())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
        val originalSalt = dataStore.data.first()[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY]
        val originalHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]
        val originalKeyEncoded = repository.withUnlockedVaultKey { key -> key.encoded.copyOf() }

        val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.Success, result)
        assertEquals(1, rewrapper.callCount)
        assertEquals(1, rewrapper.commitCount)
        assertEquals(0, rewrapper.rollbackCount)
        // Rewrapper saw the new key, not the old one.
        val rewrappedKey = rewrapper.lastKey
        assertNotNull(rewrappedKey)
        val rewrappedEncoded = rewrappedKey!!.encoded
        assertFalse(rewrappedEncoded.contentEquals(originalKeyEncoded))

        // Persisted params updated with a freshly generated salt and hash.
        val updatedPrefs = dataStore.data.first()
        val newSalt = updatedPrefs[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY]
        val newHash = updatedPrefs[VaultRepositoryImpl.PIN_HASH_KEY]
        assertNotNull(newSalt)
        assertNotNull(newHash)
        assertNotEquals(originalSalt, newSalt)
        assertNotEquals(originalHash, newHash)
        // Persisted hash bytes never contain the new PIN in clear.
        val storedStringValues = updatedPrefs.asMap().values.filterIsInstance<String>()
        assertFalse(storedStringValues.contains("2222"))

        // Vault stays unlocked and the in-memory key matches the rewrapped one.
        assertEquals(VaultState.UNLOCKED, repository.state.first())
        val activeEncoded = repository.withUnlockedVaultKey { key -> key.encoded }
        assertTrue(activeEncoded?.contentEquals(rewrappedEncoded) == true)

        // After lock, only the new PIN unlocks.
        repository.lock()
        assertFalse(repository.unlockWithPin("1111".toCharArray()))
        assertTrue(repository.unlockWithPin("2222".toCharArray()))
    }

    @Test
    fun `changePin rolls rewrap back when DataStore commit fails`() = testScope.runTest {
        val delegate = createTestDataStore()
        val dataStore = FailingDataStore(delegate)
        val rewrapper = RecordingRewrapper()
        val repository = createRepository(dataStore, rewrapper)
        repository.configurePin("1111".toCharArray())
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
        val originalHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]
        dataStore.failNextUpdate = true

        val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

        assertSame(ChangeVaultPinResult.RewrapFailed, result)
        assertEquals(1, rewrapper.callCount)
        assertEquals(0, rewrapper.commitCount)
        assertEquals(1, rewrapper.rollbackCount)
        assertEquals(originalHash, dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY])
        repository.lock()
        assertTrue(repository.unlockWithPin("1111".toCharArray()))
        assertFalse(repository.unlockWithPin("2222".toCharArray()))
    }

    @Test
    fun `changePin preserves DataStore cancellation when rollback also fails`() =
        testScope.runTest {
            val delegate = createTestDataStore()
            val dataStore = FailingDataStore(delegate)
            val rewrapper = RecordingRewrapper(throwOnRollback = true)
            val repository = createRepository(dataStore, rewrapper)
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            dataStore.nextFailure = CancellationException("cancel PIN config commit")

            val thrown = runCatching {
                repository.changePin("1111".toCharArray(), "2222".toCharArray())
            }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            assertEquals("cancel PIN config commit", thrown?.message)
            assertEquals(1, thrown?.suppressed?.size)
            assertEquals("rollback failed", thrown?.suppressed?.single()?.message)
            assertEquals(1, rewrapper.rollbackCount)
        }

    @Test
    fun `changePin refreshes Android credential protected material with the new key`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val rewrapper = RecordingRewrapper()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                rewrapper = rewrapper,
                protectUnlockMaterial = protector::protect
            )

            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            val firstEnvelope = dataStore.data.first()[
                VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
            ]

            val result = repository.changePin("1111".toCharArray(), "2222".toCharArray())

            val secondEnvelope = dataStore.data.first()[
                VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
            ]
            val activeKeyBytes = repository.withUnlockedVaultKey { key -> key.encoded }

            assertSame(ChangeVaultPinResult.Success, result)
            assertEquals(2, protector.callCount)
            assertNotNull(firstEnvelope)
            assertNotNull(secondEnvelope)
            assertNotEquals(firstEnvelope, secondEnvelope)
            assertNotNull(activeKeyBytes)
            assertTrue(protector.materials.last().contentEquals(activeKeyBytes))
        }

    @Test
    fun `resetVault returns VaultNotConfigured when no pin is set`() = testScope.runTest {
        val wiper = RecordingWiper()
        val repository = createRepository(createTestDataStore(), wiper = wiper)

        val result = repository.resetVault()

        assertSame(ResetVaultResult.VaultNotConfigured, result)
        assertEquals(0, wiper.callCount)
    }

    @Test
    fun `resetVault returns Failed when no wiper is wired and leaves state untouched`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val repository = createRepository(dataStore, wiper = null)
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            val originalHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]
            val originalSalt = dataStore.data.first()[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY]

            val result = repository.resetVault()

            val prefs = dataStore.data.first()
            assertSame(ResetVaultResult.Failed, result)
            assertEquals(originalHash, prefs[VaultRepositoryImpl.PIN_HASH_KEY])
            assertEquals(originalSalt, prefs[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY])
            // Vault is still usable: old PIN still unlocks.
            assertEquals(VaultState.UNLOCKED, repository.state.first())
            repository.lock()
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
        }

    @Test
    fun `resetVault returns Failed when wiper throws and keeps existing PIN working`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val failingWiper = RecordingWiper(throwOnWipe = true)
            val repository = createRepository(dataStore, wiper = failingWiper)
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            val originalHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]

            val result = repository.resetVault()

            val prefs = dataStore.data.first()
            assertSame(ResetVaultResult.Failed, result)
            assertEquals(1, failingWiper.callCount)
            assertEquals(originalHash, prefs[VaultRepositoryImpl.PIN_HASH_KEY])
            // In-memory key still alive; the Vault was not touched.
            assertEquals(VaultState.UNLOCKED, repository.state.first())
            repository.lock()
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
        }

    @Test
    fun `resetVault propagates wipe cancellation and leaves configured vault usable`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val wiper = RecordingWiper(cancelOnWipe = true)
            val repository = createRepository(dataStore, wiper = wiper)
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            val originalHash = dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]

            val thrown = runCatching { repository.resetVault() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            assertEquals(1, wiper.callCount)
            assertEquals(originalHash, dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY])
            assertEquals(VaultState.UNLOCKED, repository.state.first())
        }

    @Test
    fun `resetVault propagates DataStore cancellation after wipe without reporting success`() =
        testScope.runTest {
            val delegate = createTestDataStore()
            val dataStore = FailingDataStore(delegate)
            val wiper = RecordingWiper()
            val repository = createRepository(dataStore, wiper = wiper)
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            val originalHash = delegate.data.first()[VaultRepositoryImpl.PIN_HASH_KEY]
            dataStore.nextFailure = CancellationException("cancel DataStore reset")

            val thrown = runCatching { repository.resetVault() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            assertEquals(1, wiper.callCount)
            assertEquals(originalHash, delegate.data.first()[VaultRepositoryImpl.PIN_HASH_KEY])
            assertEquals(VaultState.LOCKED, repository.state.first())
        }

    @Test
    fun `configurePin propagates DataStore cancellation`() = testScope.runTest {
        val delegate = createTestDataStore()
        val dataStore = FailingDataStore(delegate).apply {
            nextFailure = CancellationException("cancel DataStore write")
        }
        val repository = createRepository(dataStore)

        val thrown = runCatching {
            repository.configurePin("1111".toCharArray())
        }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertNull(delegate.data.first()[VaultRepositoryImpl.PIN_HASH_KEY])
        assertEquals(VaultState.NOT_CONFIGURED, repository.state.first())
    }

    @Test
    fun `resetVault wipes notes, clears persisted material and discards in-memory key`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val wiper = RecordingWiper()
            val protector = RecordingUnlockMaterialProtector()
            val repository = createRepository(
                dataStore = dataStore,
                wiper = wiper,
                protectUnlockMaterial = protector::protect
            )
            repository.configurePin("1111".toCharArray())
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
            assertNotNull(dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY])
            assertNotNull(
                dataStore.data.first()[
                    VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY
                ]
            )

            val result = repository.resetVault()

            val prefs = dataStore.data.first()
            assertSame(ResetVaultResult.Success, result)
            assertEquals(1, wiper.callCount)
            // Every Vault-owned preference key has been removed.
            assertNull(prefs[VaultRepositoryImpl.PIN_ALGORITHM_KEY])
            assertNull(prefs[VaultRepositoryImpl.PIN_ITERATIONS_KEY])
            assertNull(prefs[VaultRepositoryImpl.PIN_SALT_KEY])
            assertNull(prefs[VaultRepositoryImpl.PIN_HASH_KEY])
            assertNull(prefs[VaultRepositoryImpl.KEY_DERIVATION_ALGORITHM_KEY])
            assertNull(prefs[VaultRepositoryImpl.KEY_DERIVATION_ITERATIONS_KEY])
            assertNull(prefs[VaultRepositoryImpl.KEY_DERIVATION_SALT_KEY])
            assertNull(prefs[VaultRepositoryImpl.KEY_DERIVATION_KEY_LENGTH_BITS_KEY])
            assertNull(
                prefs[VaultRepositoryImpl.ANDROID_CREDENTIAL_PROTECTED_UNLOCK_MATERIAL_KEY]
            )
            assertFalse(repository.hasAndroidCredentialProtectedUnlockMaterial.first())
            assertEquals(VaultState.NOT_CONFIGURED, repository.state.first())
            assertNull(repository.withUnlockedVaultKey { key -> key.algorithm })
            // Previously valid PIN no longer unlocks anything: Vault is gone.
            assertFalse(repository.unlockWithPin("1111".toCharArray()))
        }

    @Test
    fun `resetVault on a locked vault is rejected and leaves configuration intact`() =
        testScope.runTest {
            val dataStore = createTestDataStore()
            val wiper = RecordingWiper()
            val repository = createRepository(dataStore, wiper = wiper)
            repository.configurePin("1111".toCharArray())
            // Stay locked: never call unlockWithPin.
            assertEquals(VaultState.LOCKED, repository.state.first())

            val result = repository.resetVault()

            assertSame(ResetVaultResult.VaultLocked, result)
            assertEquals(0, wiper.callCount)
            assertEquals(VaultState.LOCKED, repository.state.first())
            assertNotNull(dataStore.data.first()[VaultRepositoryImpl.PIN_HASH_KEY])
            assertTrue(repository.unlockWithPin("1111".toCharArray()))
        }

    /**
     * Minimal stand-in for the real [VaultNoteWiper] used by tests. It just
     * records invocations so the order between wipe and persisted-state
     * mutations can be verified without touching the database layer.
     */
    private class RecordingWiper(
        private val throwOnWipe: Boolean = false,
        private val cancelOnWipe: Boolean = false,
        private val rowsRemoved: Int = 0
    ) : VaultNoteWiper {
        var callCount: Int = 0
            private set

        override suspend fun wipeAllVaultNotes(): Int {
            callCount += 1
            if (cancelOnWipe) {
                throw CancellationException("wipe cancelled")
            }
            if (throwOnWipe) {
                throw IllegalStateException("wipe failed")
            }
            return rowsRemoved
        }
    }

    /**
     * Minimal stand-in for the real [VaultNoteRewrapper] used by tests. It
     * just records invocations and the key it was handed, so we can verify
     * ordering and key identity without touching the database layer.
     */
    private class RecordingRewrapper(
        private val throwOnRewrap: Boolean = false,
        private val throwOnRollback: Boolean = false
    ) : VaultNoteRewrapper {
        var callCount: Int = 0
            private set
        var lastKey: SecretKey? = null
            private set
        var commitCount: Int = 0
            private set
        var rollbackCount: Int = 0
            private set

        override suspend fun rewrapAllVaultNotesWith(
            currentKey: SecretKey,
            newKey: SecretKey
        ): VaultNoteRewrapTransaction {
            callCount += 1
            lastKey = newKey
            if (throwOnRewrap) {
                throw IllegalStateException("rewrap failed")
            }
            return object : VaultNoteRewrapTransaction {
                override suspend fun commit() {
                    commitCount += 1
                }

                override suspend fun rollback() {
                    rollbackCount += 1
                    if (throwOnRollback) {
                        throw IllegalStateException("rollback failed")
                    }
                }
            }
        }
    }

    private class FailingDataStore(
        private val delegate: DataStore<Preferences>
    ) : DataStore<Preferences> {
        var failNextUpdate: Boolean = false
        var nextFailure: Throwable? = null

        override val data: Flow<Preferences>
            get() = delegate.data

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences {
            nextFailure?.let { failure ->
                nextFailure = null
                throw failure
            }
            if (failNextUpdate) {
                failNextUpdate = false
                throw IllegalStateException("forced DataStore failure")
            }
            return delegate.updateData(transform)
        }
    }

    private class RecordingUnlockMaterialProtector(
        private val unprotectResult: UnprotectVaultUnlockMaterialResult? = null
    ) {
        private val _materials = mutableListOf<ByteArray>()
        private val _protectedMaterials = mutableListOf<VaultAndroidCredentialProtectedMaterial>()

        val materials: List<ByteArray>
            get() = _materials
        val protectedMaterials: List<VaultAndroidCredentialProtectedMaterial>
            get() = _protectedMaterials
        var callCount: Int = 0
            private set
        var unprotectCallCount: Int = 0
            private set

        fun protect(material: ByteArray): ProtectVaultUnlockMaterialResult {
            callCount += 1
            _materials += material.copyOf()
            val protectedMaterial = VaultAndroidCredentialProtectedMaterial(
                version = 1,
                algorithm = "AES/GCM/NoPadding",
                keyAlias = "nexnote_vault_android_credential_unlock_v1",
                ivBase64 = encode("iv-$callCount"),
                ciphertextBase64 = encode("ciphertext-$callCount")
            )
            _protectedMaterials += protectedMaterial
            return ProtectVaultUnlockMaterialResult.Success(protectedMaterial)
        }

        fun unprotect(encodedMaterial: String): UnprotectVaultUnlockMaterialResult {
            unprotectCallCount += 1
            unprotectResult?.let { return it }

            val protectedMaterial = _protectedMaterials.firstOrNull {
                it.encode() == encodedMaterial
            } ?: return UnprotectVaultUnlockMaterialResult.InvalidPayload
            val materialIndex = _protectedMaterials.indexOf(protectedMaterial)
            val material = _materials.getOrNull(materialIndex)
                ?: return UnprotectVaultUnlockMaterialResult.InvalidPayload

            return UnprotectVaultUnlockMaterialResult.Success(material.copyOf())
        }

        fun stubProtectedMaterial(): VaultAndroidCredentialProtectedMaterial =
            VaultAndroidCredentialProtectedMaterial(
                version = 1,
                algorithm = "AES/GCM/NoPadding",
                keyAlias = "nexnote_vault_android_credential_unlock_v1",
                ivBase64 = encode("stub-iv"),
                ciphertextBase64 = encode("stub-ciphertext")
            )

        private fun encode(value: String): String =
            Base64.getEncoder().encodeToString(value.toByteArray())
    }

    companion object {
        private const val TEST_ITERATIONS = 1_000
    }
}
