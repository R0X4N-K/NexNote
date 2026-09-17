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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
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
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.AppFont
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.NoteStatisticsIndexState
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton
import io.github.r0x4nk.nexnote.ui.component.GroupedItemPosition
import io.github.r0x4nk.nexnote.ui.component.GroupedListItem
import io.github.r0x4nk.nexnote.ui.component.NoteCardContent
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

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
internal const val SETTINGS_SOURCE_CODE_ROW_TAG = "settings_source_code_row"
internal const val SETTINGS_NOTE_PREVIEW_TAG = "settings_note_preview"

private const val NOTE_CARD_PREVIEW_ID = -1L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    versionName: String,
    vaultPinChangeState: SettingsVaultPinChangeUiState = SettingsVaultPinChangeUiState(),
    vaultResetState: SettingsVaultResetUiState = SettingsVaultResetUiState(),
    deleteAllNotesState: SettingsDeleteAllNotesUiState = SettingsDeleteAllNotesUiState(),
    statisticsIndexState: NoteStatisticsIndexState = NoteStatisticsIndexState(),
    floatingBottomPadding: Dp = 0.dp,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit = {},
    onAccentColorChange: (AccentColor) -> Unit,
    onAppFontChange: (AppFont) -> Unit = {},
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
    onClearDeleteAllNotesFeedback: () -> Unit = {},
    onOpenSourceCode: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                onDynamicColorChange = onDynamicColorChange,
                onAccentColorChange = onAccentColorChange,
                onAppFontChange = onAppFontChange,
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
                onClearVaultResetFeedback = onClearVaultResetFeedback,
                onRequestDeleteAllNotes = onRequestDeleteAllNotes,
                onClearDeleteAllNotesFeedback = onClearDeleteAllNotesFeedback,
                versionName = versionName,
                onOpenSourceCode = onOpenSourceCode
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
            if (vaultResetState.isConfirmationVisible) {
                VaultResetConfirmationDialog(
                    isBusy = vaultResetState.isBusy,
                    onConfirm = onConfirmVaultReset,
                    onDismiss = onCancelVaultReset
                )
            }
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
    onDynamicColorChange: (Boolean) -> Unit = {},
    onAccentColorChange: (AccentColor) -> Unit,
    onAppFontChange: (AppFont) -> Unit,
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
    onClearVaultResetFeedback: () -> Unit,
    onRequestDeleteAllNotes: () -> Unit,
    onClearDeleteAllNotesFeedback: () -> Unit,
    versionName: String,
    onOpenSourceCode: () -> Unit
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
        item {
            SettingsSectionSurface(GroupedItemPosition.MIDDLE) {
                DeviceColorsPreference(uiState.dynamicColor, onDynamicColorChange)
            }
        }
        if (!uiState.dynamicColor || android.os.Build.VERSION.SDK_INT < 31) {
            accentColorSection(uiState.accentColor, onAccentColorChange)
        }
        fontFamilySection(uiState.appFont, onAppFontChange)
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
        aboutSection(versionName, onOpenSourceCode)
    }
}

