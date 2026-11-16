package dev.justinsmith.bjj.ui

import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.BackupProblem
import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.DAY_MS
import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.PathStepKind
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.core.services.ConceptInput
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.ui.backup.BackupMessage
import dev.justinsmith.bjj.ui.backup.BackupViewModel
import dev.justinsmith.bjj.ui.common.OtherEnd
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.editor.EditorFields
import dev.justinsmith.bjj.ui.editor.EditorOutcome
import dev.justinsmith.bjj.ui.editor.EditorUiState
import dev.justinsmith.bjj.ui.editor.EditorViewModel
import dev.justinsmith.bjj.ui.home.HomeUiState
import dev.justinsmith.bjj.ui.home.HomeViewModel
import dev.justinsmith.bjj.ui.home.ReviewOffer
import dev.justinsmith.bjj.ui.learn.ConceptDetailUiState
import dev.justinsmith.bjj.ui.learn.ConceptDetailViewModel
import dev.justinsmith.bjj.ui.learn.ConceptsUiState
import dev.justinsmith.bjj.ui.learn.ConceptsViewModel
import dev.justinsmith.bjj.ui.learn.PositionDetailUiState
import dev.justinsmith.bjj.ui.learn.PositionDetailViewModel
import dev.justinsmith.bjj.ui.learn.TransitionDetailUiState
import dev.justinsmith.bjj.ui.learn.TransitionDetailViewModel
import dev.justinsmith.bjj.ui.log.LogError
import dev.justinsmith.bjj.ui.log.LogUiState
import dev.justinsmith.bjj.ui.log.LogViewModel
import dev.justinsmith.bjj.ui.review.LessonChip
import dev.justinsmith.bjj.ui.review.ReviewChoice
import dev.justinsmith.bjj.ui.review.ReviewSessionViewModel
import dev.justinsmith.bjj.ui.review.ReviewUiState
import dev.justinsmith.bjj.ui.review.ReviewViewModel
import dev.justinsmith.bjj.ui.review.SessionUiState
import dev.justinsmith.bjj.ui.shell.ShellUiState
import dev.justinsmith.bjj.ui.shell.ShellViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The ViewModels over a real core on in-memory storage holding the starter content, with reads and writes
 * finishing before each call returns.
 */
