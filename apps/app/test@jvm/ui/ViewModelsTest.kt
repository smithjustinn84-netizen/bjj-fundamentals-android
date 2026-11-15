package dev.justinsmith.bjj.ui

import dev.justinsmith.bjj.core.BackupProblem
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import dev.justinsmith.bjj.di.appModules
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TodayTab
import dev.justinsmith.bjj.nav.currentTab
import dev.justinsmith.bjj.nav.sameDestination
import dev.justinsmith.bjj.nav.studyItemKey
import dev.justinsmith.bjj.platform.Links
import dev.justinsmith.bjj.ui.backup.BackupViewModel
import dev.justinsmith.bjj.ui.home.formatDay
import dev.justinsmith.bjj.ui.home.todayLabel
import dev.justinsmith.bjj.ui.log.LogViewModel
import dev.justinsmith.bjj.ui.review.ReviewSessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The ViewModels over a real core on in-memory storage, with writes finishing before each call returns. */
@OptIn(ExperimentalCoroutinesApi::class, KoinExperimentalAPI::class)
class ViewModelsTest {
    private val t: TestCore = testCore()
    private val env = CoreEnv(t.storage, newId = { "x" }, now = { t.clock.now }, zone = LocalZone.Utc)
    private val a = StudyItem(CardKind.Concept, "safety")
    private val b = StudyItem(CardKind.Concept, "posture")

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun `a session shows each card and sends one graded again to the back of the queue`() = runTest {
        val session = ReviewSessionViewModel(t.core)
        assertNull(session.state.value.queue)
        session.startIfIdle(listOf(a, b))
        // Started already: a second start changes nothing.
        session.startIfIdle(listOf(b))
        session.reveal()
        assertTrue(session.state.value.revealed)
        session.grade(Grade.Again)
        assertEquals(listOf(b, a), session.state.value.queue)
        assertFalse(session.state.value.revealed)
        session.grade(Grade.Good)
        session.grade(Grade.Easy)
        val done = session.state.value
        assertTrue(done.finished)
        assertEquals(3, done.doneCount)
        assertEquals(mapOf(Grade.Again to 1, Grade.Good to 1, Grade.Easy to 1), done.tally)
        assertEquals(3, t.storage.reviewLog.count())
        assertEquals(2, t.storage.reviews.count())
    }

    @Test
    fun `a session drops a card whose item is gone and can start another round`() = runTest {
        val session = ReviewSessionViewModel(t.core)
        session.startIfIdle(listOf(a, b))
        session.drop(a)
        assertEquals(b, session.state.value.current)
        session.restart(listOf(a))
        assertEquals(listOf(a), session.state.value.queue)
        assertEquals(0, session.state.value.doneCount)
    }

    @Test
    fun `the log saves an entry once and says so when a save fails`() = runTest {
        val log = LogViewModel(t.core)
        val entry = TrainingEntryInput("2026-11-14", 60, "", "", "", "", emptyList(), emptyList())
        var saved = 0
        log.save(null, entry) { saved++ }
        assertEquals(1, saved)
        assertEquals(1, t.storage.trainingLog.count())
        log.save(null, entry.copy(day = "yesterday")) { saved++ }
        assertEquals(1, saved)
        assertTrue(log.state.value.error.isNotEmpty())
        assertFalse(log.state.value.saving)
    }

    @Test
    fun `a backup is checked on picking and replaces nothing until confirmed`() = runTest {
        val backup = BackupViewModel(t.core, env)
        var text = ""
        backup.export { name, exported ->
            assertEquals("bjj-fundamentals-backup-2026-11-14.json", name)
            text = exported
        }
        backup.picked("{ not a backup")
        assertEquals("${BackupProblem.NotJson.message} Nothing was changed.", backup.state.value.message)
        assertTrue(backup.state.value.isError)

        t.storage.concepts.clear()
        backup.picked(text)
        assertEquals(encodeBackup(checkNotNull(backup.state.value.pending)), text)
        assertEquals(0, t.storage.concepts.count())
        backup.cancelImport()
        assertNull(backup.state.value.pending)
        backup.picked(text)
        backup.confirmImport()
        assertTrue(t.storage.concepts.count() > 0)
        assertFalse(backup.state.value.isError)
    }

    @Test
    fun `the container can build everything it declares`() {
        // What the platform supplies is taken as given; everything the app declares must be buildable from it.
        val platform = module { }
        appModules(platform).drop(1).forEach {
            it.verify(extraTypes = listOf(CoreEnv::class, Links::class, CoroutineScope::class))
        }
    }

    @Test
    fun `the bar keeps a tab lit on any page of it and under pages opened from it`() {
        val stack = listOf(TodayTab, LearnTab(LearnSection.Lessons), ConceptDetail("base"))
        assertTrue(stack.currentTab().sameDestination(LearnTab()))
        assertFalse(stack.currentTab().sameDestination(ReviewTab()))
        assertEquals(TodayTab, listOf(TodayTab).currentTab())
        assertEquals(PositionDetail("mount"), studyItemKey(StudyItem(CardKind.Situation, "mount:bottom")))
    }

    @Test
    fun `dates are written in words`() {
        // Noon on Saturday 14 November 2026.
        assertEquals("Saturday 14 November", todayLabel(t.clock.now, LocalZone.Utc))
        assertEquals("Sat 14 Nov 2026", formatDay("2026-11-14"))
        assertEquals("Thu 1 Jan 1970", formatDay("1970-01-01"))
        assertEquals("not a day", formatDay("not a day"))
    }
}
