package io.github.r0x4nk.nexnote.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import io.github.r0x4nk.nexnote.domain.repository.VaultPinRateLimitException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists an attempt before verification, so cancellation cannot refund guesses. */
internal class VaultPinAttemptLimiter(
    private val dataStore: DataStore<Preferences>,
    private val nowMillis: () -> Long,
    private val bootCount: () -> Int = { 0 }
) {
    private val mutex = Mutex()

    suspend fun verify(block: () -> Boolean): Boolean = mutex.withLock {
        var retryAfterMillis = 0L
        dataStore.edit { prefs ->
            val now = nowMillis()
            val boot = bootCount()
            val lastAttempt = prefs[LAST_ATTEMPT] ?: now
            var deadline = prefs[BLOCKED_UNTIL] ?: now
            if ((prefs[BOOT_COUNT] ?: boot) != boot || now < lastAttempt) {
                // Elapsed time resets on reboot. Conservatively restart the previous
                // penalty once; persist the rebasing even though this guess is refused.
                deadline = now + (deadline - lastAttempt).coerceIn(0L, MAX_DELAY_MILLIS)
                prefs[LAST_ATTEMPT] = now
                prefs[BLOCKED_UNTIL] = deadline
                prefs[BOOT_COUNT] = boot
            }
            retryAfterMillis = (deadline - now).coerceAtLeast(0L)
            if (retryAfterMillis > 0L) return@edit
            val attempts = ((prefs[ATTEMPTS] ?: 0) + 1).coerceAtMost(MAX_ATTEMPTS)
            val delay = if (attempts < FREE_ATTEMPTS) 0L else
                (INITIAL_DELAY_MILLIS shl (attempts - FREE_ATTEMPTS)).coerceAtMost(MAX_DELAY_MILLIS)
            prefs[BOOT_COUNT] = boot
            prefs[ATTEMPTS] = attempts
            prefs[LAST_ATTEMPT] = now
            prefs[BLOCKED_UNTIL] = now + delay
        }
        if (retryAfterMillis > 0L) throw VaultPinRateLimitException(retryAfterMillis)
        block().also { valid -> if (valid) reset() }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            prefs.remove(BOOT_COUNT)
            prefs.remove(ATTEMPTS)
            prefs.remove(LAST_ATTEMPT)
            prefs.remove(BLOCKED_UNTIL)
        }
    }

    private companion object {
        val BOOT_COUNT = intPreferencesKey("vault_pin_attempt_boot_count")
        val ATTEMPTS = intPreferencesKey("vault_pin_attempts")
        val LAST_ATTEMPT = longPreferencesKey("vault_pin_last_attempt")
        val BLOCKED_UNTIL = longPreferencesKey("vault_pin_blocked_until")
        const val FREE_ATTEMPTS = 5
        const val MAX_ATTEMPTS = 11
        const val INITIAL_DELAY_MILLIS = 30_000L
        const val MAX_DELAY_MILLIS = 30 * 60_000L
    }
}
