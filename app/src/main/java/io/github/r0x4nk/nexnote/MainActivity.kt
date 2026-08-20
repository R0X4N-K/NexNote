package io.github.r0x4nk.nexnote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.r0x4nk.nexnote.domain.model.AccentColor
import io.github.r0x4nk.nexnote.domain.model.FontScale
import io.github.r0x4nk.nexnote.domain.model.TableLayoutMode
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.domain.model.VaultAutoLockTimeout
import io.github.r0x4nk.nexnote.domain.model.VaultState
import io.github.r0x4nk.nexnote.fileimport.ExternalFileImportResult
import io.github.r0x4nk.nexnote.fileimport.ExternalFileImporter
import io.github.r0x4nk.nexnote.fileimport.ExternalFileOpenRequest
import io.github.r0x4nk.nexnote.ui.component.LocalMarkdownTableLayoutMode
import io.github.r0x4nk.nexnote.ui.navigation.AppNavigation
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val externalFileOpenRequest = MutableStateFlow<ExternalFileOpenRequest?>(null)
    private lateinit var externalFileImporter: ExternalFileImporter
    private var nextExternalFileOpenRequestId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val useCases = (application as NexNoteApp).useCases
        externalFileImporter = ExternalFileImporter(
            contentResolver = contentResolver,
            saveNote = useCases.notes.saveNote,
            indexNoteTags = useCases.tags.indexNoteTags
        )
        val preferences = useCases.preferences
        val vault = useCases.vault

        setContent {
            val themeMode by preferences.observeThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val fontScale by preferences.observeFontScale().collectAsStateWithLifecycle(FontScale.NORMAL)
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
            val pendingExternalFileOpen by externalFileOpenRequest.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT     -> false
                ThemeMode.DARK      -> true
                ThemeMode.SYSTEM    -> isSystemInDarkTheme()
                ThemeMode.TRUE_DARK -> true
            }
            val trueDark = themeMode == ThemeMode.TRUE_DARK

            NexNoteTheme(
                darkTheme   = darkTheme,
                trueDark    = trueDark,
                fontScale   = fontScale.multiplier,
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
                        onExternalFileOpenConsumed = ::consumeExternalFileOpenRequest,
                        onVaultAutoLockRequested = { vault.lockVault() }
                    )
                }
            }
        }

        if (savedInstanceState == null) {
            handleExternalFileIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalFileIntent(intent)
    }

    private fun handleExternalFileIntent(intent: Intent?) {
        lifecycleScope.launch {
            when (val result = externalFileImporter.importFrom(intent)) {
                ExternalFileImportResult.Ignored -> Unit
                is ExternalFileImportResult.Imported -> {
                    publishExternalFileOpenRequest(result.noteId)
                    clearHandledIntent()
                }
                is ExternalFileImportResult.Failed -> {
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    clearHandledIntent()
                }
            }
        }
    }

    private fun publishExternalFileOpenRequest(noteId: Long) {
        nextExternalFileOpenRequestId += 1
        externalFileOpenRequest.value = ExternalFileOpenRequest(
            requestId = nextExternalFileOpenRequestId,
            noteId = noteId
        )
    }

    private fun consumeExternalFileOpenRequest(requestId: Long) {
        if (externalFileOpenRequest.value?.requestId == requestId) {
            externalFileOpenRequest.value = null
        }
    }

    private fun clearHandledIntent() {
        setIntent(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
    }
}
