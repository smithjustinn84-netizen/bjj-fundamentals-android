package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.StarterDismissal
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.testing.testCore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun video(kind: LinkedKind, id: String) = VideoInput("Clip", "dQw4w9WgXcQ", 0, kind, id, "")

@Suppress("detekt.TooManyFunctions") // One behaviour per test, side by side.
class ItemServiceTest {
    private val t = testCore()
    private val core = t.core
    private val storage = t.storage
    private val clock = t.clock

    @Test
    fun `creates with a new id and timestamps then updates keeping the creation time`() = runTest {
        val input = ConceptInput("Balance", "", listOf("a"), "", 99, "")
        clock.now = 1000
        val id = core.items.saveConcept(null, input)
        assertEquals(
            Triple("Balance", 1000L, 1000L),
            storage.concepts.get(id)?.let {
                Triple(it.name, it.createdAt, it.updatedAt)
            },
        )
        clock.now = 2000
        assertEquals(id, core.items.saveConcept(id, input.copy(name = "Balance 2")))
        assertEquals(
            Triple("Balance 2", 1000L, 2000L),
            storage.concepts.get(id)?.let {
                Triple(it.name, it.createdAt, it.updatedAt)
            },
        )
    }

    @Test
    fun `stores no destination for a submission whatever the caller passed`() = runTest {
        val input = TransitionInput(
            name = "Choke",
            type = TransitionType.Submission,
            fromId = "mount",
            toId = "back-control",
            role = null,
            endRole = null,
            steps = emptyList(),
            commonMistakes = emptyList(),
            drill = null,
            blueBeltTest = null,
            notes = "",
        )
        assertEquals("", storage.transitions.get(core.items.saveTransition(null, input))?.toId)
        val sweep = core.items.saveTransition(null, input.copy(type = TransitionType.Sweep))
        assertEquals("back-control", storage.transitions.get(sweep)?.toId)
    }

    @Test
    fun `deleting a position removes its techniques and their cards and unlinks videos`() = runTest {
        core.reviews.recordReview(StudyItem(CardKind.Position, "mount"), Grade.Good)
        core.reviews.recordReview(StudyItem(CardKind.Transition, "upa-escape"), Grade.Good) // from mount
        core.reviews.recordReview(StudyItem(CardKind.Transition, "scissor-sweep"), Grade.Good) // into mount
        core.reviews.recordReview(StudyItem(CardKind.Transition, "toreando-pass"), Grade.Good) // unrelated
        core.reviews.recordReview(StudyItem(CardKind.Situation, "mount:bottom"), Grade.Good)
        val onPosition = core.videos.add(video(LinkedKind.Position, "mount"))
        val onTransition = core.videos.add(video(LinkedKind.Transition, "upa-escape"))
        val elsewhere = core.videos.add(video(LinkedKind.Position, "turtle"))
        core.items.deleteItem(ItemKind.Position, "mount")
        assertNull(storage.positions.get("mount"))
        val transitions = storage.transitions.all()
        assertEquals(emptyList(), transitions.filter { it.fromId == "mount" || it.toId == "mount" })
        assertTrue(transitions.any { it.id == "toreando-pass" })
        assertEquals(listOf("transition:toreando-pass"), storage.reviews.all().map { it.id })
        assertEquals(LinkedKind.None to "", storage.videos.get(onPosition)?.let { it.linkedKind to it.linkedId })
        assertEquals(LinkedKind.None to "", storage.videos.get(onTransition)?.let { it.linkedKind to it.linkedId })
        assertEquals(
            LinkedKind.Position to "turtle",
            storage.videos.get(elsewhere)?.let {
                it.linkedKind to it.linkedId
            },
        )
    }

    @Test
    fun `deleting a concept removes it from every position that listed it`() = runTest {
        assertTrue("base" in assertNotNull(storage.positions.get("mount")).conceptIds)
        core.items.deleteItem(ItemKind.Concept, "base")
        assertNull(storage.concepts.get("base"))
        assertEquals(emptyList(), storage.positions.all().filter { "base" in it.conceptIds })
    }

    @Test
    fun `deleting a technique leaves its positions alone`() = runTest {
        val before = storage.positions.count()
        core.items.deleteItem(ItemKind.Transition, "upa-escape")
        assertNull(storage.transitions.get("upa-escape"))
        assertEquals(before, storage.positions.count())
    }

