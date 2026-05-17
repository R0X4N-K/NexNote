package io.github.r0x4nk.nexnote.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultAndroidCredentialProtectedMaterialTest {

    @Test
    fun `encode and decode round trip protected material envelope`() {
        val material = VaultAndroidCredentialProtectedMaterial(
            version = 1,
            algorithm = "AES/GCM/NoPadding",
            keyAlias = "nexnote_vault_android_credential_unlock_v1",
            ivBase64 = "aXY=",
            ciphertextBase64 = "Y2lwaGVydGV4dA=="
        )

        val encoded = material.encode()
        val decoded = VaultAndroidCredentialProtectedMaterial.decode(encoded)

        assertTrue(VaultAndroidCredentialProtectedMaterial.isEncoded(encoded))
        assertEquals(material, decoded)
    }

    @Test
    fun `encoded envelope does not contain plaintext material`() {
        val encoded = VaultAndroidCredentialProtectedMaterial(
            version = 1,
            algorithm = "AES/GCM/NoPadding",
            keyAlias = "nexnote_vault_android_credential_unlock_v1",
            ivBase64 = "aXY=",
            ciphertextBase64 = "Y2lwaGVydGV4dA=="
        ).encode()

        assertFalse(encoded.contains("vault key bytes"))
        assertFalse(encoded.contains("1234"))
    }

    @Test
    fun `decode rejects non vault envelope`() {
        assertFalse(VaultAndroidCredentialProtectedMaterial.isEncoded("plain text"))

        assertThrowsMaterialException {
            VaultAndroidCredentialProtectedMaterial.decode("plain text")
        }
    }

    @Test
    fun `decode rejects malformed version`() {
        assertThrowsMaterialException {
            VaultAndroidCredentialProtectedMaterial.decode(
                "nexnote-vault-android-credential-material:not-a-number:" +
                    "AES/GCM/NoPadding:alias:aXY=:Y2lwaGVydGV4dA=="
            )
        }
    }

    @Test
    fun `decode rejects blank envelope fields`() {
        assertThrowsMaterialException {
            VaultAndroidCredentialProtectedMaterial.decode(
                "nexnote-vault-android-credential-material:1:AES/GCM/NoPadding::aXY=:Y2lwaGVydGV4dA=="
            )
        }
    }

    private fun assertThrowsMaterialException(block: () -> Unit) {
        val result = runCatching(block)

        assertTrue(result.exceptionOrNull() is VaultAndroidCredentialMaterialException)
    }
}