@OptIn(ExperimentalCoroutinesApi::class, KoinExperimentalAPI::class)
class ViewModelsTest {
    private val t: TestCore = testCore()
    private val clock = Clock(CoreEnv(t.storage, newId = { "x" }, now = { t.clock.now }, zone = LocalZone.Utc))
    private val appData = AppData(t.core, CoroutineScope(UnconfinedTestDispatcher()))
    private val videos = VideoCommands(t.core.videos) { }

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    /** Keeps a screen's state worked out, as a page showing it would, and reads it. */
    private fun <T> TestScope.watch(state: StateFlow<T>): () -> T {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect { } }
        return { state.value }
    }

    private fun TestScope.session(newCards: Int = 2): Pair<ReviewSessionViewModel, () -> SessionUiState> {
        val viewModel =
            ReviewSessionViewModel(ReviewSession(Deck.Concept.key, null, newCards), appData, t.core.reviews, clock)
        return viewModel to watch(viewModel.uiState)
    }

    @Test
    fun `a session shows each card and sends one graded again to the back of the queue`() = runTest {
        val (session, state) = session()
        val first = assertIs<SessionUiState.Active>(state())
        assertEquals(2, first.total)
        assertFalse(first.revealed)
        assertEquals(Grade.entries.toSet(), first.intervals.keys)
        session.reveal()
        assertTrue(assertIs<SessionUiState.Active>(state()).revealed)

        session.grade(Grade.Again)
        val second = assertIs<SessionUiState.Active>(state())
        assertNotEquals(first.card.item, second.card.item)
        assertFalse(second.revealed)
        session.grade(Grade.Good)
        // The card graded "again" comes back once the rest are answered.
        assertEquals(first.card.item, assertIs<SessionUiState.Active>(state()).card.item)
        session.grade(Grade.Easy)

        val done = assertIs<SessionUiState.Complete>(state())
        assertEquals(3, done.doneCount)
        assertEquals(mapOf(Grade.Again to 1, Grade.Good to 1, Grade.Easy to 1), done.tally)
        assertEquals(1, done.streakDays)
        assertTrue(done.moreWaiting > 0)
        assertEquals(3, t.storage.reviewLog.count())
        assertEquals(2, t.storage.reviews.count())
    }

    @Test
    fun `a session drops a card whose item is gone and can start another round`() = runTest {
        val (session, state) = session()
        val first = assertIs<SessionUiState.Active>(state()).card.item
        session.reveal()
        t.core.items.deleteItem(ItemKind.Concept, first.id)
        val next = assertIs<SessionUiState.Active>(state())
        assertNotEquals(first, next.card.item)
        assertEquals(1, next.total)
        // The card that took its place starts on its question.
        assertFalse(next.revealed)

        session.grade(Grade.Good)
        assertIs<SessionUiState.Complete>(state())
        session.restart()
        val again = assertIs<SessionUiState.Active>(state())
        assertEquals(0, again.doneCount)
        assertEquals(2, again.total)
    }

    @Test
    fun `the log saves an entry once and says so when a save fails`() = runTest {
        val log = LogViewModel(appData, t.core.trainingLog, clock)
        val state = watch(log.uiState)
        val entry = TrainingEntryInput("2026-11-14", 60, "", "", "", "", emptyList(), listOf("mount:bottom"))
        assertEquals("2026-11-14", assertIs<LogUiState.Ready>(state()).today)
        log.save(null, entry)
        val saved = assertIs<LogUiState.Ready>(state())
        assertEquals(1, saved.writes.saved)
        assertEquals(1, saved.entries.size)
        assertEquals(listOf("mount:bottom"), saved.entries.single().stuck.map { it.id })
        assertEquals(listOf("mount:bottom"), saved.choices.recentlyStuck)
        assertEquals(1, t.storage.trainingLog.count())

        log.save(null, entry.copy(day = "yesterday"))
        val failed = assertIs<LogUiState.Ready>(state())
        assertEquals(1, failed.writes.saved)
        assertEquals(LogError.Save, failed.writes.error)
        assertFalse(failed.writes.saving)
    }

    @Test
    fun `a backup is checked on picking and replaces nothing until confirmed`() = runTest {
        val backup = BackupViewModel(appData, t.core.backups, t.core.reviews, clock)
        val state = watch(backup.uiState)
        assertTrue(checkNotNull(state().counts[BackupTable.Concepts]) > 0)
        var text = ""
        backup.export { name, exported ->
            assertEquals("bjj-fundamentals-backup-2026-11-14.json", name)
            text = exported
        }
        backup.picked("{ not a backup")
        assertEquals(BackupMessage.Rejected(BackupProblem.NotJson), state().message)
        assertTrue(state().isError)

        t.storage.concepts.clear()
        backup.picked(text)
        assertEquals(encodeBackup(checkNotNull(state().pending)), text)
        assertEquals("Sat 14 Nov 2026", state().pendingDay)
        assertEquals(0, t.storage.concepts.count())
        backup.cancelImport()
        assertNull(state().pending)
        assertNull(state().pendingDay)
        backup.picked(text)
        backup.confirmImport()
        assertTrue(t.storage.concepts.count() > 0)
        assertEquals(BackupMessage.Restored, state().message)
        assertFalse(state().isError)
    }

    @Test
    fun `a list page finds what is searched for and adds the starter items it is missing`() = runTest {
        val concepts = ConceptsViewModel(appData, t.core.items)
        val state = watch(concepts.uiState)
        val all = assertIs<ConceptsUiState.Ready>(state())
        assertTrue(all.missingStarter.isEmpty())
        assertTrue(all.search.hits.isEmpty())
        concepts.search("frames")
        assertTrue(assertIs<ConceptsUiState.Ready>(state()).search.hits.any { it.id == "frames" })

        t.core.items.deleteItem(ItemKind.Concept, "frames")
        val without = assertIs<ConceptsUiState.Ready>(state())
        assertEquals(all.concepts.size - 1, without.concepts.size)
        assertEquals(1, without.missingStarter.size)
        concepts.addStarter()
        assertEquals(all.concepts.map { it.id }, assertIs<ConceptsUiState.Ready>(state()).concepts.map { it.id })
    }

    @Test
    fun `an item's page follows what is stored and says when the item is gone`() = runTest {
        val state = watch(ConceptDetailViewModel(ConceptDetail("posture"), appData, videos).uiState)
        val ready = assertIs<ConceptDetailUiState.Ready>(state())
        assertEquals("posture", ready.concept.id)
        assertEquals(Progress.New, ready.progress)
        assertEquals("safety", ready.previous?.id)
        assertEquals("base", ready.next?.id)

        t.core.reviews.recordReview(StudyItem(CardKind.Concept, "posture"), Grade.Good)
        assertEquals(Progress.Learning, assertIs<ConceptDetailUiState.Ready>(state()).progress)
        t.core.items.deleteItem(ItemKind.Concept, "posture")
        assertEquals(ConceptDetailUiState.Missing, state())
        assertEquals(
            PositionDetailUiState.Missing,
            watch(PositionDetailViewModel(PositionDetail("nowhere"), appData, videos).uiState)(),
        )
        assertEquals(
            TransitionDetailUiState.Missing,
            watch(TransitionDetailViewModel(TransitionDetail("nothing"), appData, videos).uiState)(),
        )
    }

    @Test
    fun `a position's page lists what each side can do and opens on the side asked for`() = runTest {
        val key = PositionDetail("mount", PositionRole.Bottom)
        val ready =
            assertIs<PositionDetailUiState.Ready>(watch(PositionDetailViewModel(key, appData, videos).uiState)())
        assertEquals(PositionRole.Bottom, ready.openOn)
        assertTrue(ready.hasTechniques)
        assertTrue(ready.options.getValue(PositionRole.Bottom).all { it.type == TransitionType.Escape })
        assertTrue(ready.options.getValue(PositionRole.Top).isNotEmpty())
        assertTrue(ready.waysIn.all { it.otherEnd is OtherEnd.From })
        assertEquals(ready.options.values.sumOf { it.size }, ready.total)
    }

    @Test
    fun `review opens on the deck or lesson asked for and picking a deck leaves the lesson`() = runTest {
        val lesson = FOUNDATION_LESSONS.first()
        val review = ReviewViewModel(ReviewTab(Deck.Concept.key, lesson.id), appData, clock)
        val state = watch(review.uiState)
        val opened = assertIs<ReviewUiState.Ready>(state())
        assertEquals(LessonChip(1, lesson.title), opened.lesson)
        assertEquals(DEFAULT_NEW_CARDS, opened.newCount)
        assertEquals(opened.due + opened.newCount, opened.ready)

        review.chooseDeck(Deck.Position)
        review.setNewLimit(0)
        val picked = assertIs<ReviewUiState.Ready>(state())
        assertNull(picked.lesson)
        assertEquals(ReviewChoice(Deck.Position, null, 0), picked.choice)
        assertEquals(0, picked.ready)
        assertTrue(picked.moreNew > 0)
    }

    @Test
    fun `the editor leaves once an item is saved or deleted and never calls a deleted item missing`() = runTest {
        val missing = EditorViewModel(Editor(ItemKind.Concept, "nothing"), appData, t.core.items)
        assertEquals(EditorUiState.Missing, watch(missing.uiState)())

        val adding = EditorViewModel(Editor(ItemKind.Concept), appData, t.core.items)
        val added = watch(adding.uiState)
        val fields = assertIs<EditorFields.ConceptFields>(assertIs<EditorUiState.Ready>(added()).fields)
        assertNull(fields.stored)
        adding.saveConcept(ConceptInput("Grips", "", emptyList(), "", fields.nextOrder, ""))
        val saved = assertIs<EditorOutcome.Saved>(assertIs<EditorUiState.Ready>(added()).outcome)
        assertEquals("Grips", t.storage.concepts.get(saved.id)?.name)

        val editing = EditorViewModel(Editor(ItemKind.Position, "mount"), appData, t.core.items)
        val edited = watch(editing.uiState)
        val before = assertIs<EditorUiState.Ready>(edited())
        assertEquals("Mount", before.name)
        assertTrue(before.cascade > 0)
        editing.delete()
        assertEquals(EditorOutcome.Deleted, assertIs<EditorUiState.Ready>(edited()).outcome)
        assertNull(t.storage.positions.get("mount"))
    }

    @Test
    fun `today and the frame follow the clock and what is stored`() = runTest {
        val shell = watch(ShellViewModel(appData, clock).uiState)
        val home = HomeViewModel(appData, clock)
        val state = watch(home.uiState)
        val fresh = assertIs<HomeUiState.Ready>(state())
        assertEquals("Saturday 14 November", fresh.dateLabel)
        assertEquals(ReviewOffer(DEFAULT_NEW_CARDS, 0, DEFAULT_NEW_CARDS, 0, 0), fresh.review)
        assertEquals(PathStepKind.entries, fresh.path.map { it.kind })
        assertEquals(1, fresh.lesson?.number)
        assertEquals(0, shell().dueCount)

        val card = StudyItem(CardKind.Concept, "safety")
        t.core.reviews.recordReview(card, Grade.Again)
        t.core.settings.setTheme(ThemeChoice.Dark)
        t.clock.advance(DAY_MS)
        // Nothing stored changes as time passes: a write is what has the screens read the clock again.
        t.core.trainingLog.save(null, TrainingEntryInput("2026-11-15", 60, "", "", "", "", emptyList(), emptyList()))
        assertEquals(ShellUiState(ThemeChoice.Dark, dueCount = 1), shell())
        val later = assertIs<HomeUiState.Ready>(state())
        assertEquals(1, later.review.due)
        assertEquals(1, later.review.started)
        assertEquals(2, later.streakDays)
        assertEquals(1, later.training.classes)
        home.search("mount")
        assertTrue(assertIs<HomeUiState.Ready>(state()).search.hits.isNotEmpty())
    }
}
