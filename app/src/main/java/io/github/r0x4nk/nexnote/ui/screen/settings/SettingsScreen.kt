package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.screen.vault.VaultAndroidCredentialPromptCoordinator

@Composable
fun SettingsScreen(
    onOpenVault: () -> Unit = {},
    floatingBottomPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vaultPinChangeState by viewModel.vaultPinChangeState.collectAsStateWithLifecycle()
    val vaultResetState by viewModel.vaultResetState.collectAsStateWithLifecycle()
    val deleteAllNotesState by viewModel.deleteAllNotesState.collectAsStateWithLifecycle()
    val statisticsIndexState by viewModel.statisticsIndexState.collectAsStateWithLifecycle()

    VaultAndroidCredentialPromptCoordinator(
        requestId = vaultPinChangeState.androidCredentialRefreshPromptRequestId,
        isPromptPending = vaultPinChangeState.isAndroidCredentialRefreshPromptPending,
        onPromptResult = viewModel::onAndroidCredentialRefreshPromptResult
    )

    SettingsScreenContent(
        uiState = uiState,
        vaultPinChangeState = vaultPinChangeState,
        vaultResetState = vaultResetState,
        deleteAllNotesState = deleteAllNotesState,
        statisticsIndexState = statisticsIndexState,
        floatingBottomPadding = floatingBottomPadding,
        onThemeModeChange = viewModel::setThemeMode,
        onAccentColorChange = viewModel::setAccentColor,
        onFontScaleChange = viewModel::setFontScale,
        onNoteCardStyleChange = viewModel::setNoteCardStyle,
        onTableLayoutModeChange = viewModel::setTableLayoutMode,
        onTimezoneChange = viewModel::setTimezoneId,
        onRebuildStatisticsIndex = viewModel::rebuildStatisticsIndex,
        onOpenVault = onOpenVault,
        onLockVault = viewModel::lockVault,
        onProtectVaultRecentPreviewsChange = viewModel::setProtectVaultRecentPreviews,
        onLockVaultOnBackgroundChange = viewModel::setLockVaultOnBackground,
        onVaultAutoLockTimeoutChange = viewModel::setVaultAutoLockTimeout,
        onUnlockVaultWithAndroidCredentialChange =
            viewModel::setUnlockVaultWithAndroidCredential,
        onChangeVaultPin = viewModel::changeVaultPin,
        onClearVaultPinChangeFeedback = viewModel::clearVaultPinChangeFeedback,
        onRequestVaultReset = viewModel::requestVaultReset,
        onCancelVaultReset = viewModel::cancelVaultReset,
        onConfirmVaultReset = viewModel::confirmVaultReset,
        onClearVaultResetFeedback = viewModel::clearVaultResetFeedback,
        onRequestDeleteAllNotes = viewModel::requestDeleteAllNotes,
        onCancelDeleteAllNotes = viewModel::cancelDeleteAllNotes,
        onConfirmDeleteAllNotes = viewModel::confirmDeleteAllNotes,
        onClearDeleteAllNotesFeedback = viewModel::clearDeleteAllNotesFeedback
    )
}
