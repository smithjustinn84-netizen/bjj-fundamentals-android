package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.trainingEntry
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrainingLogTest {
    @Test
    fun `accepts a calendar day and rejects anything else`() {
        assertTrue(isDayKey("2026-11-14"))
        // A label for sorting and display, as in the web app: day 31 passes in any month.
        assertTrue(isDayKey("2026-02-31"))
        for (bad in listOf("2026-13-01", "2026-11-32", "2026-1-4", "14/11/2026", "", "2026-00-10", "2026-11-00")) {
            assertFalse(isDayKey(bad), bad)
        }
    }

    @Test
    fun `orders by day then by when it was written`() {
        val entries = listOf(
            trainingEntry(id = "old", day = "2026-11-01"),
            trainingEntry(id = "morning", day = "2026-11-14", createdAt = 1),
            trainingEntry(id = "evening", day = "2026-11-14", createdAt = 2),
        )
        assertEquals(listOf("evening", "morning", "old"), newestFirst(entries).map { it.id })
    }

    @Test
    fun `totals classes and minutes and takes the focus from the latest class that set one`() {
        val summary = trainingSummary(
            listOf(
                trainingEntry(id = "a", day = "2026-11-01", minutes = 60, focus = "Frames first"),
                trainingEntry(id = "b", day = "2026-11-08", minutes = 90, focus = "Keep elbows in"),
                trainingEntry(id = "c", day = "2026-11-14", minutes = 45, focus = ""),
            ),
        )
        assertEquals(TrainingSummary(classes = 3, minutes = 195, focus = "Keep elbows in"), summary)
        assertEquals(TrainingSummary(0, 0, ""), trainingSummary(emptyList()))
    }

    @Test
    fun `writes mat time as minutes or whole hours or both`() {
        assertEquals("45 min", formatMatTime(45))
        assertEquals("2 h", formatMatTime(120))
        assertEquals("1 h 30 min", formatMatTime(90))
        assertEquals("0 min", formatMatTime(0))
    }

    @Test
    fun `counts classes with the right plural`() {
        assertEquals("1 class", formatClassCount(1))
        assertEquals("3 classes", formatClassCount(3))
    }

    @Test
    fun `lists techniques from the latest classes with the most recent first and each once`() {
        val entries = listOf(
            trainingEntry(id = "a", day = "2026-11-01", techniqueIds = listOf("old-move")),
            trainingEntry(id = "b", day = "2026-11-08", techniqueIds = listOf("sweep", "pass")),
            trainingEntry(id = "c", day = "2026-11-14", techniqueIds = listOf("escape", "sweep")),
        )
        assertEquals(listOf("escape", "sweep", "pass"), recentlyPractised(entries, 2))
        assertEquals(emptyList(), recentlyPractised(entries, 0))
    }

    private val lib = buildLibrary(
        emptyList(),
        listOf(position(id = "mount", category = PositionCategory.Top)),
        listOf(
            transition("upa", "A", TransitionType.Escape, "mount", role = PositionRole.Bottom),
            transition("shrimp", "B", TransitionType.Escape, "mount", role = PositionRole.Bottom),
            transition("armbar", "C", TransitionType.Submission, "mount", role = PositionRole.Top),
        ),
        emptyList(),
    )

    @Test
    fun `where the student got stuck is read from the latest recent class that named anywhere`() {
        val entries = listOf(
            trainingEntry(id = "a", day = "2026-11-01", stuckIds = listOf("mount:bottom")),
            trainingEntry(id = "b", day = "2026-11-08"),
        )
        assertEquals(listOf("mount:bottom"), recentlyStuck(entries, 2))
        // Study no longer puts that class first, so nothing should claim it does.
        assertEquals(emptyList(), recentlyStuck(entries, 1))
        assertEquals(emptyList(), studyFirst(lib, entries, 1))
    }

    @Test
    fun `puts the situation and its techniques before the techniques practised`() {
        val entry = trainingEntry(techniqueIds = listOf("armbar", "upa"), stuckIds = listOf("mount:bottom", "gone:top"))
        val expected = listOf("situation:mount:bottom", "transition:upa", "transition:shrimp", "transition:armbar")
        assertEquals(expected, studyFirst(lib, listOf(entry), 3))
        assertEquals(emptyList(), studyFirst(lib, listOf(entry), 0))
    }

    @Test
    fun `quick picks list what the entry has first then recent ones once each and only what exists`() {
        assertEquals(listOf("b", "a", "c"), quickPicks(listOf("b", "a"), listOf("a", "c")) { true })
        assertEquals(listOf("a"), quickPicks(listOf("gone", "a"), listOf("also-gone")) { it == "a" })
    }
}
