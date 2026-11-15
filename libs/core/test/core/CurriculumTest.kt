package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.LESSON_ORDER
import dev.justinsmith.bjj.core.golden.LIBRARY_CONCEPTS
import dev.justinsmith.bjj.core.golden.LIBRARY_POSITIONS
import dev.justinsmith.bjj.core.golden.LIBRARY_TRANSITIONS
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurriculumTest {
    private val concepts = listOf(
        concept(id = "x-tap", order = 1),
        concept(id = "x-frames", order = 2),
        concept(id = "x-bridge", order = 3),
        concept(id = "x-manners", order = 4), // no position lists it
    )
    private val positions = listOf(
        position(id = "x-guard", category = PositionCategory.Guard, order = 1, conceptIds = listOf("x-frames")),
        position("x-mount", category = PositionCategory.Top, order = 2, conceptIds = listOf("x-bridge", "x-frames")),
    )
    private val techniques = listOf(
        transition("x-armbar", "A", TransitionType.Submission, "x-guard", "", PositionRole.Bottom),
        transition("x-sweep", "B", TransitionType.Sweep, "x-guard", role = PositionRole.Bottom),
        transition("x-choke", "C", TransitionType.Submission, "x-mount", "", PositionRole.Top),
        transition("x-defence", "D", TransitionType.Escape, "x-mount", "x-mount", PositionRole.Bottom),
        transition("x-upa", "E", TransitionType.Escape, "x-mount", "x-guard", PositionRole.Bottom),
        transition("x-lost", "F", TransitionType.Escape, fromId = "deleted"),
    )

    @Test
    fun `teaches survival first then attacks with submissions last`() {
        val expected = listOf(
            "x-tap",
            // Survive: the pin comes before the guard although the guard is first in the list.
            "x-mount",
            // The way out of mount comes before the defence that leaves you under it.
            "x-upa",
            "x-frames",
            "x-defence",
            "x-bridge",
            "x-mount:bottom",
            "x-manners",
            "x-guard",
            // Attack: positions in their own order, the submission after the sweep.
            "x-sweep",
            "x-armbar",
            "x-guard:bottom",
            "x-choke",
            "x-mount:top",
            "x-lost",
        )
        assertEquals(expected, lessonOrder(buildLibrary(concepts, positions, techniques, emptyList())).map { it.id })
    }

    @Test
    fun `is empty for an empty library`() {
        assertEquals(emptyList(), lessonOrder(buildLibrary(emptyList(), emptyList(), emptyList(), emptyList())))
    }

    private val starter = starterContent(0)
    private val starterLib = buildLibrary(starter.concepts, starter.positions, starter.transitions, emptyList())

    @Test
    fun `sorts the starter library as the web app does`() {
        assertEquals(LIBRARY_CONCEPTS, starterLib.concepts.map { it.id })
        assertEquals(LIBRARY_POSITIONS, starterLib.positions.map { it.id })
        assertEquals(LIBRARY_TRANSITIONS, starterLib.transitions.map { it.id })
    }

    @Test
    fun `teaches the starter cards in the web app's order`() {
        assertEquals(LESSON_ORDER, lessonOrder(starterLib).map { it.cardId })
    }

    @Test
    fun `gives every starter card exactly once and every technique a place`() {
        val order = lessonOrder(starterLib)
        assertEquals(order.size, order.toSet().size)
        val situations = order.count { it.kind == CardKind.Situation }
        val items = starter.concepts.size + starter.positions.size + starter.transitions.size
        assertEquals(items + situations, order.size)
        val taught = lessonTechniques(starterLib).map { it.id }
        assertEquals(starter.transitions.size, taught.size)
        // The second lesson's mount escapes come before anything from the closed guard.
        assertTrue(taught.indexOf("elbow-knee-escape") < taught.indexOf("scissor-sweep"))
    }
}
