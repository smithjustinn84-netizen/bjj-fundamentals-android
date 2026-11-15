package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryTest {
    private val lib = buildLibrary(
        listOf(concept(id = "b", order = 2), concept(id = "a", order = 1)),
        listOf(position(id = "mount", order = 2), position(id = "guard", order = 1)),
        listOf(
            transition(id = "orphan", fromId = "deleted", name = "A"),
            transition(id = "from-mount", fromId = "mount", name = "B"),
            transition(id = "from-guard-z", fromId = "guard", name = "Z"),
            transition(id = "from-guard-a", fromId = "guard", name = "A"),
        ),
        emptyList(),
    )

    @Test
    fun `sorts concepts and positions by learning order`() {
        assertEquals(listOf("a", "b"), lib.concepts.map { it.id })
        assertEquals(listOf("guard", "mount"), lib.positions.map { it.id })
    }

    @Test
    fun `sorts transitions by starting position then name with orphans last`() {
        assertEquals(listOf("from-guard-a", "from-guard-z", "from-mount", "orphan"), lib.transitions.map { it.id })
    }

    @Test
    fun `sorts names without regard to case`() {
        val names = listOf("banana", "Apple", "apple", "Cherry").sortedWith(::compareNames)
        assertEquals(listOf("apple", "Apple", "banana", "Cherry"), names)
    }

    @Test
    fun `has no study card for an item never reviewed`() {
        assertNull(reviewOf(lib, CardKind.Position, "mount"))
    }

    @Test
    fun `looks names up by kind and returns null for missing items`() {
        assertEquals("Position", itemName(lib, ItemKind.Position, "mount"))
        assertEquals("A", itemName(lib, ItemKind.Transition, "orphan"))
        assertNull(itemName(lib, ItemKind.Concept, "nope"))
    }

    private val connected = buildLibrary(
        emptyList(),
        listOf(position(id = "guard"), position(id = "mount")),
        listOf(
            transition(id = "sweep", fromId = "guard", toId = "mount"),
            transition(id = "escape", fromId = "mount", toId = "guard"),
            transition(id = "reguard", fromId = "guard", toId = "guard"),
            transition(id = "choke", type = TransitionType.Submission, fromId = "mount", toId = ""),
        ),
        emptyList(),
    )

    @Test
    fun `splits techniques into those leaving and those arriving and counts a loop once`() {
        val (outgoing, incoming) = connections(connected, "guard")
        assertEquals(listOf("reguard", "sweep"), outgoing.map { it.id }.sorted())
        assertEquals(listOf("escape"), incoming.map { it.id })
    }

    @Test
    fun `includes submissions as outgoing`() {
        assertEquals(listOf("choke", "escape"), connections(connected, "mount").outgoing.map { it.id }.sorted())
    }
}
