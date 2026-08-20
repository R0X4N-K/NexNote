package io.github.r0x4nk.nexnote.util

import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DateUtilsTest {

    private lateinit var previousLocale: Locale
    private lateinit var previousTimeZone: TimeZone

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        previousTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
        TimeZone.setDefault(previousTimeZone)
    }

    @Test
    fun `formatDate uses explicit timezone when provided`() {
        assertEquals("01/01/1970", DateUtils.formatDate(EPOCH, timezone = "UTC"))
    }

    @Test
    fun `formatDateTime uses explicit timezone when provided`() {
        assertEquals("01/01/1970 00:00", DateUtils.formatDateTime(EPOCH, timezone = "UTC"))
    }

    @Test
    fun `formatDate falls back to device timezone when timezone is null`() {
        assertEquals("31/12/1969", DateUtils.formatDate(EPOCH))
    }

    private companion object {
        const val EPOCH = 0L
    }
}
