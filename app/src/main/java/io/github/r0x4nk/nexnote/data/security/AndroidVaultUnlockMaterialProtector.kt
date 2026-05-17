package io.github.r0x4nk.nexnote.data.security

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_CREDENTIAL_ENVELOPE_PREFIX =
    "nexnote-vault-android-credential-material"
private const val ANDROID_CREDENTIAL_ENVELOPE_SEPARATOR = ':'
private const val ANDROID_CREDENTIAL_ENVELOPE_PART_COUNT = 6
private const val ANDROID_CREDENTIAL_ENVELOPE_VERSION = 1
private const val ANDROID_CREDENTIAL_GCM_TAG_LENGTH_BITS = 128

data class VaultAndroidCredentialProtectedMaterial(
    val version: Int,
    val algorithm: String,
    val keyAlias: String,
    val ivBase64: String,
    val ciphertextBase64: String
) {
    fun encode(): String =
        listOf(
            ANDROID_CREDENTIAL_ENVELOPE_PREFIX,
            version.toString(),
            algorithm,
            keyAlias,
            ivBase64,
            ciphertextBase64
        ).joinToString(ANDROID_CREDENTIAL_ENVELOPE_SEPARATOR.toString())

    companion object {
        fun decode(value: String): VaultAndroidCredentialProtectedMaterial {
            val parts = value.split(
                ANDROID_CREDENTIAL_ENVELOPE_SEPARATOR,
                limit = ANDROID_CREDENTIAL_ENVELOPE_PART_COUNT
            )
            if (
                parts.size != ANDROID_CREDENTIAL_ENVELOPE_PART_COUNT ||
                parts[0] != ANDROID_CREDENTIAL_ENVELOPE_PREFIX
            ) {
                throw VaultAndroidCredentialMaterialException(
                    "Android credential protected material envelope is invalid."
                )
            }

            val version = parts[1].toIntOrNull()
                ?: throw VaultAndroidCredentialMaterialException(
                    "Android credential protected material envelope is invalid."
                )
            val algorithm = parts[2]
            val keyAlias = parts[3]
            val ivBase64 = parts[4]
            val ciphertextBase64 = parts[5]

            if (
                algorithm.isBlank() ||
                keyAlias.isBlank() ||
                ivBase64.isBlank() ||
                ciphertextBase64.isBlank()
            ) {
                throw VaultAndroidCredentialMaterialException(
                    "Android credential protected material envelope is invalid."
                )
            }

            return VaultAndroidCredentialProtectedMaterial(
                version = version,
                algorithm = algorithm,
                keyAlias = keyAlias,
                ivBase64 = ivBase64,
                ciphertextBase64 = ciphertextBase64
            )
        }

        fun isEncoded(value: String): Boolean =
            value.startsWith(
                "$ANDROID_CREDENTIAL_ENVELOPE_PREFIX$ANDROID_CREDENTIAL_ENVELOPE_SEPARATOR"
            )
    }
}

sealed interface ProtectVaultUnlockMaterialResult {
    data class Success(
        val protectedMaterial: VaultAndroidCredentialProtectedMaterial
    ) : ProtectVaultUnlockMaterialResult

    data object EmptyMaterial : ProtectVaultUnlockMaterialResult
    data object CredentialUnavailable : ProtectVaultUnlockMaterialResult
    data object AuthenticationRequired : ProtectVaultUnlockMaterialResult
    data object KeyInvalidated : ProtectVaultUnlockMaterialResult
    data object Failed : ProtectVaultUnlockMaterialResult
}

sealed interface UnprotectVaultUnlockMaterialResult {
    data class Success(
        val material: ByteArray
    ) : UnprotectVaultUnlockMaterialResult

    data object CredentialUnavailable : UnprotectVaultUnlockMaterialResult
    data object AuthenticationRequired : UnprotectVaultUnlockMaterialResult
    data object KeyInvalidated : UnprotectVaultUnlockMaterialResult
    data object InvalidPayload : UnprotectVaultUnlockMaterialResult
    data object Failed : UnprotectVaultUnlockMaterialResult
}

