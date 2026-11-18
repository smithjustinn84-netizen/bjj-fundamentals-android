package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.golden.LESSON_CARDS
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.review
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val NOW = 1_000_000_000_000

class LessonsTest {
    private val starter = starterContent(0)
    private val first = FOUNDATION_LESSONS.first()
    private val surviving = FOUNDATION_LESSONS[1]

    private fun library(reviews: List<ReviewState> = emptyList()) =
        buildLibrary(starter.concepts, starter.positions, starter.transitions, reviews)

    private val empty = buildLibrary(emptyList(), emptyList(), emptyList(), emptyList())

    @Test
    fun `name only starter items and each has a title and a summary and a practice script`() {
        val lib = library()
        val unknown = FOUNDATION_LESSONS.flatMap { lesson ->
            lesson.conceptIds.filterNot { it in lib.conceptById } +
                lesson.positionIds.filterNot { it in lib.positionById } +
                lesson.transitionIds.filterNot { it in lib.transitionById }
        }
        assertEquals(emptyList(), unknown)
        val blank = FOUNDATION_LESSONS.filter { it.title.isEmpty() || it.summary.isEmpty() || it.practice.isEmpty() }
        assertEquals(emptyList(), blank.map { it.id })
        assertEquals(FOUNDATION_LESSONS.size, FOUNDATION_LESSONS.map { it.id }.toSet().size)
    }

    @Test
    fun `teach every lesson's cards in the web app's order`() {
        val lib = library()
        assertEquals(LESSON_CARDS, FOUNDATION_LESSONS.associate { l -> l.id to lessonCards(lib, l).map { it.cardId } })
    }

    @Test
    fun `leave out what the library no longer has`() {
        val lib = buildLibrary(starter.concepts, starter.positions, emptyList(), emptyList())
        val kinds = listOf(CardKind.Concept, CardKind.Position, CardKind.Concept, CardKind.Concept)
        assertEquals(kinds, lessonCards(lib, surviving).map { it.kind })
        assertEquals(emptyList(), lessonCards(empty, surviving))
    }

    @Test
    fun `count progress from the review state of the cards and move on once a lesson is learned`() {
        assertEquals(LessonProgress(total = 5, started = 0, learned = 0), lessonProgress(library(), first))
        assertEquals(first.id, currentLesson(library())?.id)
        val reviews = lessonCards(library(), first).map { review(it.kind, it.id, intervalDays = 8) }
        assertEquals(LessonProgress(5, started = 3, learned = 3), lessonProgress(library(reviews.take(3)), first))
        assertEquals(FOUNDATION_LESSONS[1].id, currentLesson(library(reviews))?.id)
        assertNull(currentLesson(empty))
    }

    @Test
    fun `are found by id`() {
        assertEquals("Escaping the mount", findLesson("escaping-the-mount")?.title)
        assertNull(findLesson("nothing"))
        assertNull(findLesson(null))
    }

    @Test
    fun `a lesson's queue holds only its cards with the due ones first then new ones up to the limit`() {
        val lib = library(
            listOf(
                review(CardKind.Position, "side-control", due = NOW - 1),
                review(CardKind.Concept, "breathing", due = NOW + 1), // not due
                review(CardKind.Concept, "posture", due = NOW - 1), // due, but in another lesson
            ),
        )
        val result = buildLessonQueue(lib, surviving, 2, NOW)
        assertEquals(listOf("side-control", "side-control-escape", "frames"), result.queue.map { it.id })
        assertEquals(6, result.fresh.size)
    }
}
