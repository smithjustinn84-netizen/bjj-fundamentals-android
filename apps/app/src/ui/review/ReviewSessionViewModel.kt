package dev.justinsmith.bjj.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.cardName
import dev.justinsmith.bjj.core.describeInterval
import dev.justinsmith.bjj.core.newReviewState
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.ReviewService
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.studyStats
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One review session as it stands: the cards still to answer, which side is showing, and how it has gone. */
private data class Session(
    /** Null until the session has been given its cards. */
    val queue: List<StudyItem>? = null,
    val revealed: Boolean = false,
    val doneCount: Int = 0,
    /** How many answers got each grade. */
    val tally: Map<Grade, Int> = emptyMap(),
    /** True while a grade is being stored: the buttons wait. */
    val saving: Boolean = false,
)

sealed interface SessionUiState {
    /** Before the session has its cards, and for a moment while a card whose item is gone is dropped. */
    data object Loading : SessionUiState

    data class Active(
        val card: CardFace,
        val revealed: Boolean,
        val doneCount: Int,
        /** Cards answered and still to answer, together. */
        val total: Int,
        /** True while a grade is being stored: the buttons wait. */
        val saving: Boolean,
        /** When the card would come back for each grade, in words. */
        val intervals: Map<Grade, String>,
    ) : SessionUiState

    data class Complete(
        val doneCount: Int,
        /** How many answers got each grade. */
        val tally: Map<Grade, Int>,
        val streakDays: Int,
        /** How many cards another round with the same deck and setting would hold. */
        val moreWaiting: Int,
    ) : SessionUiState
}

/**
 * One review session: it takes its cards from the deck, lesson and limit in [key] once what is stored has been
 * read, shows them one at a time and stores each grade. It holds no interval maths, which is core's. The one
 * rule it keeps is that a card graded "again" goes back on the end of this session's queue.
 */
class ReviewSessionViewModel(
    private val key: ReviewSession,
    private val appData: AppData,
    private val reviews: ReviewService,
    private val clock: Clock,
) : ViewModel() {
    private val session = MutableStateFlow(Session())

    val uiState: StateFlow<SessionUiState> = screenState(appData, session, ::toUiState)

    init {
        viewModelScope.launch { appData.snapshot.filterNotNull().collect(::follow) }
    }

    private fun queueNow(snapshot: Snapshot): List<StudyItem> =
        queueFor(snapshot, deckOf(key.deck), key.lessonId, key.newCards, clock.now()).queue

    /** Starts the session on the first snapshot; after that, drops any card whose item has been deleted. */
    private fun follow(snapshot: Snapshot) {
        session.update { session ->
            val queue = session.queue ?: return@update session.copy(queue = queueNow(snapshot))
            val kept = queue.filter { cardName(snapshot.library, it) != null }
            // A new card on top starts on its question side.
            session.copy(queue = kept, revealed = session.revealed && kept.firstOrNull() == queue.firstOrNull())
        }
    }

    private fun toUiState(snapshot: Snapshot?, session: Session): SessionUiState {
        val queue = session.queue
        if (snapshot == null || queue == null) return SessionUiState.Loading
        val now = clock.now()
        val current = queue.firstOrNull()
            ?: return SessionUiState.Complete(
                doneCount = session.doneCount,
                tally = session.tally,
                streakDays = studyStats(snapshot, now, clock.zone).streakDays,
                moreWaiting = queueNow(snapshot).size,
            )
        val lib = snapshot.library
        val face = cardFace(lib, snapshot.videos, current) ?: return SessionUiState.Loading
        val card = reviewOf(lib, current.kind, current.id) ?: newReviewState(current.kind, current.id, now)
        return SessionUiState.Active(
            card = face,
            revealed = session.revealed,
            doneCount = session.doneCount,
            total = session.doneCount + queue.size,
            saving = session.saving,
            intervals = Grade.entries.associateWith { describeInterval(card, it, now) },
        )
    }

    /** Starts over with another round of the same deck. */
    fun restart() {
        val snapshot = appData.snapshot.value ?: return
        session.value = Session(queue = queueNow(snapshot))
    }

    fun reveal() {
        session.update { it.copy(revealed = true) }
    }

    fun grade(grade: Grade) {
        val now = session.value
        val current = now.queue?.firstOrNull() ?: return
        if (now.saving) return
        session.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                reviews.recordReview(current, grade)
                session.update {
                    val rest = it.queue.orEmpty() - current
                    it.copy(
                        // "Again" sends the card to the back of the queue; anything else finishes it for now.
                        queue = if (grade == Grade.Again) rest + current else rest,
                        revealed = false,
                        doneCount = it.doneCount + 1,
                        tally = it.tally + (grade to (it.tally[grade] ?: 0) + 1),
                    )
                }
            } finally {
                session.update { it.copy(saving = false) }
            }
        }
    }
}
