package dev.justinsmith.bjj.ui.learn

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.MapOrientation
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.ui.common.OtherEnd
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** What the pages behind the Learn tab are told to draw, from the starter library and what a test then stores. */
class LearnUiStatesTest {
    private val t: TestCore = testCore()

    private suspend fun snapshot(): Snapshot = t.core.queries.snapshot()

    @Test
    fun `lessons list their cards in order and mark the first one not yet learned`() = runTest {
        val lessons = lessonsUiState(snapshot().library).lessons
        assertEquals(FOUNDATION_LESSONS.map { it.id }, lessons.map { it.lesson.id })
        assertEquals((1..lessons.size).toList(), lessons.map { it.number })
        assertEquals(listOf(true) + List(lessons.size - 1) { false }, lessons.map { it.current })
        assertTrue(lessons.all { lesson -> lesson.cards.size == lesson.progress.total && lesson.cards.isNotEmpty() })
        assertTrue(lessons.flatMap { it.cards }.all { it.name.isNotEmpty() && it.progress == Progress.New })
    }

    @Test
    fun `the belt test groups its techniques by type and says where each starts`() = runTest {
        val before = blueBeltUiState(snapshot())
        assertEquals(before.total, before.groups.sumOf { it.techniques.size })
        assertEquals(0, before.learned)
        assertTrue(before.groups.flatMap { it.techniques }.all { it.from != null })
        assertTrue(before.missingStarter.isEmpty())

        val escape = before.groups.first { it.type == TransitionType.Escape }.techniques.first()
        t.core.reviews.recordReview(StudyItem(CardKind.Transition, escape.id), Grade.Good)
        t.core.items.deleteItem(ItemKind.Transition, before.groups.last().techniques.last().id)
        val after = blueBeltUiState(snapshot())
        assertEquals(Progress.Learning, after.groups.flatMap { it.techniques }.first { it.id == escape.id }.progress)
        assertEquals(before.total - 1, after.total)
        assertEquals(1, after.missingStarter.size)
    }

    @Test
    fun `positions come grouped by category with a map for either way the window runs`() = runTest {
        val state = positionsUiState(snapshot())
        val listed = state.groups.flatMap { it.positions }
        assertEquals(snapshot().library.positions.map { it.id }.toSet(), listed.map { it.id }.toSet())
        assertTrue(state.groups.all { it.positions.isNotEmpty() })
        assertTrue(state.orphans.isEmpty())
        assertEquals(MapOrientation.entries.toSet(), state.map.layouts.keys)
        for (layout in state.map.layouts.values) {
            assertEquals(state.map.positions.keys, layout.nodes.map { it.id }.toSet())
        }
        val mount = state.map.positions.getValue("mount")
        assertTrue(mount.outgoing.isNotEmpty() && mount.incoming.all { it.otherEnd is OtherEnd.From })

        // A technique whose starting position is deleted with it is gone too; one left behind is an orphan.
        t.storage.positions.remove("mount")
        val orphaned = positionsUiState(snapshot())
        assertEquals(mount.outgoing.map { it.id }.toSet(), orphaned.orphans.map { it.id }.toSet())
        assertFalse(orphaned.groups.flatMap { it.positions }.any { it.id == "mount" })
    }

    @Test
    fun `a technique's page knows its ends its neighbours and how it meets other techniques`() = runTest {
        val armbar = assertIs<TransitionDetailUiState.Ready>(transitionDetailUiState(snapshot(), "armbar-guard"))
        assertEquals(TransitionType.Submission, armbar.transition.type)
        assertEquals("closed-guard", assertNotNull(armbar.from).id)
        assertEquals(listOf("armbar-defence"), armbar.links.defences.map { it.id })
        assertTrue(armbar.onBlueBeltTest)

        val defence = assertIs<TransitionDetailUiState.Ready>(transitionDetailUiState(snapshot(), "armbar-defence"))
        assertEquals(listOf("armbar-guard"), defence.links.answers.map { it.id })
        assertEquals(TransitionDetailUiState.Missing, transitionDetailUiState(snapshot(), "nothing"))
    }
}
