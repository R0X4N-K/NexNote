package io.github.r0x4nk.nexnote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.AppFont
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.fileimport.ExternalImportViewModel
import io.github.r0x4nk.nexnote.ui.component.LocalMarkdownTableLayoutMode
import io.github.r0x4nk.nexnote.ui.navigation.AppNavigation
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import io.github.r0x4nk.nexnote.ui.theme.fontFamily
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val externalImports: ExternalImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val useCases = (application as NexNoteApp).useCases

        val preferences = useCases.preferences
        val vault = useCases.vault

        setContent {
            val themeMode by preferences.observeThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val appFont by preferences.observeAppFont().collectAsStateWithLifecycle(AppFont.SYSTEM)
            val fontScale by preferences.observeFontScale().collectAsStateWithLifecycle(FontScale.NORMAL)
            val dynamicColor by preferences.observeDynamicColor().collectAsStateWithLifecycle(false)
            val accentColor by preferences.observeAccentColor().collectAsStateWithLifecycle(AccentColor.VIOLET)
            val tableLayoutMode by preferences
                .observeTableLayoutMode()
                .collectAsStateWithLifecycle(TableLayoutMode.FIT_SCREEN)
            val protectVaultRecentPreviews by preferences
                .observeVaultRecentPreviewsProtection()
                .collectAsStateWithLifecycle(true)
            val lockVaultOnBackground by preferences
                .observeVaultLockOnBackground()
                .collectAsStateWithLifecycle(true)
            val vaultAutoLockTimeout by preferences
                .observeVaultAutoLockTimeout()
                .collectAsStateWithLifecycle(VaultAutoLockTimeout.IMMEDIATELY)
            val vaultState by vault.observeVaultState()
                .collectAsStateWithLifecycle(VaultState.NOT_CONFIGURED)
            val pendingExternalFileOpen by externalImports.openRequest.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT     -> false
                ThemeMode.DARK      -> true
                ThemeMode.SYSTEM    -> isSystemInDarkTheme()
                ThemeMode.TRUE_DARK -> true
            }
            val trueDark = themeMode == ThemeMode.TRUE_DARK
            val fontFamily = remember(appFont) { appFont.fontFamily() }
            SideEffect {
                // App appearance can differ from the system theme used by edge-to-edge defaults.
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            NexNoteTheme(
                darkTheme   = darkTheme,
                trueDark    = trueDark,
                fontScale   = fontScale.multiplier,
                fontFamily  = fontFamily,
                dynamicColor = dynamicColor,
                accentColor = accentColor
            ) {
                CompositionLocalProvider(
                    LocalMarkdownTableLayoutMode provides tableLayoutMode
                ) {
                    AppNavigation(
                        protectVaultRecentPreviews = protectVaultRecentPreviews,
                        lockVaultOnBackground = lockVaultOnBackground,
                        vaultAutoLockTimeout = vaultAutoLockTimeout,
                        vaultState = vaultState,
                        externalFileOpenRequest = pendingExternalFileOpen,
                        onExternalFileOpenConsumed = externalImports::consumeOpenRequest,
                        onVaultAutoLockRequested = { vault.lockVault() }
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                externalImports.errorMessages.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }

        if (savedInstanceState == null) {
            handleExternalIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(intent: Intent?) {
        externalImports.accept(intent)
        setIntent(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
    }
}
