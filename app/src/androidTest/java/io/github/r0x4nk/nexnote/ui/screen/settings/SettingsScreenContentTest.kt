package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tableLayoutModePicker_submitsHorizontalScroll() {
        var submittedMode: TableLayoutMode? = null
        composeRule.setSettingsContent(
            vaultState = VaultState.NOT_CONFIGURED,
            onTableLayoutModeChange = { submittedMode = it }
        )

        composeRule.onNodeWithText("Scroll")
            .performScrollTo()
            .performClick()

        assertEquals(TableLayoutMode.HORIZONTAL_SCROLL, submittedMode)
    }

    @Test
    fun vaultChangePinForm_isHiddenWhenVaultIsLocked() {
        composeRule.setSettingsContent(vaultState = VaultState.LOCKED)

        composeRule.onNodeWithText("Change PIN").assertDoesNotExist()
        composeRule.onNodeWithTag(SETTINGS_VAULT_CURRENT_PIN_FIELD_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(SETTINGS_VAULT_NEW_PIN_FIELD_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(SETTINGS_VAULT_CONFIRM_PIN_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun vaultChangePinForm_submitsPinsWhenVaultIsUnlocked() {
        var submittedCurrentPin: String? = null
        var submittedNewPin: String? = null
        var submittedConfirmation: String? = null

        composeRule.setSettingsContent(
            vaultState = VaultState.UNLOCKED,
            onChangeVaultPin = { currentPin, newPin, confirmation ->
                submittedCurrentPin = currentPin.concatToString()
                submittedNewPin = newPin.concatToString()
                submittedConfirmation = confirmation.concatToString()
            }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_CHANGE_PIN_BUTTON_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SETTINGS_VAULT_CURRENT_PIN_FIELD_TAG)
            .performScrollTo()
            .performTextInput("1234")
        composeRule.onNodeWithTag(SETTINGS_VAULT_NEW_PIN_FIELD_TAG)
            .performScrollTo()
            .performTextInput("5678")
        composeRule.onNodeWithTag(SETTINGS_VAULT_CONFIRM_PIN_FIELD_TAG)
            .performScrollTo()
            .performTextInput("5678")

        composeRule.onNodeWithTag(SETTINGS_VAULT_CHANGE_PIN_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        assertEquals("1234", submittedCurrentPin)
        assertEquals("5678", submittedNewPin)
        assertEquals("5678", submittedConfirmation)
    }

    @Test
    fun vaultAndroidCredentialSwitch_isDisabledBeforeVaultSetup() {
        composeRule.setSettingsContent(vaultState = VaultState.NOT_CONFIGURED)

        composeRule.onNodeWithTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG)
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun vaultAndroidCredentialSwitch_isDisabledWhenVaultIsLockedAndAndroidUnlockIsOff() {
        composeRule.setSettingsContent(vaultState = VaultState.LOCKED)

        composeRule.onNodeWithTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG)
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun vaultAndroidCredentialSwitch_submitsChangeWhenVaultIsUnlocked() {
        var submittedValue: Boolean? = null

        composeRule.setSettingsContent(
            vaultState = VaultState.UNLOCKED,
            onUnlockVaultWithAndroidCredentialChange = { submittedValue = it }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG)
            .performScrollTo()
            .performClick()

        assertEquals(true, submittedValue)
    }

    @Test
    fun vaultAndroidCredentialSwitch_requiresConfirmationBeforeDisabling() {
        var submittedValue: Boolean? = null

        composeRule.setSettingsContent(
            vaultState = VaultState.LOCKED,
            unlockVaultWithAndroidCredential = true,
            onUnlockVaultWithAndroidCredentialChange = { submittedValue = it }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Disable Android screen lock?").assertIsDisplayed()
        assertNull(submittedValue)
    }

    @Test
    fun vaultAndroidCredentialDisableConfirmation_confirmsChange() {
        var submittedValue: Boolean? = null

        composeRule.setSettingsContent(
            vaultState = VaultState.LOCKED,
            unlockVaultWithAndroidCredential = true,
            onUnlockVaultWithAndroidCredentialChange = { submittedValue = it }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Disable").performClick()

        assertEquals(false, submittedValue)
        composeRule.onNodeWithText("Disable Android screen lock?").assertDoesNotExist()
    }

    @Test
    fun vaultAndroidCredentialDisableConfirmation_canBeCanceled() {
        var submittedValue: Boolean? = null

        composeRule.setSettingsContent(
            vaultState = VaultState.LOCKED,
            unlockVaultWithAndroidCredential = true,
            onUnlockVaultWithAndroidCredentialChange = { submittedValue = it }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_ANDROID_CREDENTIAL_SWITCH_TAG)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertNull(submittedValue)
        composeRule.onNodeWithText("Disable Android screen lock?").assertDoesNotExist()
    }

    @Test
    fun vaultAutoLockTimeoutRow_submitsSelectedTimeout() {
        var submittedTimeout: VaultAutoLockTimeout? = null

        composeRule.setSettingsContent(
            vaultState = VaultState.LOCKED,
            vaultAutoLockTimeout = VaultAutoLockTimeout.IMMEDIATELY,
            onVaultAutoLockTimeoutChange = { submittedTimeout = it }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_AUTO_LOCK_TIMEOUT_ROW_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("After 5 minutes").performClick()

        assertEquals(VaultAutoLockTimeout.AFTER_5_MINUTES, submittedTimeout)
    }

    @Test
    fun vaultResetRow_isHiddenWhenVaultIsNotConfigured() {
        composeRule.setSettingsContent(vaultState = VaultState.NOT_CONFIGURED)

        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun vaultResetRow_isHiddenWhenVaultIsLocked() {
        composeRule.setSettingsContent(vaultState = VaultState.LOCKED)

        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun vaultResetRow_invokesRequestWhenVaultIsUnlocked() {
        var requested = false

        composeRule.setSettingsContent(
            vaultState = VaultState.UNLOCKED,
            onRequestVaultReset = { requested = true }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_ROW_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(requested)
    }

    @Test
    fun vaultResetConfirmation_isShownWhenStateRequestsIt() {
        composeRule.setSettingsContent(
            vaultState = VaultState.UNLOCKED,
            vaultResetState = SettingsVaultResetUiState(isConfirmationVisible = true)
        )

        composeRule.onNodeWithText("Reset Vault?").assertIsDisplayed()
        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_CONFIRM_BUTTON_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_CANCEL_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun vaultResetConfirmation_confirmInvokesCallback() {
        var confirmed = false
        var canceled = false

        composeRule.setSettingsContent(
            vaultState = VaultState.UNLOCKED,
            vaultResetState = SettingsVaultResetUiState(isConfirmationVisible = true),
            onConfirmVaultReset = { confirmed = true },
            onCancelVaultReset = { canceled = true }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_CONFIRM_BUTTON_TAG).performClick()

        assertTrue(confirmed)
        assertFalse(canceled)
    }

    @Test
    fun vaultResetConfirmation_cancelInvokesCallback() {
        var confirmed = false
        var canceled = false

        composeRule.setSettingsContent(
            vaultState = VaultState.UNLOCKED,
            vaultResetState = SettingsVaultResetUiState(isConfirmationVisible = true),
            onConfirmVaultReset = { confirmed = true },
            onCancelVaultReset = { canceled = true }
        )

        composeRule.onNodeWithTag(SETTINGS_VAULT_RESET_CANCEL_BUTTON_TAG).performClick()

        assertTrue(canceled)
        assertFalse(confirmed)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setSettingsContent(
        vaultState: VaultState,
        unlockVaultWithAndroidCredential: Boolean = false,
        vaultAutoLockTimeout: VaultAutoLockTimeout = VaultAutoLockTimeout.IMMEDIATELY,
        vaultResetState: SettingsVaultResetUiState = SettingsVaultResetUiState(),
        onChangeVaultPin: (CharArray, CharArray, CharArray) -> Unit = { _, _, _ -> },
        onUnlockVaultWithAndroidCredentialChange: (Boolean) -> Unit = {},
        onVaultAutoLockTimeoutChange: (VaultAutoLockTimeout) -> Unit = {},
        onTableLayoutModeChange: (TableLayoutMode) -> Unit = {},
        onRequestVaultReset: () -> Unit = {},
        onCancelVaultReset: () -> Unit = {},
        onConfirmVaultReset: () -> Unit = {},
        onClearVaultResetFeedback: () -> Unit = {}
    ) {
        setContent {
            NexNoteTheme {
                SettingsScreenContent(
                    uiState = SettingsUiState(
                        themeMode = ThemeMode.SYSTEM,
                        fontScale = FontScale.NORMAL,
                        timezoneId = "",
                        availableTimezones = emptyList(),
                        accentColor = AccentColor.VIOLET,
                        noteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW,
                        vaultState = vaultState,
                        protectVaultRecentPreviews = true,
                        lockVaultOnBackground = true,
                        vaultAutoLockTimeout = vaultAutoLockTimeout,
                        unlockVaultWithAndroidCredential = unlockVaultWithAndroidCredential
                    ),
                    vaultPinChangeState = SettingsVaultPinChangeUiState(),
                    vaultResetState = vaultResetState,
                    onThemeModeChange = {},
                    onAccentColorChange = {},
                    onFontScaleChange = {},
                    onNoteCardStyleChange = {},
                    onTableLayoutModeChange = onTableLayoutModeChange,
                    onTimezoneChange = {},
                    onUnlockVaultWithAndroidCredentialChange =
                        onUnlockVaultWithAndroidCredentialChange,
                    onVaultAutoLockTimeoutChange = onVaultAutoLockTimeoutChange,
                    onChangeVaultPin = onChangeVaultPin,
                    onRequestVaultReset = onRequestVaultReset,
                    onCancelVaultReset = onCancelVaultReset,
                    onConfirmVaultReset = onConfirmVaultReset,
                    onClearVaultResetFeedback = onClearVaultResetFeedback
                )
            }
        }
    }
}
