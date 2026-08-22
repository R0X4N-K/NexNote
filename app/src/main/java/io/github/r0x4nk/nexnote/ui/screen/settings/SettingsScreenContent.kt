package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton

internal const val SETTINGS_VAULT_CURRENT_PIN_FIELD_TAG = "settings_vault_current_pin_field"
internal const val SETTINGS_VAULT_NEW_PIN_FIELD_TAG = "settings_vault_new_pin_field"
internal const val SETTINGS_VAULT_CONFIRM_PIN_FIELD_TAG = "settings_vault_confirm_pin_field"
internal const val SETTINGS_VAULT_CHANGE_PIN_BUTTON_TAG = "settings_vault_change_pin_button"
internal const val SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG =
    "settings_vault_android_credential_switch"
internal const val SETTINGS_VAULT_AUTO_LOCK_TIMEOUT_ROW_TAG =
    "settings_vault_auto_lock_timeout_row"
internal const val SETTINGS_VAULT_RESET_ROW_TAG = "settings_vault_reset_row"
internal const val SETTINGS_VAULT_RESET_CONFIRM_BUTTON_TAG =
    "settings_vault_reset_confirm_button"
internal const val SETTINGS_VAULT_RESET_CANCEL_BUTTON_TAG =
    "settings_vault_reset_cancel_button"
internal const val SETTINGS_DELETE_ALL_NOTES_ROW_TAG = "settings_delete_all_notes_row"
internal const val SETTINGS_DELETE_ALL_NOTES_PIN_FIELD_TAG =
    "settings_delete_all_notes_pin_field"
internal const val SETTINGS_DELETE_ALL_NOTES_CONFIRM_BUTTON_TAG =
    "settings_delete_all_notes_confirm_button"
internal const val SETTINGS_LIST_TAG = "settings_list"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    vaultPinChangeState: SettingsVaultPinChangeUiState = SettingsVaultPinChangeUiState(),
    vaultResetState: SettingsVaultResetUiState = SettingsVaultResetUiState(),
    deleteAllNotesState: SettingsDeleteAllNotesUiState = SettingsDeleteAllNotesUiState(),
    statisticsIndexState: NoteStatisticsIndexState = NoteStatisticsIndexState(),
    floatingBottomPadding: Dp = 0.dp,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onFontScaleChange: (FontScale) -> Unit,
    onNoteCardStyleChange: (NoteCardStyle) -> Unit,
    onTableLayoutModeChange: (TableLayoutMode) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onRebuildStatisticsIndex: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onLockVault: () -> Unit = {},
    onProtectVaultRecentPreviewsChange: (Boolean) -> Unit = {},
    onLockVaultOnBackgroundChange: (Boolean) -> Unit = {},
    onVaultAutoLockTimeoutChange: (VaultAutoLockTimeout) -> Unit = {},
    onUnlockVaultWithAndroidCredentialChange: (Boolean) -> Unit = {},
    onChangeVaultPin: (CharArray, CharArray, CharArray) -> Unit = { _, _, _ -> },
    onClearVaultPinChangeFeedback: () -> Unit = {},
    onRequestVaultReset: () -> Unit = {},
    onCancelVaultReset: () -> Unit = {},
    onConfirmVaultReset: () -> Unit = {},
    onClearVaultResetFeedback: () -> Unit = {},
    onRequestDeleteAllNotes: () -> Unit = {},
    onCancelDeleteAllNotes: () -> Unit = {},
    onConfirmDeleteAllNotes: (CharArray) -> Unit = {},
    onClearDeleteAllNotesFeedback: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = nexTopAppBarColors()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsList(
                uiState = uiState,
                vaultPinChangeState = vaultPinChangeState,
                vaultResetState = vaultResetState,
                deleteAllNotesState = deleteAllNotesState,
                statisticsIndexState = statisticsIndexState,
                listState = listState,
                bottomContentPadding = floatingBottomPadding,
                modifier = Modifier.fillMaxSize(),
                onThemeModeChange = onThemeModeChange,
                onAccentColorChange = onAccentColorChange,
                onFontScaleChange = onFontScaleChange,
                onNoteCardStyleChange = onNoteCardStyleChange,
                onTableLayoutModeChange = onTableLayoutModeChange,
                onTimezoneChange = onTimezoneChange,
                onRebuildStatisticsIndex = onRebuildStatisticsIndex,
                onOpenVault = onOpenVault,
                onLockVault = onLockVault,
                onProtectVaultRecentPreviewsChange = onProtectVaultRecentPreviewsChange,
                onLockVaultOnBackgroundChange = onLockVaultOnBackgroundChange,
                onVaultAutoLockTimeoutChange = onVaultAutoLockTimeoutChange,
                onUnlockVaultWithAndroidCredentialChange =
                    onUnlockVaultWithAndroidCredentialChange,
                onChangeVaultPin = onChangeVaultPin,
                onClearVaultPinChangeFeedback = onClearVaultPinChangeFeedback,
                onRequestVaultReset = onRequestVaultReset,
                onCancelVaultReset = onCancelVaultReset,
                onConfirmVaultReset = onConfirmVaultReset,
                onClearVaultResetFeedback = onClearVaultResetFeedback,
                onRequestDeleteAllNotes = onRequestDeleteAllNotes,
                onClearDeleteAllNotesFeedback = onClearDeleteAllNotesFeedback
            )
            ScrollToTopButton(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = floatingBottomPadding + 16.dp
                    )
            )
            if (deleteAllNotesState.isConfirmationVisible) {
                DeleteAllNotesConfirmationDialog(
                    state = deleteAllNotesState,
                    onConfirm = onConfirmDeleteAllNotes,
                    onDismiss = onCancelDeleteAllNotes,
                    onClearFeedback = onClearDeleteAllNotesFeedback
                )
            }
        }
    }
}

