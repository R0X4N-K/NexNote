package io.github.r0x4nk.nexnote.ui.screen.vault

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import io.github.r0x4nk.nexnote.domain.model.VaultAndroidCredentialPromptResult

@Composable
internal fun VaultAndroidCredentialPromptCoordinator(
    requestId: Long,
    isPromptPending: Boolean,
    onPromptResult: (VaultAndroidCredentialPromptResult) -> Unit
) {
    val context = LocalContext.current
    val latestOnPromptResult by rememberUpdatedState(onPromptResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        latestOnPromptResult(
            VaultAndroidCredentialPromptResultMapper.fromActivityResult(result)
        )
    }

    LaunchedEffect(requestId, isPromptPending) {
        if (!shouldLaunchAndroidCredentialPrompt(requestId, isPromptPending)) {
            return@LaunchedEffect
        }

        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            latestOnPromptResult(VaultAndroidCredentialPromptResult.UNAVAILABLE)
            return@LaunchedEffect
        }

        val promptIntent = keyguardManager.createConfirmDeviceCredentialIntent(
            "Unlock Vault",
            "Confirm your Android screen lock to continue."
        )
        if (promptIntent == null) {
            latestOnPromptResult(VaultAndroidCredentialPromptResult.UNAVAILABLE)
            return@LaunchedEffect
        }

        try {
            launcher.launch(promptIntent)
        } catch (_: ActivityNotFoundException) {
            latestOnPromptResult(VaultAndroidCredentialPromptResult.UNAVAILABLE)
        } catch (_: SecurityException) {
            latestOnPromptResult(VaultAndroidCredentialPromptResult.FAILED)
        }
    }
}

internal fun shouldLaunchAndroidCredentialPrompt(
    requestId: Long,
    isPromptPending: Boolean
): Boolean = isPromptPending && requestId > 0L

internal object VaultAndroidCredentialPromptResultMapper {
    fun fromActivityResult(result: ActivityResult): VaultAndroidCredentialPromptResult {
        return fromResultCode(result.resultCode)
    }

    fun fromResultCode(resultCode: Int): VaultAndroidCredentialPromptResult {
        return if (resultCode == Activity.RESULT_OK) {
            VaultAndroidCredentialPromptResult.AUTHENTICATED
        } else {
            VaultAndroidCredentialPromptResult.CANCELED
        }
    }
}
