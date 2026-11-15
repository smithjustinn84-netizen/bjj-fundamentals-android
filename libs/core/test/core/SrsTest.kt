package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.SCHEDULE_GOLDEN
import dev.justinsmith.bjj.core.golden.SCHEDULE_WALK
import dev.justinsmith.bjj.core.testing.TEST_NOW
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val NOW = TEST_NOW
private const val CLOSE = 1e-9

class SrsTest {
    private val fresh = newReviewState(CardKind.Concept, "x", NOW)

    @Test
    fun `walks a new card through 1d then 3d then ease-multiplied intervals`() {
        var s = schedule(newReviewState(CardKind.Concept, "base", NOW), Grade.Good, NOW)
        assertEquals(1, s.intervalDays)
        assertEquals(NOW + DAY_MS, s.due)
        s = schedule(s, Grade.Good, NOW)
        assertEquals(3, s.intervalDays)
        s = schedule(s, Grade.Good, NOW)
        assertEquals(8, s.intervalDays) // round(3 * 2.5)
        assertEquals(3, s.repetitions)
    }

    @Test
    fun `again resets repetitions and lowers ease and brings the card back soon`() {
        var s = schedule(schedule(newReviewState(CardKind.Position, "mount", NOW), Grade.Good, NOW), Grade.Good, NOW)
        s = schedule(s, Grade.Again, NOW)
        assertEquals(0, s.repetitions)
        assertEquals(1, s.lapses)
        assertEquals(2.3, s.ease, CLOSE)
        assertEquals(NOW + RELEARN_MS, s.due)
    }

    @Test
    fun `never drops ease below the floor`() {
        var s = fresh
        repeat(20) { s = schedule(s, Grade.Again, NOW) }
        assertEquals(1.3, s.ease)
    }

    @Test
    fun `easy on a new card skips ahead and raises ease`() {
        val s = schedule(fresh, Grade.Easy, NOW)
        assertEquals(4, s.intervalDays)
        assertEquals(2.65, s.ease, CLOSE)
    }

    @Test
    fun `hard grows the interval slowly and lowers ease and may keep the interval flat`() {
        var s = schedule(fresh, Grade.Hard, NOW)
        assertEquals(1, s.intervalDays)
        assertEquals(2.35, s.ease, CLOSE)
        s = schedule(s, Grade.Hard, NOW)
        assertEquals(1, s.intervalDays) // max(1, round(1 * 1.2))
    }

    @Test
    fun `easy on a reviewed card multiplies by ease and the easy bonus`() {
        val s = schedule(schedule(fresh, Grade.Good, NOW), Grade.Easy, NOW)
        assertEquals(3, s.intervalDays) // round(1 * 2.5 * 1.3)
        assertEquals(2.65, s.ease, CLOSE)
    }

    @Test
    fun `schedules every case exactly as the web app does`() {
        for (case in SCHEDULE_GOLDEN) {
            val prev = fresh.copy(
                ease = case.ease,
                intervalDays = case.intervalDays,
                repetitions = case.repetitions,
                lapses = 1,
            )
            val next = schedule(prev, case.grade, NOW)
            assertEquals(case.nextEase, next.ease, "$case")
            assertEquals(case.nextIntervalDays, next.intervalDays, "$case")
            assertEquals(case.nextRepetitions, next.repetitions, "$case")
            assertEquals(case.nextLapses, next.lapses, "$case")
            assertEquals(case.dueIn, next.due - NOW, "$case")
            assertEquals(case.label, describeInterval(prev, case.grade, NOW), "$case")
        }
    }

    @Test
    fun `builds up ease over many reviews exactly as the web app does`() {
        var card = fresh
        for ((index, step) in SCHEDULE_WALK.withIndex()) {
            card = schedule(card, step.grade, NOW)
            assertEquals(step.ease, card.ease, "step $index")
            assertEquals(step.intervalDays, card.intervalDays, "step $index")
            assertEquals(step.repetitions, card.repetitions, "step $index")
            assertEquals(step.lapses, card.lapses, "step $index")
        }
    }

    @Test
    fun `shows minutes for again and days otherwise`() {
        assertEquals("10m", describeInterval(fresh, Grade.Again, NOW))
        assertEquals("1d", describeInterval(fresh, Grade.Good, NOW))
    }

    @Test
    fun `switches to months and years for long intervals`() {
        assertEquals("2mo", describeInterval(fresh.copy(repetitions = 5, intervalDays = 20), Grade.Good, NOW))
        assertEquals("1.4y", describeInterval(fresh.copy(repetitions = 5, intervalDays = 200), Grade.Good, NOW))
    }

    @Test
    fun `learned needs a multi-day interval`() {
        assertFalse(isLearned(null))
        assertFalse(isLearned(fresh.copy(intervalDays = 2)))
        assertTrue(isLearned(fresh.copy(intervalDays = 3)))
    }

    @Test
    fun `a streak counts a day with a logged class like a day with a review`() {
        // Reviewed today, trained yesterday, reviewed the day before: three days in a row.
        val yesterday = localDayKey(NOW - DAY_MS, LocalZone.Utc)
        assertEquals(3, streakDays(listOf(NOW, NOW - 2 * DAY_MS), NOW, LocalZone.Utc, listOf(yesterday)))
        assertEquals(1, streakDays(emptyList(), NOW, LocalZone.Utc, listOf(localDayKey(NOW, LocalZone.Utc))))
    }

    @Test
    fun `a streak counts consecutive days ending today`() {
        assertEquals(3, streakDays(listOf(NOW, NOW - DAY_MS, NOW - 2 * DAY_MS), NOW, LocalZone.Utc))
    }

    @Test
    fun `a streak that ended yesterday still counts`() {
        assertEquals(2, streakDays(listOf(NOW - DAY_MS, NOW - 2 * DAY_MS), NOW, LocalZone.Utc))
    }

    @Test
    fun `a streak is zero with a gap`() {
        assertEquals(0, streakDays(listOf(NOW - 3 * DAY_MS), NOW, LocalZone.Utc))
    }

    @Test
    fun `a streak follows the local day and not the day in UTC`() {
        // 23:30 UTC is already tomorrow two hours east: a review then and one an hour later are the same local day.
        val east = LocalZone { 2 * HOUR_MS }
        val lateEvening = NOW + 11 * HOUR_MS + 30 * MINUTE_MS
        assertEquals("2026-11-15", localDayKey(lateEvening, east))
        assertEquals("2026-11-14", localDayKey(lateEvening, LocalZone.Utc))
        assertEquals(1, streakDays(listOf(lateEvening), lateEvening + HOUR_MS, east))
    }
}
