package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.DAY_MS
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.RELEARN_MS
import dev.justinsmith.bjj.core.ReviewLogEntry
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.positionOptions
import dev.justinsmith.bjj.core.testing.TEST_NOW
import dev.justinsmith.bjj.core.testing.testCore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reviews, the training log, videos, settings and the queries over them, through the services. */
@Suppress("detekt.TooManyFunctions") // One behaviour per test, side by side.
class StudyServicesTest {
    private val t = testCore()
    private val core = t.core
    private val storage = t.storage
    private val clock = t.clock
    private val card = StudyItem(CardKind.Concept, "base")
    private val underMount = StudyItem(CardKind.Situation, "mount:bottom")
    private val entry = TrainingEntryInput("2026-11-14", 90, "Scissor sweep", "", "", "", emptyList(), emptyList())

    private fun video(kind: LinkedKind = LinkedKind.None, id: String = "", title: String = "Clip") =
        VideoInput(title, "dQw4w9WgXcQ", 0, kind, id, "")

    @Test
    fun `the first review creates the card and appends to the history`() = runTest {
        val next = core.reviews.recordReview(card, Grade.Good)
        assertEquals(Triple("concept:base", 1, TEST_NOW + DAY_MS), Triple(next.id, next.intervalDays, next.due))
        assertEquals(next, storage.reviews.get("concept:base"))
        assertEquals(listOf(ReviewLogEntry(1, "concept:base", Grade.Good, TEST_NOW)), storage.reviewLog.all())
    }

    @Test
    fun `later reviews build on the stored state`() = runTest {
        core.reviews.recordReview(card, Grade.Good)
        clock.advance(DAY_MS)
        assertEquals(2 to 3, core.reviews.recordReview(card, Grade.Good).let { it.repetitions to it.intervalDays })
    }

    @Test
    fun `reviews drive the due count and the streak`() = runTest {
        clock.now = TEST_NOW - DAY_MS
        core.reviews.recordReview(card, Grade.Again)
        assertEquals(0, core.queries.dueCount())
        clock.advance(RELEARN_MS)
        assertEquals(1, core.queries.dueCount())
        clock.now = TEST_NOW
        core.reviews.recordReview(StudyItem(CardKind.Concept, "posture"), Grade.Good)
        assertEquals(StudyStats(dueCount = 1, streakDays = 2), core.queries.studyStats())
    }

    @Test
    fun `resetting progress clears cards and history but keeps the items`() = runTest {
        core.reviews.recordReview(card, Grade.Good)
        core.reviews.resetProgress()
        assertEquals(0, storage.reviews.count())
        assertEquals(0, storage.reviewLog.count())
        assertNotNull(storage.concepts.get("base"))
    }

    @Test
    fun `a situation card is reviewed like any other`() = runTest {
        core.reviews.recordReview(underMount, Grade.Again)
        assertEquals(
            CardKind.Situation to 1,
            storage.reviews.get("situation:mount:bottom")?.let {
                it.kind to it.lapses
            },
        )
    }

    @Test
    fun `a situation card is not due once its side has no techniques and goes with its position`() = runTest {
        core.reviews.recordReview(underMount, Grade.Good)
        clock.advance(2 * DAY_MS)
        assertEquals(1, core.queries.dueCount())
        val waysOut = positionOptions(core.queries.library(), "mount").bottom
        assertTrue(waysOut.isNotEmpty())
        for (technique in waysOut) core.items.deleteItem(ItemKind.Transition, technique.id)
        assertEquals(0, core.queries.dueCount())
        core.items.deleteItem(ItemKind.Position, "mount")
        assertNull(storage.reviews.get("situation:mount:bottom"))
    }

    @Test
    fun `the library loads every item with its review state`() = runTest {
        core.reviews.recordReview(card, Grade.Good)
        val lib = core.queries.library()
        assertTrue(lib.concepts.size > 5)
        assertEquals("Mount", lib.positionById["mount"]?.name)
        assertEquals(1, lib.reviews["concept:base"]?.repetitions)
    }

    @Test
    fun `videos are newest first overall and oldest first for one item`() = runTest {
        storage.videos.clear()
        val first = core.videos.add(video(LinkedKind.Position, "mount"))
        clock.advance(1000)
        val unlinked = core.videos.add(video())
        clock.advance(1000)
        val last = core.videos.add(video(LinkedKind.Position, "mount"))
        assertEquals(listOf(last, unlinked, first), core.queries.videos().map { it.id })
        assertEquals(listOf(first, last), core.queries.linkedVideos(ItemKind.Position, "mount").map { it.id })
        assertEquals(emptyList(), core.queries.linkedVideos(ItemKind.Concept, "mount"))
    }

    @Test
    fun `a bookmark keeps a trimmed title or gets a placeholder`() = runTest {
        assertEquals("Hip escape", storage.videos.get(core.videos.add(video(title = "  Hip escape  ")))?.title)
        assertEquals(UNTITLED_VIDEO, storage.videos.get(core.videos.add(video(title = "   ")))?.title)
    }

