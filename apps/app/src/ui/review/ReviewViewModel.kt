package dev.justinsmith.bjj.ui.review

import androidx.lifecycle.ViewModel
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.DEFAULT_NEW_CARDS
import dev.justinsmith.bjj.core.Deck
import dev.justinsmith.bjj.core.FOUNDATION_LESSONS
import dev.justinsmith.bjj.core.RECENT_CLASS_COUNT
import dev.justinsmith.bjj.core.StudyQueue
import dev.justinsmith.bjj.core.buildLessonQueue
import dev.justinsmith.bjj.core.buildQueue
import dev.justinsmith.bjj.core.findLesson
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.studyFirst
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The deck a key names; anything unrecognised is everything. */
fun deckOf(key: String?): Deck = Deck.entries.firstOrNull { it.key == key } ?: Deck.All

/** The session a deck, or a lesson in its place, would hold now. */
fun queueFor(snapshot: Snapshot, deck: Deck, lessonId: String?, newLimit: Int, now: Long): StudyQueue {
    val lib = snapshot.library
    val lesson = findLesson(lessonId)
    return if (lesson != null) {
        buildLessonQueue(lib, lesson, newLimit, now)
    } else {
        buildQueue(lib, deck, newLimit, now, studyFirst(lib, snapshot.trainingLog, RECENT_CLASS_COUNT))
    }
}

/** What the student has picked to study: a deck, or one lesson in its place, and how many new cards to bring in. */
data class ReviewChoice(val deck: Deck, val lessonId: String?, val newLimit: Int)

/** The lesson being studied in place of a deck. */
data class LessonChip(val number: Int, val title: String)

sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    data class Ready(
        val choice: ReviewChoice,
        /** Null when a deck is being studied. */
        val lesson: LessonChip?,
        /** Cards a session would hold now: [due] of them due, [newCount] new. */
        val ready: Int,
        val due: Int,
        val newCount: Int,
        /** New cards the limit leaves out. */
        val moreNew: Int,
        /** How many of the new cards the training log put first. */
        val fromClass: Int,
    ) : ReviewUiState
}

fun reviewUiState(snapshot: Snapshot, choice: ReviewChoice, now: Long): ReviewUiState.Ready {
    val preview = queueFor(snapshot, choice.deck, choice.lessonId, choice.newLimit, now)
    val newCount = minOf(choice.newLimit, preview.fresh.size)
    return ReviewUiState.Ready(
        choice = choice,
        lesson = findLesson(choice.lessonId)?.let { LessonChip(FOUNDATION_LESSONS.indexOf(it) + 1, it.title) },
        ready = preview.queue.size,
        due = preview.due.size,
        newCount = newCount,
        moreNew = preview.fresh.size - newCount,
        fromClass = preview.fromClass,
    )
}

/**
 * Review before a session. A list page can open it on its own deck, and Lessons on one lesson ([key]); picking
 * a deck leaves the lesson.
 */
class ReviewViewModel(key: ReviewTab, appData: AppData, private val clock: Clock) : ViewModel() {
    private val choice = MutableStateFlow(ReviewChoice(deckOf(key.deck), key.lessonId, DEFAULT_NEW_CARDS))

    val uiState: StateFlow<ReviewUiState> = screenState(appData, choice) { snapshot, choice ->
        snapshot?.let { reviewUiState(it, choice, clock.now()) } ?: ReviewUiState.Loading
    }

    fun chooseDeck(deck: Deck) {
        choice.update { it.copy(deck = deck, lessonId = null) }
    }

    fun setNewLimit(limit: Int) {
        choice.update { it.copy(newLimit = limit) }
    }
}