@Composable
private fun SettingsList(
    uiState: SettingsUiState,
    vaultPinChangeState: SettingsVaultPinChangeUiState,
    vaultResetState: SettingsVaultResetUiState,
    deleteAllNotesState: SettingsDeleteAllNotesUiState,
    statisticsIndexState: NoteStatisticsIndexState,
    listState: LazyListState,
    bottomContentPadding: Dp,
    modifier: Modifier,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onFontScaleChange: (FontScale) -> Unit,
    onNoteCardStyleChange: (NoteCardStyle) -> Unit,
    onTableLayoutModeChange: (TableLayoutMode) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onRebuildStatisticsIndex: () -> Unit,
    onOpenVault: () -> Unit,
    onLockVault: () -> Unit,
    onProtectVaultRecentPreviewsChange: (Boolean) -> Unit,
    onLockVaultOnBackgroundChange: (Boolean) -> Unit,
    onVaultAutoLockTimeoutChange: (VaultAutoLockTimeout) -> Unit,
    onUnlockVaultWithAndroidCredentialChange: (Boolean) -> Unit,
    onChangeVaultPin: (CharArray, CharArray, CharArray) -> Unit,
    onClearVaultPinChangeFeedback: () -> Unit,
    onRequestVaultReset: () -> Unit,
    onCancelVaultReset: () -> Unit,
    onConfirmVaultReset: () -> Unit,
    onClearVaultResetFeedback: () -> Unit,
    onRequestDeleteAllNotes: () -> Unit,
    onClearDeleteAllNotesFeedback: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier.testTag(SETTINGS_LIST_TAG),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = bottomContentPadding + 12.dp
        )
    ) {
        appearanceSection(uiState.themeMode, onThemeModeChange)
        accentColorSection(uiState.accentColor, onAccentColorChange)
        textSection(uiState.fontScale, onFontScaleChange)
        noteAppearanceSection(uiState.noteCardStyle, onNoteCardStyleChange)
        tableLayoutSection(uiState.tableLayoutMode, onTableLayoutModeChange)
        vaultSection(
            vaultState = uiState.vaultState,
            canChangePin = uiState.canChangeVaultPin,
            pinChangeState = vaultPinChangeState,
            resetState = vaultResetState,
            protectRecentPreviews = uiState.protectVaultRecentPreviews,
            lockOnBackground = uiState.lockVaultOnBackground,
            autoLockTimeout = uiState.vaultAutoLockTimeout,
            unlockWithAndroidCredential = uiState.unlockVaultWithAndroidCredential,
            canConfigureAndroidCredentialUnlock =
                uiState.canConfigureAndroidCredentialUnlock,
            onOpenVault = onOpenVault,
            onLockVault = onLockVault,
            onProtectRecentPreviewsChange = onProtectVaultRecentPreviewsChange,
            onLockOnBackgroundChange = onLockVaultOnBackgroundChange,
            onAutoLockTimeoutChange = onVaultAutoLockTimeoutChange,
            onUnlockWithAndroidCredentialChange =
                onUnlockVaultWithAndroidCredentialChange,
            onChangePin = onChangeVaultPin,
            onClearPinChangeFeedback = onClearVaultPinChangeFeedback,
            onRequestReset = onRequestVaultReset,
            onCancelReset = onCancelVaultReset,
            onConfirmReset = onConfirmVaultReset,
            onClearResetFeedback = onClearVaultResetFeedback
        )
        statisticsIndexSection(statisticsIndexState, onRebuildStatisticsIndex)
        storedNotesSection(
            state = deleteAllNotesState,
            onRequestDeleteAllNotes = onRequestDeleteAllNotes,
            onClearFeedback = onClearDeleteAllNotesFeedback
        )
        developerToolsSection()
        timezoneSection(uiState, onTimezoneChange)
    }
}

