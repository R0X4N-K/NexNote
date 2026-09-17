package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.VaultState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** In-memory picker request. Never stores file bytes, note content, PINs or keys. */
internal class EditorPendingAttachment(
    private val currentState: () -> EditorUiState,
    private val persist: suspend () -> Boolean,
    private val reloadVaultNote: suspend (Long, suspend () -> Boolean) -> Unit,
    private val onError: () -> Unit = {},
    private val readVaultState: (suspend () -> VaultState)? = null
) {
    private val mutex = Mutex()
    private var targetId: Long? = null
    private var result: (() -> Unit)? = null
    private var revision = 0L
    private val pending = MutableStateFlow(false)
    val hasResult = pending.asStateFlow()
    private val vault = MutableStateFlow(VaultState.LOCKED)
    val vaultState = vault.asStateFlow()

    fun onVaultState(state: VaultState) {
        if (state == vault.value) return
        revision++
        vault.value = state
        if (state == VaultState.NOT_CONFIGURED) cancel()
    }

    suspend fun prepare(): Boolean = mutex.withLock {
        if (targetId != null || !canEdit() || !hasVaultAccess()) return@withLock false
        val expectedRevision = revision
        if (!persist() || !canEdit() || !hasVaultAccess() || revision != expectedRevision) return@withLock false
        targetId = currentState().noteId.takeIf { it != EditorViewModel.NO_ID }
        targetId != null
    }

    fun accept(import: () -> Unit) {
        if (targetId == null || pending.value) return
        result = import
        pending.value = true
    }

    fun cancel() {
        revision++
        result = null
        targetId = null
        pending.value = false
    }

    /** Called only while the editor is RESUMED, after the normal auto-lock checks. */
    suspend fun resume() = mutex.withLock {
        val import = result ?: return@withLock
        val id = targetId ?: return@withLock
        if (currentState().noteId != id) {
            cancel()
            return@withLock
        }
        if (currentState().isVaultNote) {
            // Activity ON_START can lock synchronously before our flow collector runs.
            // Read the repository now; a cached UI/collector state is not authorization.
            val actualState = currentVaultState()
            onVaultState(actualState)
            if (actualState != VaultState.UNLOCKED) return@withLock
            val expectedRevision = revision
            val mayApply: suspend () -> Boolean = { currentVaultState() == VaultState.UNLOCKED && revision == expectedRevision }
            try {
                if (currentState().isVaultLocked) reloadVaultNote(id, mayApply)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (!mayApply()) return@withLock
                cancel()
                onError()
                return@withLock
            }
            currentCoroutineContext().ensureActive()
            if (!mayApply()) return@withLock
        }
        if (!canEdit()) {
            cancel()
            return@withLock
        }
        cancel() // Consume before dispatch: recomposition/resume cannot import twice.
        import()
    }

    private suspend fun currentVaultState(): VaultState = readVaultState?.invoke() ?: vault.value

    private suspend fun hasVaultAccess(): Boolean =
        !currentState().isVaultNote || currentVaultState() == VaultState.UNLOCKED

    private fun canEdit(): Boolean = currentState().let {
        !it.isLoading && !it.isReadOnly && !it.isVaultLocked && !it.isTemplateMode &&
            (!it.isVaultNote || vault.value == VaultState.UNLOCKED)
    }
}
