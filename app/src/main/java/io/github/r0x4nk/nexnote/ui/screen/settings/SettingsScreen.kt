package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.BuildConfig
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog
import io.github.r0x4nk.nexnote.ui.screen.vault.VaultAndroidCredentialPromptCoordinator

internal const val NEXNOTE_SOURCE_CODE_URL = "https://github.com/R0X4N-K/NexNote"

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
    val uriHandler = LocalUriHandler.current

    VaultAndroidCredentialPromptCoordinator(
        requestId = vaultPinChangeState.androidCredentialRefreshPromptRequestId,
        isPromptPending = vaultPinChangeState.isAndroidCredentialRefreshPromptPending,
        onPromptResult = viewModel::onAndroidCredentialRefreshPromptResult
    )

    SettingsScreenContent(
        uiState = uiState,
        versionName = BuildConfig.VERSION_NAME,
        vaultPinChangeState = vaultPinChangeState,
        vaultResetState = vaultResetState,
        deleteAllNotesState = deleteAllNotesState,
        statisticsIndexState = statisticsIndexState,
        floatingBottomPadding = floatingBottomPadding,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        onAccentColorChange = viewModel::setAccentColor,
        onAppFontChange = viewModel::setAppFont,
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
        onClearDeleteAllNotesFeedback = viewModel::clearDeleteAllNotesFeedback,
        onOpenSourceCode = {
            runCatching { uriHandler.openUri(NEXNOTE_SOURCE_CODE_URL) }
        }
    )
    OperationProgressDialog(
        when {
            deleteAllNotesState.isBusy -> stringResource(R.string.settings_progress_deleting)
            vaultResetState.isBusy -> stringResource(R.string.settings_progress_resetting_vault)
            vaultPinChangeState.isBusy -> stringResource(R.string.settings_progress_updating_vault)
            else -> null
        }
    )

}