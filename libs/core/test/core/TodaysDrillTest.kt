package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.review
import dev.justinsmith.bjj.core.testing.trainingEntry
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodaysDrillTest {
    private val positions =
        listOf(position("mount", category = PositionCategory.Top, order = 1), position("guard", order = 2))
    private val techniques = listOf(
        transition("upa", "A", TransitionType.Escape, "mount", role = PositionRole.Bottom, drill = "Bridge ten times"),
        transition("shrimp", "B", TransitionType.Escape, "mount", role = PositionRole.Bottom, drill = "Shrimp the mat"),
        transition("sweep", "C", TransitionType.Sweep, "guard", role = PositionRole.Bottom, drill = "Ten sweeps"),
        transition("pass", "D", TransitionType.Pass, "guard", role = PositionRole.Top, drill = ""),
    )

    private fun library(vararg reviews: ReviewState) = buildLibrary(
        emptyList(),
        positions,
        techniques,
        reviews.toList(),
    )

    private fun learned(id: String) = review(CardKind.Transition, id, intervalDays = 8)

    private fun pick(lib: Library, vararg entries: TrainingEntry) =
        pickDrill(lib, entries.toList())?.let { it.reason to it.drill }

    @Test
    fun `starts with a way out of where the student last got stuck and skips what is already learned`() {
        val entry = trainingEntry(stuckIds = listOf("mount:bottom"), techniqueIds = listOf("sweep"))
        assertEquals(DrillReason.Stuck to "Bridge ten times", pick(library(), entry))
        assertEquals(DrillReason.Stuck to "Shrimp the mat", pick(library(learned("upa")), entry))
    }

    @Test
    fun `then takes a technique that keeps slipping then one practised in class then the next unlearned one`() {
        val slipping = review(CardKind.Transition, "sweep", lapses = 2, intervalDays = 0)
        assertEquals(DrillReason.Slipping, pickDrill(library(slipping), emptyList())?.reason)
        val practised = trainingEntry(techniqueIds = listOf("shrimp"))
        assertEquals(DrillReason.Practised to "Shrimp the mat", pick(library(), practised))
        assertEquals(DrillReason.Next to "Bridge ten times", pick(library()))
    }

    @Test
    fun `picks the next unlearned technique in lesson order with an escape from a pin before an attack`() {
        val guardFirst =
            listOf(position("guard", order = 1), position("mount", category = PositionCategory.Top, order = 2))
        val lib = buildLibrary(emptyList(), guardFirst, techniques, emptyList())
        assertEquals(DrillReason.Next to "Bridge ten times", pick(lib))
    }

    @Test
    fun `finds a slipping technique behind any number of other slipping cards`() {
        val cards = listOf("a", "b", "c", "d", "e", "f").map {
            review(CardKind.Concept, it, lapses = 5, intervalDays = 0)
        }
        val lib = buildLibrary(
            cards.map { concept(id = it.itemId) },
            positions,
            techniques,
            cards + review(CardKind.Transition, "sweep", lapses = 2, intervalDays = 0),
        )
        assertEquals(DrillReason.Slipping to "Ten sweeps", pick(lib))
    }

    @Test
    fun `ignores where the student got stuck once that class is no longer recent`() {
        val entries = listOf(trainingEntry(id = "old", day = "2026-10-01", stuckIds = listOf("guard:bottom"))) +
            listOf("2026-11-01", "2026-11-02", "2026-11-03").map { trainingEntry(id = it, day = it) }
        assertEquals(DrillReason.Next, pickDrill(library(), entries)?.reason)
    }

    @Test
    fun `passes over techniques with no drill and finds nothing once every drill is learned`() {
        val entry = trainingEntry(techniqueIds = listOf("pass"))
        assertEquals(DrillReason.Next, pickDrill(library(), listOf(entry))?.reason)
        assertNull(pickDrill(library(learned("upa"), learned("shrimp"), learned("sweep")), listOf(entry)))
    }

    @Test
    fun `counts the learned techniques among those that start at a position`() {
        val lib = library(learned("upa"), review(CardKind.Transition, "shrimp", intervalDays = 1), learned("sweep"))
        assertEquals(PositionProgress(total = 2, learned = 1), positionProgress(lib, "mount"))
        assertEquals(PositionProgress(0, 0), positionProgress(lib, "nowhere"))
    }
}
