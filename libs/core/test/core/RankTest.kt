package dev.justinsmith.bjj.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RankTest {
    @Test
    fun `starts as a plain white belt`() {
        assertEquals(BeltProgress(BeltRank.White, 0, BeltRank.Blue, 25), beltRank(0))
    }

    @Test
    fun `adds a stripe every 5 points and never more than the maximum`() {
        assertEquals(0, beltRank(4).stripes)
        assertEquals(1, beltRank(5).stripes)
        assertEquals(BeltProgress(BeltRank.White, MAX_STRIPES, BeltRank.Blue, 1), beltRank(24))
    }

    @Test
    fun `moves up a belt every 25 points`() {
        assertEquals(BeltRank.Blue to 0, beltRank(25).let { it.belt to it.stripes })
        assertEquals(BeltRank.Purple, beltRank(50).belt)
        assertEquals(BeltRank.Brown, beltRank(75).belt)
        assertEquals(BeltProgress(BeltRank.Brown, MAX_STRIPES, BeltRank.Black, 1), beltRank(99))
    }

    @Test
    fun `gives the black belt only when everything is learned`() {
        assertEquals(BeltProgress(BeltRank.Black, 0, null, 0), beltRank(100))
    }

    @Test
    fun `clamps out-of-range input`() {
        assertEquals(BeltRank.White, beltRank(-10).belt)
        assertEquals(BeltRank.Black, beltRank(250).belt)
    }
}