class AndroidVaultUnlockMaterialProtector(
    context: Context,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val authenticationValiditySeconds: Int = DEFAULT_AUTH_VALIDITY_SECONDS
) {
    private val keyguardManager: KeyguardManager? =
        context.applicationContext.getSystemService(KeyguardManager::class.java)

    fun protect(material: ByteArray): ProtectVaultUnlockMaterialResult {
        if (material.isEmpty()) {
            return ProtectVaultUnlockMaterialResult.EmptyMaterial
        }
        if (!hasSecureAndroidCredential()) {
            return ProtectVaultUnlockMaterialResult.CredentialUnavailable
        }

        var iv = ByteArray(0)
        var ciphertext = ByteArray(0)

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            iv = cipher.iv ?: return ProtectVaultUnlockMaterialResult.Failed
            ciphertext = cipher.doFinal(material)

            ProtectVaultUnlockMaterialResult.Success(
                VaultAndroidCredentialProtectedMaterial(
                    version = ANDROID_CREDENTIAL_ENVELOPE_VERSION,
                    algorithm = TRANSFORMATION,
                    keyAlias = keyAlias,
                    ivBase64 = encode(iv),
                    ciphertextBase64 = encode(ciphertext)
                )
            )
        } catch (error: UserNotAuthenticatedException) {
            ProtectVaultUnlockMaterialResult.AuthenticationRequired
        } catch (error: KeyPermanentlyInvalidatedException) {
            deleteKey()
            ProtectVaultUnlockMaterialResult.KeyInvalidated
        } catch (error: GeneralSecurityException) {
            ProtectVaultUnlockMaterialResult.Failed
        } catch (error: Exception) {
            ProtectVaultUnlockMaterialResult.Failed
        } finally {
            iv.fill(0)
            ciphertext.fill(0)
        }
    }

    fun unprotect(encodedMaterial: String): UnprotectVaultUnlockMaterialResult {
        if (!hasSecureAndroidCredential()) {
            return UnprotectVaultUnlockMaterialResult.CredentialUnavailable
        }

        val protectedMaterial = runCatching {
            VaultAndroidCredentialProtectedMaterial.decode(encodedMaterial)
        }.getOrElse {
            return UnprotectVaultUnlockMaterialResult.InvalidPayload
        }

        if (
            protectedMaterial.version != ANDROID_CREDENTIAL_ENVELOPE_VERSION ||
            protectedMaterial.algorithm != TRANSFORMATION ||
            protectedMaterial.keyAlias != keyAlias
        ) {
            return UnprotectVaultUnlockMaterialResult.InvalidPayload
        }

        val key = getExistingKey()
            ?: return UnprotectVaultUnlockMaterialResult.KeyInvalidated
        val iv = runCatching { decode(protectedMaterial.ivBase64) }
            .getOrElse { return UnprotectVaultUnlockMaterialResult.InvalidPayload }
        val ciphertext = runCatching { decode(protectedMaterial.ciphertextBase64) }
            .getOrElse {
                iv.fill(0)
                return UnprotectVaultUnlockMaterialResult.InvalidPayload
            }
        var material = ByteArray(0)

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(ANDROID_CREDENTIAL_GCM_TAG_LENGTH_BITS, iv)
            )
            material = cipher.doFinal(ciphertext)

            UnprotectVaultUnlockMaterialResult.Success(material)
        } catch (error: UserNotAuthenticatedException) {
            UnprotectVaultUnlockMaterialResult.AuthenticationRequired
        } catch (error: KeyPermanentlyInvalidatedException) {
            deleteKey()
            UnprotectVaultUnlockMaterialResult.KeyInvalidated
        } catch (error: AEADBadTagException) {
            UnprotectVaultUnlockMaterialResult.InvalidPayload
        } catch (error: GeneralSecurityException) {
            material.fill(0)
            UnprotectVaultUnlockMaterialResult.Failed
        } catch (error: Exception) {
            material.fill(0)
            UnprotectVaultUnlockMaterialResult.Failed
        } finally {
            iv.fill(0)
            ciphertext.fill(0)
        }
    }

    private fun hasSecureAndroidCredential(): Boolean =
        keyguardManager?.isDeviceSecure == true

    private fun getExistingKey(): SecretKey? =
        try {
            loadKeyStore().getKey(keyAlias, null) as? SecretKey
        } catch (error: UnrecoverableKeyException) {
            null
        } catch (error: GeneralSecurityException) {
            null
        } catch (error: Exception) {
            null
        }

    private fun getOrCreateKey(): SecretKey {
        getExistingKey()?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        keyGenerator.init(newKeySpec())
        return keyGenerator.generateKey()
    }

    @Suppress("DEPRECATION")
    private fun newKeySpec(): KeyGenParameterSpec =
        KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(authenticationValiditySeconds)
            .build()

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private fun deleteKey() {
        runCatching { loadKeyStore().deleteEntry(keyAlias) }
    }

    private fun encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray =
        Base64.getDecoder().decode(value)

    companion object {
        const val DEFAULT_KEY_ALIAS = "nexnote_vault_android_credential_unlock_v1"
        const val DEFAULT_AUTH_VALIDITY_SECONDS = 30
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
    }
}

class VaultAndroidCredentialMaterialException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)
