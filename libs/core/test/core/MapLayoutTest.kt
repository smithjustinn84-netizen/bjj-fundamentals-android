package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.MAP_HORIZONTAL
import dev.justinsmith.bjj.core.golden.MAP_VERTICAL
import dev.justinsmith.bjj.core.golden.MapGolden
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MapLayoutTest {
    private val starter = starterContent(0)
    private val lib = buildLibrary(starter.concepts, starter.positions, starter.transitions, emptyList())

    private fun assertMatches(golden: MapGolden, orientation: MapOrientation) {
        val layout = layoutPositionMap(lib.positions, lib.transitions, orientation)
        assertEquals(golden.width, layout.width)
        assertEquals(golden.height, layout.height)
        assertEquals(golden.lanes, layout.lanes.map { it.category.key to listOf(it.labelX, it.labelY) })
        val nodes = layout.nodes.map { it.id to listOf(it.x, it.y, it.width, it.height, it.submissions.toDouble()) }
        assertEquals(golden.nodes, nodes)
        val edges = layout.edges.map { edge ->
            val (start, control, end) = edge.curve
            edge.id to listOf(edge.bow, start.x, start.y, control.x, control.y, end.x, end.y)
        }
        assertEquals(golden.edges, edges)
    }

    @Test
    fun `lays the starter map out across the screen as the web app does`() {
        assertMatches(MAP_HORIZONTAL, MapOrientation.Horizontal)
    }

    @Test
    fun `lays the starter map out down the screen as the web app does`() {
        assertMatches(MAP_VERTICAL, MapOrientation.Vertical)
    }

    private val positions = listOf(
        position(id = "guard", category = PositionCategory.Guard, order = 1),
        position(id = "mount", category = PositionCategory.Top, order = 2),
        position(id = "standing", category = PositionCategory.Standing, order = 3),
    )

    @Test
    fun `puts lanes in dominance order whatever order the positions come in`() {
        val layout = layoutPositionMap(positions, emptyList(), MapOrientation.Horizontal)
        val lanes = listOf(PositionCategory.Standing, PositionCategory.Guard, PositionCategory.Top)
        assertEquals(lanes, layout.lanes.map { it.category })
        val x = layout.nodes.associate { it.id to it.x }
        assertTrue(x.getValue("standing") < x.getValue("guard") && x.getValue("guard") < x.getValue("mount"))
    }

    @Test
    fun `counts submissions on their position and draws no arrow for them or for a loop or a missing end`() {
        val transitions = listOf(
            transition(id = "choke", type = TransitionType.Submission, fromId = "mount", toId = ""),
            transition(id = "armbar", type = TransitionType.Submission, fromId = "mount", toId = ""),
            transition(id = "retain", fromId = "guard", toId = "guard"),
            transition(id = "lost", fromId = "guard", toId = "deleted"),
            transition(id = "sweep", fromId = "guard", toId = "mount"),
        )
        val layout = layoutPositionMap(positions, transitions, MapOrientation.Vertical)
        assertEquals(2, layout.nodes.first { it.id == "mount" }.submissions)
        assertEquals(listOf("sweep"), layout.edges.map { it.id })
    }

    @Test
    fun `gives an arrow and its return trip and a second arrow the same way each a curve of its own`() {
        val transitions = listOf(
            transition(id = "sweep", fromId = "guard", toId = "mount"),
            transition(id = "escape", type = TransitionType.Escape, fromId = "mount", toId = "guard"),
            transition(id = "sweep-2", fromId = "guard", toId = "mount"),
        )
        val curves = layoutPositionMap(positions, transitions, MapOrientation.Horizontal).edges.map { it.curve.control }
        assertEquals(3, curves.toSet().size)
        assertNotEquals(curves[0], curves[1])
    }

    @Test
    fun `is an empty sheet for an empty library`() {
        val layout = layoutPositionMap(emptyList(), emptyList(), MapOrientation.Horizontal)
        assertEquals(emptyList(), layout.nodes)
        assertEquals(emptyList(), layout.edges)
    }
}
