package dev.justinsmith.bjj.ui.review

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What a study card shows, from the starter library. */
class CardFaceTest {
    private val t: TestCore = testCore()

    private suspend fun snapshot(): Snapshot = t.core.queries.snapshot()

    @Test
    fun `every kind of study card has a face and a card about nothing has none`() = runTest {
        val snapshot = snapshot()
        fun face(kind: CardKind, id: String) = cardFace(snapshot.library, snapshot.videos, StudyItem(kind, id))

        assertEquals("safety", assertIs<CardFace.ConceptCard>(face(CardKind.Concept, "safety")).concept.id)
        assertEquals("mount", assertIs<CardFace.PositionCard>(face(CardKind.Position, "mount")).position.id)
        val technique = assertIs<CardFace.TechniqueCard>(face(CardKind.Transition, "armbar-guard"))
        assertNotNull(technique.fromName)
        assertEquals(listOf("armbar-defence"), technique.links.defences.map { it.id })
        val situation = assertIs<CardFace.SituationCard>(face(CardKind.Situation, "mount:${PositionRole.Bottom.key}"))
        assertTrue(situation.options.isNotEmpty() && situation.goals.isNotEmpty())

        for (kind in CardKind.entries) assertNull(face(kind, "nothing"))
    }
}
