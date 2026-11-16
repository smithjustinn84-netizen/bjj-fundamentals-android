package dev.justinsmith.bjj.ui.common

import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import kotlin.test.Test
import kotlin.test.assertEquals

/** How dates are written. */
class DatesTest {
    private val t: TestCore = testCore()

    @Test
    fun `dates are written in words`() {
        // Noon on Saturday 14 November 2026.
        assertEquals("Saturday 14 November", todayLabel(t.clock.now, LocalZone.Utc))
        assertEquals("Sat 14 Nov 2026", formatDay("2026-11-14"))
        assertEquals("Thu 1 Jan 1970", formatDay("1970-01-01"))
        assertEquals("not a day", formatDay("not a day"))
    }
}
