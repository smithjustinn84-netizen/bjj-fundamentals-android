package dev.justinsmith.bjj.ui.home

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.DAY_MS
import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.PathStepKind
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import dev.justinsmith.bjj.ui.ViewModelTest
import dev.justinsmith.bjj.ui.shell.ShellUiState
import dev.justinsmith.bjj.ui.shell.ShellViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Today's ViewModel, and the frame's beside it, over the starter content. */
class HomeViewModelTest : ViewModelTest() {
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
        val searched = assertIs<HomeUiState.Ready>(state())
        assertTrue(searched.search.hits.isNotEmpty())
        // Typing works out the search again, not the rest of the page.
        assertSame(later.path, searched.path)
        assertSame(later.review, searched.review)
    }
}
