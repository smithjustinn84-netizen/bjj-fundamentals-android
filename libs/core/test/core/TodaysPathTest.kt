package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.review
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodaysPathTest {
    private val concepts = listOf(concept("frames", "Frames", order = 1), concept("base", "Base", order = 2))
    private val positions = listOf(position("mount", "Mount", PositionCategory.Top))
    private val techniques = listOf(
        transition("upa", "Upa", TransitionType.Escape, "mount", role = PositionRole.Bottom),
        transition("armbar", "Armbar", TransitionType.Submission, "mount", role = PositionRole.Top),
    )

    private fun path(vararg reviews: ReviewState, videoCount: Int = 0) =
        todaysPath(buildLibrary(concepts, positions, techniques, reviews.toList()), videoCount)
            .associateBy { it.kind }

    @Test
    fun `the steps come in the order a beginner takes them`() {
        assertEquals(
            PathStepKind.entries,
            todaysPath(buildLibrary(concepts, positions, techniques, emptyList()), 0).map {
                it.kind
            },
        )
    }

    @Test
    fun `a new library has every step open and each names the first thing to study`() {
        val steps = path()
        assertTrue(steps.values.none { it.done })
        assertEquals(StudyItem(CardKind.Transition, "upa"), steps.getValue(PathStepKind.Survive).next)
        assertEquals("Upa", steps.getValue(PathStepKind.Survive).nextName)
        assertEquals(StudyItem(CardKind.Concept, "frames"), steps.getValue(PathStepKind.Concepts).next)
        assertEquals(StudyItem(CardKind.Position, "mount"), steps.getValue(PathStepKind.Positions).next)
        assertEquals(StudyItem(CardKind.Transition, "armbar"), steps.getValue(PathStepKind.Attack).next)
        assertNull(steps.getValue(PathStepKind.Videos).next)
    }

    @Test
    fun `a step moves on as its cards are studied and is done when none is left`() {
        val one = path(review(CardKind.Concept, "frames"))
        assertEquals("Base", one.getValue(PathStepKind.Concepts).nextName)
        assertFalse(one.getValue(PathStepKind.Concepts).done)

        val all =
            path(
                review(CardKind.Concept, "frames"),
                review(CardKind.Concept, "base"),
                review(CardKind.Transition, "upa"),
            )
        assertTrue(all.getValue(PathStepKind.Concepts).done)
        assertNull(all.getValue(PathStepKind.Concepts).next)
        assertTrue(all.getValue(PathStepKind.Survive).done)
        assertFalse(all.getValue(PathStepKind.Attack).done)
    }

    @Test
    fun `a step with nothing in the library is not done and names nothing`() {
        val steps = todaysPath(buildLibrary(emptyList(), emptyList(), emptyList(), emptyList()), 0).associateBy {
            it.kind
        }
        for (kind in PathStepKind.entries) {
            assertFalse(steps.getValue(kind).done)
            assertNull(steps.getValue(kind).next)
        }
    }

    @Test
    fun `the videos step is done once a bookmark is saved`() {
        assertFalse(path().getValue(PathStepKind.Videos).done)
        assertTrue(path(videoCount = 1).getValue(PathStepKind.Videos).done)
    }
}
