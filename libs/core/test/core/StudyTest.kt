package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.review
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val NOW = 1_000_000_000_000

class StudyTest {
    private val concepts =
        listOf(concept(id = "c1", order = 1), concept(id = "c2", order = 2), concept("c3", order = 3))
    private val positions = listOf(position(id = "p1"))
    private val transitions = listOf(transition(id = "t1"))
    private val plain = buildLibrary(concepts, positions, transitions, emptyList())

    @Test
    fun `puts due cards first and most overdue first then new cards in learning order up to the limit`() {
        val lib = buildLibrary(
            concepts,
            positions,
            transitions,
            listOf(
                review(CardKind.Position, "p1", due = NOW - DAY_MS),
                review(CardKind.Concept, "c3", due = NOW - 3 * DAY_MS),
                review(CardKind.Concept, "c1", due = NOW + DAY_MS), // not due yet: excluded
            ),
        )
        val result = buildQueue(lib, Deck.All, 1, NOW)
        assertEquals(listOf("c3", "p1"), result.due.map { it.id })
        // The one technique is a sweep, so the bottom side of its position has a situation card too.
        assertEquals(listOf("c2", "t1", "p1:bottom"), result.fresh.map { it.id })
        assertEquals(listOf("c3", "p1", "c2"), result.queue.map { it.id })
    }

    @Test
    fun `filters by deck`() {
        assertEquals(listOf(StudyItem(CardKind.Transition, "t1")), buildQueue(plain, Deck.Transition, 10, NOW).queue)
    }

    @Test
    fun `keeps the blue belt deck to techniques on the test list`() {
        val techniques = listOf(
            transition(id = "listed", blueBeltTest = true),
            transition(id = "unlisted", blueBeltTest = false),
        )
        val lib = buildLibrary(concepts, positions, techniques, emptyList())
        assertEquals(listOf(StudyItem(CardKind.Transition, "listed")), buildQueue(lib, Deck.BlueBelt, 10, NOW).queue)
    }

    @Test
    fun `puts the new cards the training log names first in its order`() {
        val techniques = listOf(transition("t1", "A"), transition("t2", "B"), transition("t3", "C"))
        val lib = buildLibrary(
            concepts,
            positions,
            techniques,
            listOf(review(CardKind.Transition, "t2", due = NOW + DAY_MS)),
        )
        // t2 was practised too, but it is already being studied and not due: only fresh cards jump the queue.
        val result = buildQueue(lib, Deck.All, 2, NOW, listOf("transition:t3", "transition:t2", "transition:t1"))
        assertEquals(listOf("t3", "t1"), result.queue.map { it.id })
        assertEquals(2, result.fromClass)
        assertEquals(listOf("t3", "t1", "c1", "p1", "c2", "c3", "p1:bottom"), result.fresh.map { it.id })
        assertEquals(0, buildQueue(lib, Deck.Concept, 2, NOW, listOf("transition:t3")).fromClass)
    }

    @Test
    fun `has a situations deck with one card per side that has options after the techniques`() {
        val situation = StudyItem(CardKind.Situation, "p1:bottom")
        assertEquals(listOf(situation), buildQueue(plain, Deck.Situation, 10, NOW).queue)
        assertEquals(situation, buildQueue(plain, Deck.All, 10, NOW).queue.last())
    }

    @Test
    fun `brings new cards in lesson order but keeps the concept and position decks in their own order`() {
        val lib = buildLibrary(
            concepts,
            listOf(
                position(id = "guard", order = 1),
                position(id = "pin", category = PositionCategory.Top, order = 2, conceptIds = listOf("c3")),
            ),
            listOf(
                transition(id = "sweep", fromId = "guard", role = PositionRole.Bottom),
                transition(id = "upa", type = TransitionType.Escape, fromId = "pin", role = PositionRole.Bottom),
            ),
            emptyList(),
        )
        val queued = { deck: Deck -> buildQueue(lib, deck, 4, NOW).queue.map { it.id } }
        assertEquals(listOf("c1", "pin", "upa", "c3"), queued(Deck.All))
        assertEquals(listOf("upa", "sweep"), queued(Deck.Transition))
        assertEquals(listOf("c1", "c2", "c3"), queued(Deck.Concept))
        assertEquals(listOf("guard", "pin"), queued(Deck.Position))
    }

    @Test
    fun `adds no new cards when the limit is zero`() {
        assertEquals(emptyList(), buildQueue(plain, Deck.All, 0, NOW).queue)
    }

    @Test
    fun `counts started and learned cards`() {
        val lib = buildLibrary(
            concepts,
            positions,
            transitions,
            listOf(review(CardKind.Concept, "c1", intervalDays = 8), review(CardKind.Concept, "c2", intervalDays = 1)),
        )
        assertEquals(ProgressSummary(total = 6, started = 2, learned = 1, percentLearned = 17), progressSummary(lib))
    }

    @Test
    fun `progress is zero for an empty library`() {
        val empty = buildLibrary(emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(0, progressSummary(empty).percentLearned)
    }

    @Test
    fun `lists cards forgotten more than once and not learned with the most forgotten first then lowest ease`() {
        val lib = buildLibrary(
            concepts,
            positions,
            transitions,
            listOf(
                review(CardKind.Concept, "c1", lapses = 2, ease = 2.5, intervalDays = 1),
                review(CardKind.Concept, "c2", lapses = 3, ease = 2.5, intervalDays = 0),
                review(CardKind.Concept, "c3", lapses = 4, intervalDays = 8), // forgotten before, but learned again
                review(CardKind.Position, "p1", lapses = 0, intervalDays = 1), // never forgotten
                review(CardKind.Transition, "t1", lapses = 2, ease = 1.3, intervalDays = 1),
                review(CardKind.Situation, "p1:bottom", lapses = 1, intervalDays = 0), // one miss is not a pattern
            ),
        )
        assertEquals(listOf("c2", "t1", "c1"), stickingPoints(lib, 5).map { it.id })
        assertEquals(listOf("c2"), stickingPoints(lib, 1).map { it.id })
    }

    @Test
    fun `has no sticking points before anything has been forgotten`() {
        assertEquals(emptyList(), stickingPoints(plain, 5))
    }

    @Test
    fun `names an item by its name and a situation by its position and side or null once it is gone`() {
        assertEquals("Transition", cardName(plain, StudyItem(CardKind.Transition, "t1")))
        assertEquals("Position, playing guard", cardName(plain, StudyItem(CardKind.Situation, "p1:bottom")))
        assertNull(cardName(plain, StudyItem(CardKind.Situation, "p1:top")))
        assertNull(cardName(plain, StudyItem(CardKind.Concept, "gone")))
    }

    @Test
    fun `lists the concepts not learned yet in learning order up to the limit`() {
        val lib = buildLibrary(
            concepts,
            positions,
            transitions,
            listOf(
                review(CardKind.Concept, "c1", intervalDays = 8), // learned
                review(CardKind.Concept, "c2", intervalDays = 1), // started, not learned
            ),
        )
        assertEquals(listOf("c2", "c3"), conceptsToLearn(lib, 5).map { it.id })
        assertEquals(listOf("c2"), conceptsToLearn(lib, 1).map { it.id })
    }
}
