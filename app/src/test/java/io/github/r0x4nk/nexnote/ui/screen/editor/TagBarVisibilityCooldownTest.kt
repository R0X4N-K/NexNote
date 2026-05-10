package io.github.r0x4nk.nexnote.ui.screen.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagBarVisibilityCooldownTest {

    /** Mutable virtual clock so the tests stay deterministic. */
    private class FakeClock {
        var nowMs: Long = 0L
            private set

        val nowMsProvider: () -> Long = { nowMs }

        fun advance(deltaMs: Long) {
            nowMs += deltaMs
        }
    }

    @Test
    fun `same direction requests are always forwarded`() {
        val clock = FakeClock()
        val cooldown = TagBarVisibilityCooldown(
            cooldownMs = 100L,
            nowMs = clock.nowMsProvider
        )

        assertEquals(
            TagBarVisibilityRequest.Hide,
            cooldown.accept(TagBarVisibilityRequest.Hide)
        )
        clock.advance(10L)
        assertEquals(
            "Same-direction repeats must never be suppressed",
            TagBarVisibilityRequest.Hide,
            cooldown.accept(TagBarVisibilityRequest.Hide)
        )
    }

    @Test
    fun `opposite direction is suppressed inside the cooldown window`() {
        val clock = FakeClock()
        val cooldown = TagBarVisibilityCooldown(
            cooldownMs = 100L,
            nowMs = clock.nowMsProvider
        )

        cooldown.accept(TagBarVisibilityRequest.Hide)
        clock.advance(50L)

        assertNull(
            "Show inside the cooldown window must be dropped to prevent the bounce",
            cooldown.accept(TagBarVisibilityRequest.Show)
        )
    }

    @Test
    fun `opposite direction is forwarded once the cooldown elapses`() {
        val clock = FakeClock()
        val cooldown = TagBarVisibilityCooldown(
            cooldownMs = 100L,
            nowMs = clock.nowMsProvider
        )

        cooldown.accept(TagBarVisibilityRequest.Hide)
        clock.advance(100L)

        assertEquals(
            TagBarVisibilityRequest.Show,
            cooldown.accept(TagBarVisibilityRequest.Show)
        )
    }

    @Test
    fun `each accepted flip restarts the cooldown window`() {
        val clock = FakeClock()
        val cooldown = TagBarVisibilityCooldown(
            cooldownMs = 100L,
            nowMs = clock.nowMsProvider
        )

        cooldown.accept(TagBarVisibilityRequest.Hide)
        clock.advance(120L)
        // First legitimate flip after the cooldown elapsed.
        assertEquals(
            TagBarVisibilityRequest.Show,
            cooldown.accept(TagBarVisibilityRequest.Show)
        )
        // The window must restart from this new emit.
        clock.advance(20L)
        assertNull(
            cooldown.accept(TagBarVisibilityRequest.Hide)
        )
    }

    @Test
    fun `reset clears the cooldown so any direction can flip immediately`() {
        val clock = FakeClock()
        val cooldown = TagBarVisibilityCooldown(
            cooldownMs = 100L,
            nowMs = clock.nowMsProvider
        )

        cooldown.accept(TagBarVisibilityRequest.Hide)
        cooldown.reset()

        assertEquals(
            TagBarVisibilityRequest.Show,
            cooldown.accept(TagBarVisibilityRequest.Show)
        )
    }
}
