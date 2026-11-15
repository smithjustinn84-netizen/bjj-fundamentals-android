package dev.justinsmith.bjj.core

import kotlin.test.Test
import kotlin.test.assertEquals

class JsRoundTest {
    @Test
    fun `a half rounds up, not to the even neighbour`() {
        assertEquals(3, jsRound(2.5))
        assertEquals(4, jsRound(3.5))
        assertEquals(1, jsRound(0.5))
    }

    @Test
    fun `a negative half rounds towards positive infinity`() {
        assertEquals(-2, jsRound(-2.5))
        assertEquals(0, jsRound(-0.5))
    }

    @Test
    fun `anything else rounds to the nearest whole number`() {
        assertEquals(2, jsRound(2.4))
        assertEquals(3, jsRound(2.6))
        assertEquals(-3, jsRound(-2.6))
        assertEquals(7, jsRoundToInt(7.0))
    }
}