private fun LazyListScope.storedNotesSection(
    state: SettingsDeleteAllNotesUiState,
    onRequestDeleteAllNotes: () -> Unit,
    onClearFeedback: () -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader(stringResource(R.string.settings_storage))
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
            SettingsSectionHeader(stringResource(R.string.settings_indexing))
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    state.isRetryingAfterError ->
                        stringResource(R.string.settings_index_interrupted)
                    state.isIndexing ->
                        stringResource(
                            R.string.settings_index_progress,
                            state.indexedNotes,
                            state.totalNotes
                        )
                    else ->
                        stringResource(R.string.settings_index_up_to_date, state.totalNotes)
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
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_restart_indexing))
                } else {
                    Text(stringResource(R.string.settings_reindex_notes))
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
        SettingsSectionSurface(GroupedItemPosition.FIRST) {
            SettingsSectionHeader(stringResource(R.string.settings_appearance))
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
        SettingsSectionSurface(GroupedItemPosition.MIDDLE) {
            SettingsSectionHeader(stringResource(R.string.settings_accent_color))
            Spacer(Modifier.height(14.dp))
            AccentColorPicker(
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.fontFamilySection(
    selected: AppFont,
    onSelect: (AppFont) -> Unit
) {
    item {
        SettingsSectionSurface(GroupedItemPosition.MIDDLE) {
            SettingsSectionHeader(stringResource(R.string.settings_font_family))
            Spacer(Modifier.height(10.dp))
            AppFontPicker(
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
        SettingsSectionSurface(GroupedItemPosition.MIDDLE) {
            SettingsSectionHeader(stringResource(R.string.settings_text))
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
        SettingsSectionSurface(GroupedItemPosition.MIDDLE) {
            SettingsSectionHeader(stringResource(R.string.settings_note_appearance))
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_note_appearance_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            NoteCardStylePicker(
                selected = selected,
                onSelect = onSelect
            )
            Spacer(Modifier.height(14.dp))
            NoteCardStylePreview(style = selected)
        }
    }
}

/**
 * Live sample of the selected card style so the setting has a visible effect.
 * It reuses the real card body, including the footer metadata (tags and files),
 * instead of duplicating the layout.
 */
@Composable
private fun NoteCardStylePreview(style: NoteCardStyle) {
    val previewTitle = stringResource(R.string.settings_note_card_preview_title)
    val previewContent = stringResource(R.string.settings_note_card_preview_content)
    val sample = remember(previewTitle, previewContent) {
        Note(
            id = NOTE_CARD_PREVIEW_ID,
            title = previewTitle,
            content = previewContent,
            imagePaths = listOf("images/attachments/release-notes.pdf")
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SETTINGS_NOTE_PREVIEW_TAG)
    ) {
        NoteCardContent(
            note = sample,
            onClick = {},
            onLongPress = {},
            selectionMode = false,
            selected = false,
            noteCardStyle = style,
            titleHighlightRanges = emptyList(),
            contentHighlightRanges = emptyList()
        )
    }
}

private fun LazyListScope.tableLayoutSection(
    selected: TableLayoutMode,
    onSelect: (TableLayoutMode) -> Unit
) {
    item {
        SettingsSectionSurface(GroupedItemPosition.LAST) {
            SettingsSectionHeader(stringResource(R.string.settings_tables))
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_tables_hint),
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
    onClearResetFeedback: () -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader(stringResource(R.string.settings_vault))
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

@Composable
private fun vaultPrimaryLabel(state: VaultState): String =
    when (state) {
        VaultState.NOT_CONFIGURED -> stringResource(R.string.settings_vault_setup)
        VaultState.LOCKED         -> stringResource(R.string.settings_vault_open)
        VaultState.UNLOCKED       -> stringResource(R.string.settings_vault_open)
    }

@Composable
private fun vaultSecondaryLabel(state: VaultState): String =
    when (state) {
        VaultState.NOT_CONFIGURED -> stringResource(R.string.settings_vault_not_configured)
        VaultState.LOCKED         -> stringResource(R.string.settings_vault_locked)
        VaultState.UNLOCKED       -> stringResource(R.string.settings_vault_unlocked)
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
                text = stringResource(R.string.settings_vault_use_android_lock),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    vaultState == VaultState.NOT_CONFIGURED ->
                        stringResource(R.string.settings_vault_setup_first)
                    !enabled -> stringResource(R.string.settings_vault_unlock_first)
                    unlockWithAndroidCredential -> stringResource(R.string.settings_on)
                    else -> stringResource(R.string.settings_off)
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
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_vault_disable_lock_title)) },
        text = {
            Text(stringResource(R.string.settings_vault_disable_lock_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_disable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
                text = stringResource(R.string.settings_vault_protect_previews),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (protectRecentPreviews) {
                    stringResource(R.string.settings_on)
                } else {
                    stringResource(R.string.settings_off)
                },
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
                text = stringResource(R.string.settings_vault_lock_background),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (lockOnBackground) {
                    stringResource(R.string.settings_on)
                } else {
                    stringResource(R.string.settings_off)
                },
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
                    text = stringResource(R.string.settings_vault_auto_lock_timeout),
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

@Composable
private fun VaultAutoLockTimeout.label(): String = when (this) {
    VaultAutoLockTimeout.IMMEDIATELY -> stringResource(R.string.settings_vault_auto_lock_immediately)
    VaultAutoLockTimeout.AFTER_1_MINUTE -> stringResource(R.string.settings_vault_auto_lock_1m)
    VaultAutoLockTimeout.AFTER_5_MINUTES -> stringResource(R.string.settings_vault_auto_lock_5m)
    VaultAutoLockTimeout.AFTER_15_MINUTES -> stringResource(R.string.settings_vault_auto_lock_15m)
    VaultAutoLockTimeout.AFTER_30_MINUTES -> stringResource(R.string.settings_vault_auto_lock_30m)
    VaultAutoLockTimeout.NEVER -> stringResource(R.string.settings_vault_auto_lock_never)
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
                text = stringResource(R.string.settings_vault_lock),
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
                text = stringResource(R.string.settings_vault_reset),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.settings_vault_reset_summary),
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
                text = stringResource(R.string.settings_vault_reset_done),
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
        tonalElevation = 1.dp,
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        title = { Text(stringResource(R.string.settings_vault_reset_title)) },
        text = {
            Text(stringResource(R.string.settings_vault_reset_message))
        },
        confirmButton = {
            NexDestructiveButton(
                modifier = Modifier.testTag(SETTINGS_VAULT_RESET_CONFIRM_BUTTON_TAG),
                enabled = !isBusy,
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.settings_reset))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(SETTINGS_VAULT_RESET_CANCEL_BUTTON_TAG),
                enabled = !isBusy,
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SettingsVaultResetError.message(): String = when (this) {
    SettingsVaultResetError.VAULT_NOT_CONFIGURED ->
        stringResource(R.string.vault_error_not_configured)
    SettingsVaultResetError.VAULT_LOCKED ->
        stringResource(R.string.settings_vault_reset_error_locked)
    SettingsVaultResetError.OPERATION_FAILED ->
        stringResource(R.string.settings_vault_reset_error_failed)
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
                text = stringResource(R.string.settings_delete_all_notes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (hasNotes) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = when {
                    !hasNotes -> stringResource(R.string.settings_delete_all_notes_empty)
                    state.vaultNoteCount > 0 ->
                        stringResource(
                            R.string.settings_delete_all_notes_count_vault,
                            state.totalNoteCount
                        )
                    else -> stringResource(
                        R.string.settings_delete_all_notes_count,
                        state.totalNoteCount
                    )
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
        state.isSuccessful -> stringResource(R.string.settings_delete_all_notes_done)
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
        tonalElevation = 1.dp,
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = { Text(stringResource(R.string.settings_delete_all_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_delete_all_message))
                if (state.requiresVaultAuthentication) {
                    Text(
                        stringResource(
                            R.string.settings_delete_all_vault_message,
                            state.vaultNoteCount
                        )
                    )
                    SettingsVaultPinField(
                        value = vaultPin,
                        label = stringResource(R.string.settings_vault_pin_label),
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
            NexDestructiveButton(
                modifier = Modifier.testTag(SETTINGS_DELETE_ALL_NOTES_CONFIRM_BUTTON_TAG),
                enabled = !state.isBusy,
                onClick = { onConfirm(vaultPin.toCharArray()) }
            ) {
                Text(stringResource(R.string.settings_delete_permanently))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !state.isBusy,
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SettingsDeleteAllNotesError.message(): String = when (this) {
    SettingsDeleteAllNotesError.EMPTY_VAULT_PIN ->
        stringResource(R.string.settings_delete_all_error_empty_pin)
    SettingsDeleteAllNotesError.WRONG_VAULT_PIN ->
        stringResource(R.string.settings_delete_all_error_wrong_pin)
    SettingsDeleteAllNotesError.OPERATION_FAILED ->
        stringResource(R.string.settings_delete_all_error_failed)
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
            text = stringResource(R.string.settings_vault_change_pin),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        SettingsVaultPinField(
            value = currentPin,
            label = stringResource(R.string.settings_vault_current_pin),
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
            label = stringResource(R.string.settings_vault_new_pin),
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
            label = stringResource(R.string.settings_vault_confirm_new_pin),
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
                text = stringResource(R.string.settings_vault_pin_changed),
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
                Text(stringResource(R.string.settings_vault_change_pin))
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

@Composable
private fun SettingsVaultPinChangeError.message(): String = when (this) {
    SettingsVaultPinChangeError.EMPTY_CURRENT_PIN ->
        stringResource(R.string.settings_vault_error_empty_current)
    SettingsVaultPinChangeError.EMPTY_NEW_PIN ->
        stringResource(R.string.settings_vault_error_empty_new)
    SettingsVaultPinChangeError.PIN_MISMATCH ->
        stringResource(R.string.settings_vault_error_mismatch)
    SettingsVaultPinChangeError.VAULT_NOT_CONFIGURED ->
        stringResource(R.string.settings_vault_error_not_configured)
    SettingsVaultPinChangeError.VAULT_LOCKED ->
        stringResource(R.string.settings_vault_error_locked)
    SettingsVaultPinChangeError.WRONG_CURRENT_PIN ->
        stringResource(R.string.settings_vault_error_wrong_current)
    SettingsVaultPinChangeError.PIN_RATE_LIMITED ->
        stringResource(R.string.vault_error_rate_limited)
    SettingsVaultPinChangeError.OPERATION_FAILED ->
        stringResource(R.string.settings_vault_error_failed)
}

private fun LazyListScope.timezoneSection(
    uiState: SettingsUiState,
    onSelect: (String) -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader(stringResource(R.string.settings_timezone))
            Spacer(Modifier.height(10.dp))
            TimezoneDropdown(
                selectedId = uiState.timezoneId,
                availableTimezones = uiState.availableTimezones,
                onSelect = onSelect
            )
        }
    }
}

private fun LazyListScope.aboutSection(
    versionName: String,
    onOpenSourceCode: () -> Unit
) {
    item {
        SettingsSectionSurface {
            SettingsSectionHeader(stringResource(R.string.settings_about))
            Spacer(Modifier.height(6.dp))
            SourceCodeRow(onClick = onOpenSourceCode)
            Spacer(Modifier.height(4.dp))
            AboutInfoRow(
                label = stringResource(R.string.settings_version),
                value = versionName
            )
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun SourceCodeRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SETTINGS_SOURCE_CODE_ROW_TAG)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_source_code),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_source_code_url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SettingsSectionSurface(
    position: GroupedItemPosition = GroupedItemPosition.ONLY,
    content: @Composable () -> Unit
) {
    val endsGroup = position == GroupedItemPosition.LAST || position == GroupedItemPosition.ONLY
    GroupedListItem(
        position = position,
        modifier = Modifier.padding(bottom = if (endsGroup) 12.dp else 2.dp)
    ) {
        content()
    }
}
