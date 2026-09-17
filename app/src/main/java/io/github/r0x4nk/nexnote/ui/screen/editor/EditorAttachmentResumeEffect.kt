package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.ui.screen.vault.VaultAccessViewModel
import io.github.r0x4nk.nexnote.ui.screen.vault.VaultAndroidCredentialPromptCoordinator
import io.github.r0x4nk.nexnote.ui.screen.vault.VaultUnlockForm
import kotlinx.coroutines.flow.combine

@Composable
internal fun EditorAttachmentResumeEffect(editor: EditorViewModel) {
    val request = editor.pendingAttachment
    val owner = LocalLifecycleOwner.current
    val hasResult by request.hasResult.collectAsStateWithLifecycle()
    val vaultState by request.vaultState.collectAsStateWithLifecycle()
    LaunchedEffect(owner, request) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            combine(request.hasResult, request.vaultState) { pending, _ -> pending }
                .collect { if (it) request.resume() }
        }
    }
    if (hasResult && vaultState == VaultState.LOCKED) {
        val access: VaultAccessViewModel = viewModel(
            key = "attachment-vault-access", factory = VaultAccessViewModel.Factory
        )
        val accessState by access.uiState.collectAsStateWithLifecycle()
        VaultAndroidCredentialPromptCoordinator(
            requestId = accessState.androidCredentialPromptRequestId,
            isPromptPending = accessState.isAndroidCredentialPromptPending,
            onPromptResult = access::onAndroidCredentialPromptResult
        )
        AlertDialog(
            tonalElevation = 1.dp,
            onDismissRequest = request::cancel,
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.editor_attachment_vault_locked))
                    VaultUnlockForm(
                        uiState = accessState,
                        onUnlockWithPin = access::unlockWithPin,
                        onRequestAndroidCredentialPrompt = access::requestAndroidCredentialPrompt,
                        onClearError = access::clearError
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = request::cancel) {
                    Text(stringResource(R.string.editor_cancel_attachment))
                }
            }
        )
    }
}
