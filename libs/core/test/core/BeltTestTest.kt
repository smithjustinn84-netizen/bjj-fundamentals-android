package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.seed.STARTER_BLUE_BELT_TEST_IDS
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.review
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BeltTestTest {
    @Test
    fun `uses the stored flag when there is one`() {
        assertTrue(isOnBlueBeltTest(transition(blueBeltTest = true)))
        assertFalse(isOnBlueBeltTest(transition(blueBeltTest = false)))
        // A starter technique the user took off the list stays off.
        assertFalse(isOnBlueBeltTest(transition(id = "scissor-sweep", blueBeltTest = false)))
    }

    @Test
    fun `defaults techniques stored before the flag existed to starter ones on and the rest off`() {
        assertTrue(isOnBlueBeltTest(transition(id = "scissor-sweep")))
        assertFalse(isOnBlueBeltTest(transition(id = "my-own-move")))
    }

    @Test
    fun `matches starter blue belt test ids in a fresh installation excluding self-defense and omoplata`() {
        val transitions = starterContent(0).transitions
        assertTrue(transitions.isNotEmpty())
        assertTrue(transitions.all { it.blueBeltTest == (it.id in STARTER_BLUE_BELT_TEST_IDS) })
        val off = transitions.filter { it.blueBeltTest != true }.map { it.id }
        assertEquals(
            listOf(
                "wrist-grab-release",
                "front-choke-defence",
                "bear-hug-escape",
                "standing-headlock-escape",
                "omoplata-guard",
                "ground-headlock-escape",
            ),
            off,
        )
    }

    private val lib = buildLibrary(
        emptyList(),
        listOf(position(id = "guard", order = 1), position(id = "mount", order = 2)),
        listOf(
            transition(id = "choke", type = TransitionType.Submission, fromId = "mount", blueBeltTest = true),
            transition(id = "armbar", type = TransitionType.Submission, fromId = "guard", blueBeltTest = true),
            transition(id = "sweep", type = TransitionType.Sweep, fromId = "guard", blueBeltTest = true),
            transition(id = "not-tested", type = TransitionType.Pass, fromId = "guard", blueBeltTest = false),
        ),
        listOf(
            review(CardKind.Transition, "armbar", intervalDays = 5, repetitions = 1),
            review(CardKind.Transition, "sweep", intervalDays = 1, repetitions = 1),
            review(CardKind.Transition, "not-tested", intervalDays = 5, repetitions = 1),
        ),
    )

    @Test
    fun `groups listed techniques by type in type order and leaves out empty types`() {
        val groups = blueBeltTestList(lib).groups.map { group -> group.type to group.techniques.map { it.id } }
        val expected = listOf(
            TransitionType.Sweep to listOf("sweep"),
            TransitionType.Submission to listOf("armbar", "choke"),
        )
        assertEquals(expected, groups)
    }

    @Test
    fun `counts only listed techniques and only learned cards as learned`() {
        val list = blueBeltTestList(lib)
        assertEquals(3, list.total)
        assertEquals(1, list.learned)
    }

    @Test
    fun `is empty when nothing is on the list`() {
        val none = buildLibrary(emptyList(), emptyList(), listOf(transition(id = "x")), emptyList())
        assertEquals(BeltTestList(emptyList(), 0, 0), blueBeltTestList(none))
    }

    @Test
    fun `a technique's drill is the stored one including one the user cleared`() {
        assertEquals("My own drill", techniqueDrill(transition(id = "scissor-sweep", drill = "My own drill")))
        assertEquals("", techniqueDrill(transition(id = "scissor-sweep", drill = "")))
    }

    @Test
    fun `a starter technique stored without a drill gets the starter one and any other none`() {
        assertTrue("10 reps" in techniqueDrill(transition(id = "scissor-sweep")))
        assertEquals("", techniqueDrill(transition(id = "my-own-move")))
    }
}
