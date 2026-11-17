package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.TECHNIQUE_LINKS
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChainsTest {
    private val starter = starterContent(0)
    private val lib = buildLibrary(starter.concepts, starter.positions, starter.transitions, emptyList())

    @Test
    fun `links every starter technique as the web app does`() {
        val actual = lib.transitions
            .associate { it.id to techniqueLinks(lib, it.id) }
            .filterValues { !it.isEmpty }
            .mapValues { (_, links) ->
                Triple(
                    links.defences.map { it.id },
                    links.answers.map { it.id },
                    links.followUps.map { "${it.technique.id}|${it.whenText}" },
                )
            }
        assertEquals(TECHNIQUE_LINKS, actual)
    }

    @Test
    fun `names only starter techniques`() {
        assertEquals(emptySet(), CHAIN_IDS - lib.transitionById.keys)
    }

    @Test
    fun `lists no follow-up twice for one technique`() {
        lib.transitions.forEach { technique ->
            val followUps = techniqueLinks(lib, technique.id).followUps.map { it.technique.id }
            assertEquals(followUps.distinct(), followUps, technique.id)
        }
    }

    @Test
    fun `leaves out techniques the library no longer has and has nothing for one the user added`() {
        val without = buildLibrary(
            emptyList(),
            starter.positions,
            listOf(transition(id = "armbar-guard"), transition(id = "own")),
            emptyList(),
        )
        assertTrue(techniqueLinks(without, "armbar-guard").isEmpty)
        assertTrue(techniqueLinks(without, "own").isEmpty)
        assertFalse(techniqueLinks(lib, "armbar-guard").isEmpty)
    }
}
