package dev.justinsmith.bjj.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.StudyItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One review session as it stands: the cards still to answer, which side is showing, and how it has gone. */
data class SessionUiState(
    /** Null until the session has been given its cards. */
    val queue: List<StudyItem>? = null,
    val revealed: Boolean = false,
    val doneCount: Int = 0,
    /** How many answers got each grade. */
    val tally: Map<Grade, Int> = emptyMap(),
    /** True while a grade is being stored: the buttons wait. */
    val saving: Boolean = false,
) {
    val current: StudyItem? get() = queue?.firstOrNull()
    val finished: Boolean get() = queue != null && queue.isEmpty()
}

/**
 * State for one review session: the queue of cards, which side is showing, and grading. It holds no interval
 * maths, which is core's. The one rule it keeps is that a card graded "again" goes back on the end of this
 * session's queue.
 */
class ReviewSessionViewModel(private val core: Core) : ViewModel() {
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    /** Gives the session its cards, once: a later call while it runs changes nothing. */
    fun startIfIdle(items: List<StudyItem>) {
        _state.update { if (it.queue == null) SessionUiState(queue = items) else it }
    }

    /** Starts over with another round of cards. */
    fun restart(items: List<StudyItem>) {
        _state.value = SessionUiState(queue = items)
    }

    fun reveal() {
        _state.update { it.copy(revealed = true) }
    }

    /** Takes a card out of the session unanswered: what it was about has been deleted. */
    fun drop(item: StudyItem) {
        _state.update { it.copy(queue = it.queue?.minus(item), revealed = false) }
    }

    fun grade(grade: Grade) {
        val session = _state.value
        val current = session.current ?: return
        if (session.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                core.reviews.recordReview(current, grade)
                _state.update {
                    val rest = it.queue.orEmpty().drop(1)
                    it.copy(
                        // "Again" sends the card to the back of the queue; anything else finishes it for now.
                        queue = if (grade == Grade.Again) rest + current else rest,
                        revealed = false,
                        doneCount = it.doneCount + 1,
                        tally = it.tally + (grade to (it.tally[grade] ?: 0) + 1),
                    )
                }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}
