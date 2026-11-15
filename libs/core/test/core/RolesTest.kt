package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RolesTest {
    private val positions = listOf(
        position(id = "guard", category = PositionCategory.Guard, order = 1),
        position(id = "mount", category = PositionCategory.Top, order = 2),
    )
    private val lib = buildLibrary(emptyList(), positions, emptyList(), emptyList())

    @Test
    fun `uses the stored role when there is one`() {
        val sweep = transition(id = "scissor-sweep", fromId = "guard", role = PositionRole.Top)
        assertEquals(PositionRole.Top, performerRole(lib, sweep))
    }

    @Test
    fun `gives a starter technique stored without a role the starter answer`() {
        // By type alone this would be "top": the starter content knows it is done from the guard's bottom.
        val stored = transition(id = "half-to-full-guard", type = TransitionType.Transition, fromId = "guard")
        assertEquals(PositionRole.Bottom, performerRole(lib, stored))
    }

    @Test
    fun `falls back to the usual role for the type and for a submission to where it starts`() {
        val fromGuard = transition(id = "my-armbar", type = TransitionType.Submission, fromId = "guard")
        val fromMount = transition(id = "my-choke", type = TransitionType.Submission, fromId = "mount")
        assertEquals(PositionRole.Bottom, performerRole(lib, fromGuard))
        assertEquals(PositionRole.Top, performerRole(lib, fromMount))
        assertEquals(PositionRole.Bottom, defaultRole(TransitionType.Escape, PositionCategory.Top))
        assertEquals(PositionRole.Top, defaultRole(TransitionType.Pass, null))
    }

    @Test
    fun `splits the techniques that start at a position by who does them`() {
        val lib = buildLibrary(
            emptyList(),
            positions,
            listOf(
                transition("pass", "Pass", TransitionType.Pass, fromId = "guard", role = PositionRole.Top),
                transition("sweep", "Sweep", TransitionType.Sweep, fromId = "guard", role = PositionRole.Bottom),
                transition("armbar", "Armbar", TransitionType.Submission, fromId = "guard", role = PositionRole.Bottom),
                transition("escape", "Escape", TransitionType.Escape, "mount", "guard", role = PositionRole.Bottom),
            ),
            emptyList(),
        )
        val options = positionOptions(lib, "guard")
        assertEquals(listOf("pass"), options.top.map { it.id })
        assertEquals(listOf("armbar", "sweep"), options.bottom.map { it.id })
        // Nothing for the person on top of mount: a gap.
        assertEquals(emptyList(), positionOptions(lib, "mount").top)
    }

    @Test
    fun `gives both people something to do from every starter position`() {
        val starter = starterContent(0)
        val lib = buildLibrary(starter.concepts, starter.positions, starter.transitions, emptyList())
        val gaps = starter.positions.flatMap { p ->
            PositionRole.entries.filter { positionOptions(lib, p.id)[it].isEmpty() }.map { "${p.id} ${it.key}" }
        }
        assertEquals(emptyList(), gaps)
    }

    @Test
    fun `the end side is the stored one and then the starter answer`() {
        val stored = transition(id = "back-escape", type = TransitionType.Escape, endRole = PositionRole.Bottom)
        assertEquals(PositionRole.Bottom, endRole(lib, stored))
        // Escaping the back by the starter method leaves you on top, in their guard.
        assertEquals(PositionRole.Top, endRole(lib, transition(id = "back-escape", type = TransitionType.Escape)))
    }

    @Test
    fun `otherwise a sweep or pass or takedown ends on top and anything else on its starting side`() {
        assertEquals(PositionRole.Top, endRole(lib, transition(id = "my-sweep", fromId = "guard")))
        val escape = transition(id = "my-escape", type = TransitionType.Escape, fromId = "mount")
        assertEquals(PositionRole.Bottom, endRole(lib, escape))
        val move = transition("my-move", type = TransitionType.Transition, fromId = "mount", role = PositionRole.Bottom)
        assertEquals(PositionRole.Bottom, endRole(lib, move))
    }

    @Test
    fun `has a usual end side for each type before anything is saved`() {
        assertEquals(PositionRole.Top, defaultEndRole(TransitionType.Sweep, PositionRole.Bottom))
        assertEquals(PositionRole.Bottom, defaultEndRole(TransitionType.Escape, PositionRole.Bottom))
        assertNull(defaultEndRole(TransitionType.Submission, PositionRole.Top))
    }

    @Test
    fun `the end side is nothing for a submission and set for every other starter technique`() {
        assertNull(endRole(lib, transition(id = "my-choke", type = TransitionType.Submission, fromId = "mount")))
        val unset = starterContent(0).transitions.filter { !endsTheRoll(it.type) && it.endRole == null }
        assertEquals(emptyList(), unset.map { it.id })
    }
}
