package dev.justinsmith.bjj.core

import kotlin.test.Test
import kotlin.test.assertEquals

class DaysTest {
    @Test
    fun `names a day number as a calendar date`() {
        assertEquals("1970-01-01", dayKeyOf(0))
        assertEquals("1969-12-31", dayKeyOf(-1))
        assertEquals("2000-02-29", dayKeyOf(11_016))
        assertEquals("2000-03-01", dayKeyOf(11_017))
        assertEquals("2026-11-14", dayKeyOf(20_771))
        assertEquals("2100-03-01", dayKeyOf(47_541))
    }

    @Test
    fun `puts a moment on the local day`() {
        val west = LocalZone { -8 * HOUR_MS }
        assertEquals("1970-01-01", localDayKey(0, LocalZone.Utc))
        assertEquals("1969-12-31", localDayKey(0, west))
        assertEquals(-1, localDayNumber(0, west))
    }

    @Test
    fun `counts the days to a calendar date and back again`() {
        assertEquals(0, dayNumberOf(1970, 1, 1))
        assertEquals(20_771, dayNumberOf(2026, 11, 14))
        assertEquals(11_016, dayNumberOf(2000, 2, 29))
        for (day in listOf(-1L, 0, 59, 60, 11_016, 20_771, 47_541, 900_000)) {
            val (year, month, date) = dayKeyOf(day).split('-').map { it.toInt() }
            assertEquals(day, dayNumberOf(year, month, date), dayKeyOf(day))
        }
    }
}
