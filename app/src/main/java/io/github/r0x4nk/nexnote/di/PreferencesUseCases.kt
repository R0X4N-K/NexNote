package io.github.r0x4nk.nexnote.di

import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveAppFontUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveDynamicColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTableLayoutModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ObserveVaultRecentPreviewsProtectionUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetAccentColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetAppFontUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetDynamicColorUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetFontScaleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetNoteCardStyleUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetTableLayoutModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetThemeModeUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetTimezoneIdUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultAndroidCredentialUnlockUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultAutoLockTimeoutUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultLockOnBackgroundUseCase
import io.github.r0x4nk.nexnote.domain.usecase.SetVaultRecentPreviewsProtectionUseCase

internal class PreferencesUseCases internal constructor(
    preferencesRepository: IUserPreferencesRepository
) {
    val observeThemeMode = ObserveThemeModeUseCase(preferencesRepository)
    val observeAppFont = ObserveAppFontUseCase(preferencesRepository)
    val observeFontScale = ObserveFontScaleUseCase(preferencesRepository)
    val observeTimezoneId = ObserveTimezoneIdUseCase(preferencesRepository)
    val observeDynamicColor = ObserveDynamicColorUseCase(preferencesRepository)
    val observeAccentColor = ObserveAccentColorUseCase(preferencesRepository)
    val observeNoteCardStyle = ObserveNoteCardStyleUseCase(preferencesRepository)
    val observeTableLayoutMode = ObserveTableLayoutModeUseCase(preferencesRepository)
    val observeVaultRecentPreviewsProtection =
        ObserveVaultRecentPreviewsProtectionUseCase(preferencesRepository)
    val observeVaultLockOnBackground =
        ObserveVaultLockOnBackgroundUseCase(preferencesRepository)
    val observeVaultAutoLockTimeout =
        ObserveVaultAutoLockTimeoutUseCase(preferencesRepository)
    val observeVaultAndroidCredentialUnlock =
        ObserveVaultAndroidCredentialUnlockUseCase(preferencesRepository)
    val setThemeMode = SetThemeModeUseCase(preferencesRepository)
    val setAppFont = SetAppFontUseCase(preferencesRepository)
    val setFontScale = SetFontScaleUseCase(preferencesRepository)
    val setTimezoneId = SetTimezoneIdUseCase(preferencesRepository)
    val setDynamicColor = SetDynamicColorUseCase(preferencesRepository)
    val setAccentColor = SetAccentColorUseCase(preferencesRepository)
    val setNoteCardStyle = SetNoteCardStyleUseCase(preferencesRepository)
    val setTableLayoutMode = SetTableLayoutModeUseCase(preferencesRepository)
    val setVaultRecentPreviewsProtection =
        SetVaultRecentPreviewsProtectionUseCase(preferencesRepository)
    val setVaultLockOnBackground =
        SetVaultLockOnBackgroundUseCase(preferencesRepository)
    val setVaultAutoLockTimeout =
        SetVaultAutoLockTimeoutUseCase(preferencesRepository)
    val setVaultAndroidCredentialUnlock =
        SetVaultAndroidCredentialUnlockUseCase(preferencesRepository)
}
