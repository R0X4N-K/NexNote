package io.github.r0x4nk.nexnote.data.security

import android.app.KeyguardManager
import android.content.Context
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialAvailability
import io.github.r0x4nk.nexnote.domain.repository.VaultAndroidCredentialRepository

class AndroidVaultCredentialRepository(
    context: Context
) : VaultAndroidCredentialRepository {

    private val keyguardManager: KeyguardManager? =
        context.applicationContext.getSystemService(KeyguardManager::class.java)

    override fun getAvailability(): VaultAndroidCredentialAvailability {
        val manager = keyguardManager
            ?: return VaultAndroidCredentialAvailability.UNAVAILABLE

        return if (manager.isDeviceSecure) {
            VaultAndroidCredentialAvailability.AVAILABLE
        } else {
            VaultAndroidCredentialAvailability.LOCK_SCREEN_NOT_SECURED
        }
    }
}
