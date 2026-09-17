package io.github.r0x4nk.nexnote.data.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.r0x4nk.nexnote.domain.repository.VaultPinRateLimitException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultPinAttemptLimiterTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun lockoutPersistsAndDoublesWithoutExecutingBlockedGuesses() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.newFile("pin.preferences_pb")
        }
        var now = 100_000L
        val limiter = VaultPinAttemptLimiter(store, nowMillis = { now })
        repeat(5) { assertFalse(limiter.verify { false }) }
        val recreated = VaultPinAttemptLimiter(store, nowMillis = { now })
        try {
            recreated.verify { error("Blocked guess must not execute") }
            fail("Expected lockout")
        } catch (error: VaultPinRateLimitException) {
            assertEquals(30_000L, error.retryAfterMillis)
        }
        now += 30_000L
        assertFalse(recreated.verify { false })
        try {
            recreated.verify { true }
            fail("Expected doubled lockout")
        } catch (error: VaultPinRateLimitException) {
            assertEquals(60_000L, error.retryAfterMillis)
        }
        now += 60_000L
        assertTrue(recreated.verify { true })
        repeat(4) { assertFalse(recreated.verify { false }) }
        assertTrue(recreated.verify { true })
    }

    @Test fun interruptedVerificationsCountAndBackwardsClockDoesNotUnlock() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.newFile("pin.preferences_pb")
        }
        var now = 100_000L
        val limiter = VaultPinAttemptLimiter(store, nowMillis = { now })
        repeat(5) {
            try {
                limiter.verify { throw CancellationException("Interrupted") }
            } catch (_: CancellationException) { }
        }
        now = 1L
        try {
            limiter.verify { error("Clock rollback must not permit a guess") }
            fail("Expected lockout")
        } catch (error: VaultPinRateLimitException) {
            assertEquals(30_000L, error.retryAfterMillis)
        }
    }

    @Test fun rebootRestartsPenaltyOnceEvenWhenNewUptimeExceedsOldDeadline() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.newFile("pin.preferences_pb")
        }
        var now = 100_000L
        var boot = 1
        val limiter = VaultPinAttemptLimiter(store, { now }, { boot })
        repeat(5) { limiter.verify { false } }
        boot = 2
        now = 1_000_000L
        try {
            limiter.verify { error("A reboot must not refund attempts") }
            fail("Expected lockout")
        } catch (error: VaultPinRateLimitException) {
            assertEquals(30_000L, error.retryAfterMillis)
        }
        now += 30_000L
        assertTrue(VaultPinAttemptLimiter(store, { now }, { boot }).verify { true })
    }
    @Test fun exponentialDelayIsCapped() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.newFile("pin.preferences_pb")
        }
        var now = 100_000L
        val limiter = VaultPinAttemptLimiter(store, nowMillis = { now })
        repeat(20) { attempt ->
            assertFalse(limiter.verify { false })
            if (attempt >= 4) {
                try {
                    limiter.verify { error("Expected lockout") }
                    fail("Expected lockout")
                } catch (error: VaultPinRateLimitException) {
                    val expected = (30_000L shl (attempt - 4).coerceAtMost(6)).coerceAtMost(1_800_000L)
                    assertEquals(expected, error.retryAfterMillis)
                    now += error.retryAfterMillis
                }
            }
        }
    }
}