    @Test
    fun `adds back missing starter techniques without touching stored ones and only once`() = runTest {
        val before = storage.transitions.count()
        assertEquals(emptyList(), core.queries.missingStarterTechniques())
        core.items.deleteItem(ItemKind.Transition, "triangle-choke")
        core.items.deleteItem(ItemKind.Transition, "kimura-guard")
        storage.transitions.put(assertNotNull(storage.transitions.get("scissor-sweep")).copy(notes = "mine"))
        assertEquals(listOf("triangle-choke", "kimura-guard"), core.queries.missingStarterTechniques().map { it.id })
        clock.advance(5000)
        assertEquals(2, core.items.addMissingStarterTechniques())
        assertEquals(before, storage.transitions.count())
        assertEquals(
            true to clock.now,
            storage.transitions.get("triangle-choke")?.let {
                it.blueBeltTest to it.createdAt
            },
        )
        assertEquals("mine", storage.transitions.get("scissor-sweep")?.notes)
        // Deleting a technique keeps its bookmark, unlinked, so only that one is not re-added.
        assertEquals("", storage.videos.get("video-triangle-choke")?.linkedId)
        assertEquals(0, core.items.addMissingStarterTechniques())
    }

    @Test
    fun `brings the starter video for a technique the library never had`() = runTest {
        // A library set up before the armbar from mount was added to the starter content.
        storage.transitions.remove("armbar-mount")
        storage.videos.remove("video-armbar-mount")
        assertEquals(1, core.items.addMissingStarterTechniques())
        val video = storage.videos.get("video-armbar-mount")
        assertEquals(LinkedKind.Transition to "armbar-mount", video?.let { it.linkedKind to it.linkedId })
    }

    @Test
    fun `skips starter techniques whose position was deleted`() = runTest {
        core.items.deleteItem(ItemKind.Position, "mount")
        assertEquals(0, core.items.addMissingStarterTechniques())
        assertFalse(storage.transitions.all().any { it.fromId == "mount" || it.toId == "mount" })
    }

    @Test
    fun `dismissing stops offering the current techniques but not ones that go missing later`() = runTest {
        core.items.deleteItem(ItemKind.Transition, "triangle-choke")
        clock.now = 7000
        assertEquals(1, core.items.dismissMissingStarterTechniques())
        assertEquals(StarterDismissal("triangle-choke", 7000), storage.starterDismissals.get("triangle-choke"))
        assertEquals(emptyList(), core.queries.missingStarterTechniques())
        assertEquals(0, core.items.addMissingStarterTechniques())
        assertNull(storage.transitions.get("triangle-choke"))
        core.items.deleteItem(ItemKind.Transition, "kimura-guard")
        assertEquals(listOf("kimura-guard"), core.queries.missingStarterTechniques().map { it.id })
    }

    @Test
    fun `adds back missing starter concepts and relinks them to starter positions and only once`() = runTest {
        assertEquals(emptyList(), core.queries.missingStarterConcepts())
        val before = storage.concepts.count()
        core.items.deleteItem(ItemKind.Concept, "angles")
        assertFalse("angles" in assertNotNull(storage.positions.get("closed-guard")).conceptIds)
        assertEquals(listOf("angles"), core.queries.missingStarterConcepts().map { it.id })
        clock.advance(5000)
        assertEquals(1, core.items.addMissingStarterConcepts())
        assertEquals(before, storage.concepts.count())
        assertEquals(clock.now, storage.concepts.get("angles")?.createdAt)
        val guard = assertNotNull(storage.positions.get("closed-guard"))
        assertEquals(clock.now, guard.updatedAt)
        assertTrue("angles" in guard.conceptIds)
        // Deleting a concept keeps its bookmarks, unlinked, so they are not re-added.
        assertEquals(emptyList(), storage.videos.all().filter { it.linkedId == "angles" })
        assertEquals(0, core.items.addMissingStarterConcepts())
    }

    @Test
    fun `brings the starter videos for a concept the library never had and keeps position edits`() = runTest {
        // A library set up before "Breathe and stay calm" was added to the starter content.
        val itsVideos = storage.videos.all().filter { it.linkedId == "breathing" }.map { it.id }
        storage.concepts.remove("breathing")
        itsVideos.forEach { storage.videos.remove(it) }
        storage.positions.put(assertNotNull(storage.positions.get("mount")).copy(conceptIds = listOf("base")))
        assertEquals(1, core.items.addMissingStarterConcepts())
        assertEquals(
            itsVideos.sorted(),
            storage.videos.all().filter {
                it.linkedId == "breathing"
            }.map { it.id }.sorted(),
        )
        assertEquals(listOf("base", "breathing"), storage.positions.get("mount")?.conceptIds)
    }

    @Test
    fun `dismissing stops offering the current concepts but not ones that go missing later`() = runTest {
        core.items.deleteItem(ItemKind.Concept, "angles")
        clock.now = 7000
        assertEquals(1, core.items.dismissMissingStarterConcepts())
        assertEquals(StarterDismissal("angles", 7000), storage.starterDismissals.get("angles"))
        assertEquals(emptyList(), core.queries.missingStarterConcepts())
        assertEquals(0, core.items.addMissingStarterConcepts())
        core.items.deleteItem(ItemKind.Concept, "breathing")
        assertEquals(listOf("breathing"), core.queries.missingStarterConcepts().map { it.id })
    }
}