private fun LazyListScope.storedNotesSection(
    state: SettingsDeleteAllNotesUiState,
    onRequestDeleteAllNotes: () -> Unit,
    onClearFeedback: () -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Storage")
            Spacer(Modifier.height(6.dp))
            DeleteAllNotesRow(
                state = state,
                onClick = onRequestDeleteAllNotes
            )
            DeleteAllNotesFeedback(
                state = state,
                onClearFeedback = onClearFeedback
            )
        }
    }
}

private fun LazyListScope.statisticsIndexSection(
    state: NoteStatisticsIndexState,
    onRebuild: () -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Indexing")
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    state.isRetryingAfterError ->
                        "Indexing was interrupted. Retrying in background…"
                    state.isIndexing ->
                        "Indexing ${state.indexedNotes} of ${state.totalNotes} notes in background."
                    else ->
                        "Up to date · ${state.totalNotes} notes processed"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRebuild,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                if (state.isIndexing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                    )
                    Text("  Restart indexing")
                } else {
                    Text("Reindex notes")
                }
            }
        }
    }
}

private fun LazyListScope.appearanceSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Appearance")
            Spacer(Modifier.height(10.dp))
            ThemeModePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.accentColorSection(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Accent color")
            Spacer(Modifier.height(14.dp))
            AccentColorPicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.textSection(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Text")
            Spacer(Modifier.height(10.dp))
            FontScalePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.noteAppearanceSection(
    selected: NoteCardStyle,
    onSelect: (NoteCardStyle) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Note appearance")
            Spacer(Modifier.height(10.dp))
            NoteCardStylePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.tableLayoutSection(
    selected: TableLayoutMode,
    onSelect: (TableLayoutMode) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Tables")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Wrap table text to fit the screen or keep wider columns and scroll horizontally.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            TableLayoutModePicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.vaultSection(
    vaultState: VaultState,
    canChangePin: Boolean,
    pinChangeState: SettingsVaultPinChangeUiState,
    resetState: SettingsVaultResetUiState,
    protectRecentPreviews: Boolean,
    lockOnBackground: Boolean,
    autoLockTimeout: VaultAutoLockTimeout,
    unlockWithAndroidCredential: Boolean,
    canConfigureAndroidCredentialUnlock: Boolean,
    onOpenVault: () -> Unit,
    onLockVault: () -> Unit,
    onProtectRecentPreviewsChange: (Boolean) -> Unit,
    onLockOnBackgroundChange: (Boolean) -> Unit,
    onAutoLockTimeoutChange: (VaultAutoLockTimeout) -> Unit,
    onUnlockWithAndroidCredentialChange: (Boolean) -> Unit,
    onChangePin: (CharArray, CharArray, CharArray) -> Unit,
    onClearPinChangeFeedback: () -> Unit,
    onRequestReset: () -> Unit,
    onCancelReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onClearResetFeedback: () -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Vault")
            Spacer(Modifier.height(10.dp))
            VaultSettingsRow(
                vaultState = vaultState,
                onClick = onOpenVault
            )
            Spacer(Modifier.height(4.dp))
            VaultAndroidCredentialUnlockRow(
                vaultState = vaultState,
                unlockWithAndroidCredential = unlockWithAndroidCredential,
                enabled = canConfigureAndroidCredentialUnlock,
                onChange = onUnlockWithAndroidCredentialChange
            )
            Spacer(Modifier.height(4.dp))
            VaultRecentPreviewsRow(
                protectRecentPreviews = protectRecentPreviews,
                onChange = onProtectRecentPreviewsChange
            )
            Spacer(Modifier.height(4.dp))
            VaultLockOnBackgroundRow(
                lockOnBackground = lockOnBackground,
                onChange = onLockOnBackgroundChange
            )
            Spacer(Modifier.height(4.dp))
            VaultAutoLockTimeoutRow(
                selected = autoLockTimeout,
                onSelect = onAutoLockTimeoutChange
            )
            if (canChangePin) {
                Spacer(Modifier.height(10.dp))
                VaultChangePinForm(
                    state = pinChangeState,
                    onChangePin = onChangePin,
                    onClearFeedback = onClearPinChangeFeedback
                )
            }
            if (vaultState == VaultState.UNLOCKED) {
                Spacer(Modifier.height(4.dp))
                VaultLockRow(onClick = onLockVault)
            }
            if (vaultState == VaultState.UNLOCKED) {
                Spacer(Modifier.height(4.dp))
                VaultResetRow(
                    isBusy = resetState.isBusy,
                    onClick = onRequestReset
                )
            }
            VaultResetFeedback(
                state = resetState,
                onClearFeedback = onClearResetFeedback
            )
            if (resetState.isConfirmationVisible) {
                VaultResetConfirmationDialog(
                    isBusy = resetState.isBusy,
                    onConfirm = onConfirmReset,
                    onDismiss = onCancelReset
                )
            }
        }
    }
}

@Composable
private fun VaultSettingsRow(
    vaultState: VaultState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = vaultPrimaryLabel(vaultState),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = vaultSecondaryLabel(vaultState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun vaultPrimaryLabel(state: VaultState): String =
    when (state) {
        VaultState.NOT_CONFIGURED -> "Set up Vault"
        VaultState.LOCKED         -> "Open Vault"
        VaultState.UNLOCKED       -> "Open Vault"
    }

private fun vaultSecondaryLabel(state: VaultState): String =
    when (state) {
        VaultState.NOT_CONFIGURED -> "Not configured"
        VaultState.LOCKED         -> "Locked"
        VaultState.UNLOCKED       -> "Unlocked"
}

@Composable
private fun VaultAndroidCredentialUnlockRow(
    vaultState: VaultState,
    unlockWithAndroidCredential: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    var showDisableConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(unlockWithAndroidCredential, enabled) {
        if (!unlockWithAndroidCredential || !enabled) {
            showDisableConfirmation = false
        }
    }

    if (showDisableConfirmation) {
        VaultDisableAndroidCredentialDialog(
            onConfirm = {
                showDisableConfirmation = false
                onChange(false)
            },
            onDismiss = {
                showDisableConfirmation = false
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = "Use Android screen lock",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    vaultState == VaultState.NOT_CONFIGURED -> "Set up Vault first"
                    !enabled -> "Unlock Vault first"
                    unlockWithAndroidCredential -> "On"
                    else -> "Off"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            modifier = Modifier.testTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG),
            checked = unlockWithAndroidCredential,
            enabled = enabled,
            onCheckedChange = { checked ->
                if (checked || !unlockWithAndroidCredential) {
                    showDisableConfirmation = false
                    onChange(checked)
                } else {
                    showDisableConfirmation = true
                }
            }
        )
    }
}

@Composable
private fun VaultDisableAndroidCredentialDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disable Android screen lock?") },
        text = {
            Text(
                "You can still unlock the Vault with your PIN. Android screen lock can be enabled again after a PIN unlock."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Disable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VaultRecentPreviewsRow(
    protectRecentPreviews: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = "Protect recent previews",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (protectRecentPreviews) "On" else "Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = protectRecentPreviews,
            onCheckedChange = onChange
        )
    }
}

@Composable
private fun VaultLockOnBackgroundRow(
    lockOnBackground: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = "Lock on background",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (lockOnBackground) "On" else "Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = lockOnBackground,
            onCheckedChange = onChange
        )
    }
}

@Composable
private fun VaultAutoLockTimeoutRow(
    selected: VaultAutoLockTimeout,
    onSelect: (VaultAutoLockTimeout) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .testTag(SETTINGS_VAULT_AUTO_LOCK_TIMEOUT_ROW_TAG)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = "Auto-lock timeout",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selected.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VaultAutoLockTimeout.entries.forEach { timeout ->
                DropdownMenuItem(
                    text = { Text(timeout.label()) },
                    onClick = {
                        onSelect(timeout)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun VaultAutoLockTimeout.label(): String = when (this) {
    VaultAutoLockTimeout.IMMEDIATELY -> "Immediately"
    VaultAutoLockTimeout.AFTER_1_MINUTE -> "After 1 minute"
    VaultAutoLockTimeout.AFTER_5_MINUTES -> "After 5 minutes"
    VaultAutoLockTimeout.AFTER_15_MINUTES -> "After 15 minutes"
    VaultAutoLockTimeout.AFTER_30_MINUTES -> "After 30 minutes"
    VaultAutoLockTimeout.NEVER -> "Never during this session"
}

@Composable
private fun VaultLockRow(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = "Lock Vault",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VaultResetRow(
    isBusy: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBusy, onClick = onClick)
            .testTag(SETTINGS_VAULT_RESET_ROW_TAG)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = "Reset Vault",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Erases all Vault notes and the Vault PIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VaultResetFeedback(
    state: SettingsVaultResetUiState,
    onClearFeedback: () -> Unit
) {
    if (state.error != null) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearFeedback)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = state.error.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    } else if (state.isSuccessful) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearFeedback)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "Vault reset.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VaultResetConfirmationDialog(
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        title = { Text("Reset Vault?") },
        text = {
            Text(
                "This permanently erases all notes in the Vault, the Vault PIN " +
                    "and any Android screen lock material. This cannot be undone."
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(SETTINGS_VAULT_RESET_CONFIRM_BUTTON_TAG),
                enabled = !isBusy,
                onClick = onConfirm
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(SETTINGS_VAULT_RESET_CANCEL_BUTTON_TAG),
                enabled = !isBusy,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun SettingsVaultResetError.message(): String = when (this) {
    SettingsVaultResetError.VAULT_NOT_CONFIGURED -> "Vault is not configured."
    SettingsVaultResetError.VAULT_LOCKED -> "Unlock the Vault before resetting it."
    SettingsVaultResetError.OPERATION_FAILED -> "Vault reset failed."
}

@Composable
private fun DeleteAllNotesRow(
    state: SettingsDeleteAllNotesUiState,
    onClick: () -> Unit
) {
    val hasNotes = state.totalNoteCount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasNotes && !state.isBusy, onClick = onClick)
            .testTag(SETTINGS_DELETE_ALL_NOTES_ROW_TAG)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = "Delete all notes",
                style = MaterialTheme.typography.bodyLarge,
                color = if (hasNotes) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = when {
                    !hasNotes -> "There are no notes to delete."
                    state.vaultNoteCount > 0 ->
                        "${state.totalNoteCount} notes, including trash and Vault."
                    else -> "${state.totalNoteCount} notes, including trash."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )
        } else if (hasNotes) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeleteAllNotesFeedback(
    state: SettingsDeleteAllNotesUiState,
    onClearFeedback: () -> Unit
) {
    val message = when {
        state.error != null -> state.error.message()
        state.isSuccessful -> "All notes were permanently deleted."
        else -> null
    } ?: return
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClearFeedback)
            .padding(vertical = 4.dp),
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.error != null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    )
}

@Composable
private fun DeleteAllNotesConfirmationDialog(
    state: SettingsDeleteAllNotesUiState,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
    onClearFeedback: () -> Unit
) {
    var vaultPin by remember { mutableStateOf("") }
    LaunchedEffect(state.isConfirmationVisible) {
        if (!state.isConfirmationVisible) vaultPin = ""
    }
    AlertDialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = { Text("Delete all notes?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This permanently deletes all normal notes and all notes in " +
                        "the trash. Templates and settings are preserved."
                )
                if (state.requiresVaultAuthentication) {
                    Text(
                        "${state.vaultNoteCount} Vault notes will also be deleted. " +
                            "Enter the Vault PIN to authorize this action."
                    )
                    SettingsVaultPinField(
                        value = vaultPin,
                        label = "Vault PIN",
                        enabled = !state.isBusy,
                        isError = state.error == SettingsDeleteAllNotesError.EMPTY_VAULT_PIN ||
                            state.error == SettingsDeleteAllNotesError.WRONG_VAULT_PIN,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.testTag(SETTINGS_DELETE_ALL_NOTES_PIN_FIELD_TAG),
                        onValueChange = {
                            vaultPin = it
                            onClearFeedback()
                        },
                        onDone = {
                            if (!state.isBusy) onConfirm(vaultPin.toCharArray())
                        }
                    )
                }
                state.error?.let { error ->
                    Text(
                        text = error.message(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(SETTINGS_DELETE_ALL_NOTES_CONFIRM_BUTTON_TAG),
                enabled = !state.isBusy,
                onClick = { onConfirm(vaultPin.toCharArray()) }
            ) {
                Text("Delete permanently")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !state.isBusy,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun SettingsDeleteAllNotesError.message(): String = when (this) {
    SettingsDeleteAllNotesError.EMPTY_VAULT_PIN -> "Enter the Vault PIN."
    SettingsDeleteAllNotesError.WRONG_VAULT_PIN -> "Incorrect Vault PIN."
    SettingsDeleteAllNotesError.OPERATION_FAILED -> "Could not delete all notes."
}

@Composable
private fun VaultChangePinForm(
    state: SettingsVaultPinChangeUiState,
    onChangePin: (CharArray, CharArray, CharArray) -> Unit,
    onClearFeedback: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Change PIN",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        SettingsVaultPinField(
            value = currentPin,
            label = "Current PIN",
            enabled = !state.isBusy,
            isError = state.error != null,
            imeAction = ImeAction.Next,
            modifier = Modifier.testTag(SETTINGS_VAULT_CURRENT_PIN_FIELD_TAG),
            onValueChange = {
                currentPin = it
                onClearFeedback()
            },
            onDone = {}
        )
        SettingsVaultPinField(
            value = newPin,
            label = "New PIN",
            enabled = !state.isBusy,
            isError = state.error != null,
            imeAction = ImeAction.Next,
            modifier = Modifier.testTag(SETTINGS_VAULT_NEW_PIN_FIELD_TAG),
            onValueChange = {
                newPin = it
                onClearFeedback()
            },
            onDone = {}
        )
        SettingsVaultPinField(
            value = confirmation,
            label = "Confirm new PIN",
            enabled = !state.isBusy,
            isError = state.error != null,
            imeAction = ImeAction.Done,
            modifier = Modifier.testTag(SETTINGS_VAULT_CONFIRM_PIN_FIELD_TAG),
            onValueChange = {
                confirmation = it
                onClearFeedback()
            },
            onDone = {
                submitVaultPinChange(
                    currentPin = currentPin,
                    newPin = newPin,
                    confirmation = confirmation,
                    onClearFields = {
                        currentPin = ""
                        newPin = ""
                        confirmation = ""
                    },
                    onChangePin = onChangePin
                )
            }
        )
        state.error?.let { error ->
            Text(
                text = error.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (state.isSuccessful) {
            Text(
                text = "PIN changed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SETTINGS_VAULT_CHANGE_PIN_BUTTON_TAG),
            enabled = !state.isBusy,
            onClick = {
                submitVaultPinChange(
                    currentPin = currentPin,
                    newPin = newPin,
                    confirmation = confirmation,
                    onClearFields = {
                        currentPin = ""
                        newPin = ""
                        confirmation = ""
                    },
                    onChangePin = onChangePin
                )
            },
            shape = MaterialTheme.shapes.extraLarge
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
            } else {
                Text("Change PIN")
            }
        }
    }
}

@Composable
private fun SettingsVaultPinField(
    value: String,
    label: String,
    enabled: Boolean,
    isError: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}

private fun submitVaultPinChange(
    currentPin: String,
    newPin: String,
    confirmation: String,
    onClearFields: () -> Unit,
    onChangePin: (CharArray, CharArray, CharArray) -> Unit
) {
    val currentPinChars = currentPin.toCharArray()
    val newPinChars = newPin.toCharArray()
    val confirmationChars = confirmation.toCharArray()
    onClearFields()
    try {
        onChangePin(currentPinChars, newPinChars, confirmationChars)
    } finally {
        currentPinChars.fill('\u0000')
        newPinChars.fill('\u0000')
        confirmationChars.fill('\u0000')
    }
}

private fun SettingsVaultPinChangeError.message(): String = when (this) {
    SettingsVaultPinChangeError.EMPTY_CURRENT_PIN -> "Enter your current PIN."
    SettingsVaultPinChangeError.EMPTY_NEW_PIN -> "Enter a new PIN."
    SettingsVaultPinChangeError.PIN_MISMATCH -> "New PINs do not match."
    SettingsVaultPinChangeError.VAULT_NOT_CONFIGURED -> "Set up the Vault first."
    SettingsVaultPinChangeError.VAULT_LOCKED -> "Unlock the Vault before changing the PIN."
    SettingsVaultPinChangeError.WRONG_CURRENT_PIN -> "Wrong current PIN."
    SettingsVaultPinChangeError.OPERATION_FAILED -> "PIN change failed."
}

private fun LazyListScope.timezoneSection(
    uiState: SettingsUiState,
    onSelect: (String) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader("Timezone")
            Spacer(Modifier.height(10.dp))
            TimezoneDropdown(
                selectedId = uiState.timezoneId,
                availableTimezones = uiState.availableTimezones,
                onSelect = onSelect
            )
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
internal fun SettingsSectionSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}
