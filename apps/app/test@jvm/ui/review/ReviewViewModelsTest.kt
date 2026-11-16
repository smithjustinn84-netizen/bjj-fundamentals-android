package dev.justinsmith.bjj.ui.review

import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.ui.ViewModelTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Choosing what to review, and a session of it, over the starter content. */
class ReviewViewModelsTest : ViewModelTest() {
    private fun TestScope.session(newCards: Int = 2): Pair<ReviewSessionViewModel, () -> ReviewSessionUiState> {
        val viewModel =
            ReviewSessionViewModel(ReviewSession(Deck.Concept.key, null, newCards), appData, t.core.reviews, clock)
        return viewModel to watch(viewModel.uiState)
    }

    @Test
    fun `a session shows each card and sends one graded again to the back of the queue`() = runTest {
        val (session, state) = session()
        val first = assertIs<ReviewSessionUiState.Active>(state())
        assertEquals(2, first.total)
        assertFalse(first.revealed)
        assertEquals(Grade.entries.toSet(), first.intervals.keys)
        session.reveal()
        assertTrue(assertIs<ReviewSessionUiState.Active>(state()).revealed)

        session.grade(Grade.Again)
        val second = assertIs<ReviewSessionUiState.Active>(state())
        assertNotEquals(first.card.item, second.card.item)
        assertFalse(second.revealed)
        session.grade(Grade.Good)
        // The card graded "again" comes back once the rest are answered.
        assertEquals(first.card.item, assertIs<ReviewSessionUiState.Active>(state()).card.item)
        session.grade(Grade.Easy)

        val done = assertIs<ReviewSessionUiState.Complete>(state())
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
        val first = assertIs<ReviewSessionUiState.Active>(state()).card.item
        session.reveal()
        t.core.items.deleteItem(ItemKind.Concept, first.id)
        val next = assertIs<ReviewSessionUiState.Active>(state())
        assertNotEquals(first, next.card.item)
        assertEquals(1, next.total)
        // The card that took its place starts on its question.
        assertFalse(next.revealed)

        session.grade(Grade.Good)
        assertIs<ReviewSessionUiState.Complete>(state())
        session.restart()
        val again = assertIs<ReviewSessionUiState.Active>(state())
        assertEquals(0, again.doneCount)
        assertEquals(2, again.total)
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
}