    @Test
    fun `a change made through a service is reported`() = runTest {
        val reported = async(start = CoroutineStart.UNDISPATCHED) { core.changes.first() }
        core.videos.add(video())
        reported.await()
    }

    @Test
    fun `the theme follows the device until one is chosen then remembers the latest choice`() = runTest {
        assertEquals(ThemeChoice.Device, core.queries.themeChoice())
        core.settings.setTheme(ThemeChoice.Dark)
        core.settings.setTheme(ThemeChoice.Light)
        assertEquals(ThemeChoice.Light, core.queries.themeChoice())
        assertEquals(1, storage.settings.count())
    }

    @Test
    fun `a class write-up is tidied and keeps only techniques and situations that exist once each`() = runTest {
        val id = core.trainingLog.save(
            null,
            entry.copy(
                focus = "  Get to my side  ",
                techniqueIds = listOf("scissor-sweep", "not-a-technique", "scissor-sweep"),
                stuckIds = listOf("mount:bottom", "no-such-position:bottom", "mount", "mount:bottom"),
            ),
        )
        val saved = assertNotNull(storage.trainingLog.get(id))
        assertEquals("Get to my side", saved.focus)
        assertEquals(listOf("scissor-sweep"), saved.techniqueIds)
        assertEquals(listOf("mount:bottom"), saved.stuckIds)
        assertEquals(0, storage.trainingLog.get(core.trainingLog.save(null, entry.copy(minutes = -5)))?.minutes)
    }

    @Test
    fun `an edited class keeps its creation time`() = runTest {
        val id = core.trainingLog.save(null, entry)
        val created = clock.now
        clock.advance(1000)
        core.trainingLog.save(id, entry.copy(taught = "Hip bump sweep"))
        val saved = assertNotNull(storage.trainingLog.get(id))
        assertEquals(
            Triple("Hip bump sweep", created, created + 1000),
            Triple(saved.taught, saved.createdAt, saved.updatedAt),
        )
        assertEquals(1, storage.trainingLog.count())
    }

    @Test
    fun `a day that is not a calendar day is refused`() = runTest {
        val error =
            assertFailsWith<IllegalArgumentException> { core.trainingLog.save(null, entry.copy(day = "yesterday")) }
        assertTrue("not a day" in error.message.orEmpty())
        assertEquals(0, storage.trainingLog.count())
    }

    @Test
    fun `classes are listed most recent first and one can be removed`() = runTest {
        val older = core.trainingLog.save(null, entry.copy(day = "2026-11-01"))
        val newer = core.trainingLog.save(null, entry)
        assertEquals(listOf(newer, older), core.queries.trainingLog().map { it.id })
        core.trainingLog.remove(older)
        assertEquals(listOf(newer), core.queries.trainingLog().map { it.id })
    }

    @Test
    fun `deleting what a class mentions keeps the entry and drops the mention`() = runTest {
        val id = core.trainingLog.save(
            null,
            entry.copy(
                techniqueIds = listOf("scissor-sweep", "upa-escape", "elbow-knee-escape", "rear-naked-choke"),
                stuckIds = listOf("mount:bottom", "half-guard:bottom"),
            ),
        )
        core.items.deleteItem(ItemKind.Transition, "scissor-sweep")
        val kept = listOf("upa-escape", "elbow-knee-escape", "rear-naked-choke")
        assertEquals(kept, storage.trainingLog.get(id)?.techniqueIds)
        core.items.deleteItem(ItemKind.Position, "mount")
        assertEquals(listOf("rear-naked-choke"), storage.trainingLog.get(id)?.techniqueIds)
        assertEquals(listOf("half-guard:bottom"), storage.trainingLog.get(id)?.stuckIds)
    }

    @Test
    fun `the streak counts a day with a logged class even when nothing was reviewed`() = runTest {
        assertEquals(0, core.queries.studyStats().streakDays)
        core.trainingLog.save(null, entry)
        assertEquals(1, core.queries.studyStats().streakDays)
    }

    @Test
    fun `a snapshot agrees with the single queries`() = runTest {
        core.reviews.recordReview(card, Grade.Again)
        core.trainingLog.save(null, entry)
        core.items.deleteItem(ItemKind.Concept, "angles")
        core.settings.setTheme(ThemeChoice.Dark)
        clock.advance(RELEARN_MS)
        val snapshot = core.queries.snapshot()
        assertEquals(core.queries.library(), snapshot.library)
        assertEquals(core.queries.videos(), snapshot.videos)
        assertEquals(core.queries.trainingLog(), snapshot.trainingLog)
        assertEquals(ThemeChoice.Dark, snapshot.themeChoice)
        assertEquals(listOf("angles"), snapshot.missingStarterConcepts.map { it.id })
        assertEquals(core.queries.missingStarterTechniques(), snapshot.missingStarterTechniques)
        assertEquals(core.backups.tableCounts(), snapshot.tableCounts)
        assertEquals(core.queries.studyStats(), studyStats(snapshot, clock.now, LocalZone.Utc))
        assertEquals(StudyStats(dueCount = 1, streakDays = 1), studyStats(snapshot, clock.now, LocalZone.Utc))
    }
}
