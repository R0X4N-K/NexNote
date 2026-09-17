package io.github.r0x4nk.nexnote.ui.screen.editor

import io.github.r0x4nk.nexnote.domain.model.VaultState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorPendingAttachmentTest {
    private var state = EditorUiState(noteId = 42, isVaultNote = true)
    private var imports = 0
    private var reloads = 0
    private var saves = 0
    private fun request(persist: suspend () -> Boolean = { saves++; true },
                        reload: suspend (Long, suspend () -> Boolean) -> Unit = { id, allowed ->
                            assertEquals(42L, id)
                            reloads++
                            if (allowed()) state = state.copy(isVaultLocked = false, isReadOnly = false)
                        }) = EditorPendingAttachment({ state }, persist, reload).also {
        it.onVaultState(VaultState.UNLOCKED)
    }
    private fun lock(request: EditorPendingAttachment) {
        state = state.copy(isVaultLocked = true, isReadOnly = true, content = "")
        request.onVaultState(VaultState.LOCKED)
    }

    @Test fun `locked result waits for authentication then reloads original note and imports once`() = runTest {
        val request = request()
        assertTrue(request.prepare())
        assertEquals(1, saves)
        lock(request)
        request.accept { imports++ }
        request.resume()
        assertEquals(0, imports)
        assertEquals(0, reloads)
        assertTrue(request.hasResult.value)
        request.onVaultState(VaultState.UNLOCKED)
        request.resume()
        request.resume()
        request.accept { imports++ }
        request.resume()
        assertEquals(1, reloads)
        assertEquals(1, imports)
        assertFalse(request.hasResult.value)
    }

    @Test fun `result arriving before auto lock is not imported in callback`() = runTest {
        val request = request()
        assertTrue(request.prepare())
        request.accept { imports++ }
        assertEquals(0, imports)
        lock(request)
        request.resume()
        assertEquals(0, imports)
    }

    @Test fun `cancel and vault reset discard result without opening file`() = runTest {
        for (reset in listOf(false, true)) {
            state = EditorUiState(noteId = 42, isVaultNote = true)
            val request = request()
            assertTrue(request.prepare())
            lock(request)
            request.accept { imports++ }
            if (reset) request.onVaultState(VaultState.NOT_CONFIGURED) else request.cancel()
            request.onVaultState(VaultState.UNLOCKED)
            request.resume()
            assertFalse(request.hasResult.value)
        }
        assertEquals(0, imports)
    }

    @Test fun `save failure and locked editor never launch a picker`() = runTest {
        val failed = request(persist = { false })
        assertFalse(failed.prepare())
        failed.accept { imports++ }
        failed.resume()
        val locked = request()
        lock(locked)
        assertFalse(locked.prepare())
        assertEquals(0, imports)
        assertEquals(0, saves)
    }

    @Test fun `new lock during reload prevents decrypted content from being applied`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var applied = false
        val request = request(reload = { _, allowed ->
            gate.await()
            if (allowed()) applied = true
        })
        assertTrue(request.prepare())
        lock(request)
        request.accept { imports++ }
        request.onVaultState(VaultState.UNLOCKED)
        val job = launch { request.resume() }
        runCurrent()
        lock(request)
        gate.complete(Unit)
        job.join()
        assertFalse(applied)
        assertEquals(0, imports)
        assertTrue(request.hasResult.value)
    }

    @Test fun `cancellation while reloading leaves result retryable without importing`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val request = request(reload = { _, _ -> gate.await() })
        assertTrue(request.prepare())
        lock(request)
        request.accept { imports++ }
        request.onVaultState(VaultState.UNLOCKED)
        val job = launch { request.resume() }
        runCurrent()
        job.cancel()
        job.join()
        assertEquals(0, imports)
        assertTrue(request.hasResult.value)
    }

    @Test fun `different note and missing restored note cannot receive attachment`() = runTest {
        val request = request(reload = { _, _ -> })
        assertTrue(request.prepare())
        request.accept { imports++ }
        state = state.copy(noteId = 99)
        request.resume()
        assertFalse(request.hasResult.value)
        state = state.copy(noteId = 42)
        assertTrue(request.prepare())
        lock(request)
        request.accept { imports++ }
        request.onVaultState(VaultState.UNLOCKED)
        request.resume()
        assertEquals(0, imports)
        assertFalse(request.hasResult.value)
    }

    @Test fun `authoritative lock wins before observer receives the lock event`() = runTest {
        var actual = VaultState.UNLOCKED
        val request = EditorPendingAttachment({ state }, { true }, { _, _ -> },
            readVaultState = { actual })
        request.onVaultState(VaultState.UNLOCKED)
        assertTrue(request.prepare())
        request.accept { imports++ }
        actual = VaultState.LOCKED // observer/UI intentionally still say UNLOCKED
        request.resume()
        assertEquals(0, imports)
        assertTrue(request.hasResult.value)
        assertEquals(VaultState.LOCKED, request.vaultState.value)
    }

    @Test fun `reload rejects an unobserved new repository lock`() = runTest {
        var actual = VaultState.UNLOCKED
        var applied = false
        val request = EditorPendingAttachment({ state }, { true }, { _, allowed ->
            actual = VaultState.LOCKED
            if (allowed()) applied = true
        }, readVaultState = { actual })
        request.onVaultState(VaultState.UNLOCKED)
        assertTrue(request.prepare())
        lock(request)
        request.accept { imports++ }
        request.onVaultState(VaultState.UNLOCKED)
        request.resume()
        assertFalse(applied)
        assertEquals(0, imports)
    }

    @Test fun `normal note imports on resume without vault authentication`() = runTest {
        state = EditorUiState(noteId = 42)
        val request = request()
        request.onVaultState(VaultState.LOCKED)
        assertTrue(request.prepare())
        request.accept { imports++ }
        request.resume()
        assertEquals(1, imports)
        assertEquals(0, reloads)
    }

    @Test fun `cancel during persistence does not leave an outstanding picker`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val request = request(persist = { gate.await(); true })
        var prepared = true
        val job = launch { prepared = request.prepare() }
        runCurrent()
        request.cancel()
        gate.complete(Unit)
        job.join()
        assertFalse(prepared)
    }
}
