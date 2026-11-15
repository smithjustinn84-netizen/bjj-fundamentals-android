package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SituationsTest {
    private val lib = buildLibrary(
        emptyList(),
        listOf(
            position(id = "guard", name = "Closed Guard", category = PositionCategory.Guard, order = 1),
            position(id = "mount", name = "Mount", category = PositionCategory.Top, order = 2),
        ),
        listOf(
            transition(id = "sweep", type = TransitionType.Sweep, fromId = "guard", role = PositionRole.Bottom),
            transition(id = "pass", type = TransitionType.Pass, fromId = "guard", role = PositionRole.Top),
            transition(id = "escape", type = TransitionType.Escape, fromId = "mount", role = PositionRole.Bottom),
        ),
        emptyList(),
    )

    @Test
    fun `lists each side of each position that has something to do in learning order`() {
        assertEquals(listOf("guard:top", "guard:bottom", "mount:bottom"), allSituations(lib).map { it.id })
    }

    @Test
    fun `finds a situation by id with its options and names it`() {
        val found = findSituation(lib, "mount:bottom")
        assertEquals(listOf("escape"), found?.options?.map { it.id })
        assertEquals("Mount, underneath", found?.let(::situationName))
    }

    @Test
    fun `finds nothing for a side with no options or a missing position or a bad id`() {
        assertNull(findSituation(lib, "mount:top"))
        assertNull(findSituation(lib, "gone:bottom"))
        assertNull(findSituation(lib, "mount"))
        assertNull(findSituation(lib, "mount:sideways"))
    }

    @Test
    fun `keeps a position id that contains a colon intact`() {
        assertEquals("my:position", situationPositionId(situationId("my:position", PositionRole.Top)))
    }
}
