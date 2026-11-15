package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals

class StarterTest {
    private val positions = listOf(position(id = "guard"), position(id = "mount"))
    private val techniques = listOf(
        transition(id = "sweep", fromId = "guard", toId = "mount"),
        transition(id = "choke", type = TransitionType.Submission, fromId = "mount", toId = ""),
        transition(id = "throw", fromId = "standing", toId = "mount"),
        transition(id = "pull", fromId = "guard", toId = "standing"),
    )

    private fun missing(stored: List<Transition>, dismissed: List<StarterDismissal> = emptyList()) =
        findMissingStarterTechniques(techniques, stored, positions, dismissed).map { it.id }

    @Test
    fun `offers starter techniques that are not stored including submissions with no destination`() {
        val stored = listOf(transition(id = "sweep", name = "Renamed by the user"), transition(id = "my-own"))
        assertEquals(listOf("choke"), missing(stored))
    }

    @Test
    fun `leaves out techniques whose start or end position the library no longer has`() {
        assertEquals(listOf("sweep", "choke"), missing(emptyList()))
    }

    @Test
    fun `leaves out techniques the user dismissed and is empty when everything is stored`() {
        assertEquals(listOf("choke"), missing(emptyList(), listOf(StarterDismissal("sweep", 1))))
        assertEquals(emptyList(), missing(techniques))
    }

    private val concepts = listOf(concept(id = "base"), concept(id = "angles"), concept(id = "breathing"))

    @Test
    fun `offers starter concepts that are not stored whatever the stored copy now says`() {
        val stored = listOf(concept(id = "base", name = "Renamed by the user"), concept(id = "my-own"))
        assertEquals(
            listOf("angles", "breathing"),
            findMissingStarterConcepts(concepts, stored, emptyList()).map {
                it.id
            },
        )
    }

    @Test
    fun `leaves out concepts the user dismissed and is empty when everything is stored`() {
        val dismissed = listOf(StarterDismissal("angles", 1))
        assertEquals(
            listOf("base", "breathing"),
            findMissingStarterConcepts(concepts, emptyList(), dismissed).map {
                it.id
            },
        )
        assertEquals(emptyList(), findMissingStarterConcepts(concepts, concepts, emptyList()))
    }

    private val starterPositions = listOf(
        position(id = "guard", conceptIds = listOf("base", "angles")),
        position(id = "mount", conceptIds = listOf("breathing", "angles")),
    )

    @Test
    fun `appends added concepts to stored starter positions that list them after the user's own links`() {
        val stored = listOf(position(id = "guard", conceptIds = listOf("mine")), position(id = "mount"))
        val linked = linkAddedConcepts(setOf("angles"), starterPositions, stored)
        assertEquals(
            listOf("guard" to listOf("mine", "angles"), "mount" to listOf("angles")),
            linked.map {
                it.id to
                    it.conceptIds
            },
        )
    }

    @Test
    fun `links nothing to a position that lists the concept already or is not a starter position`() {
        val stored = listOf(position(id = "guard", conceptIds = listOf("angles")), position(id = "my-own"))
        assertEquals(emptyList(), linkAddedConcepts(setOf("angles"), starterPositions, stored))
    }

    @Test
    fun `only links concepts that were added`() {
        assertEquals(
            emptyList(),
            linkAddedConcepts(setOf("breathing"), starterPositions, listOf(position(id = "guard"))),
        )
    }
}
