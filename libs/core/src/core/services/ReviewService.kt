package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ReviewLogEntry
import dev.justinsmith.bjj.core.ReviewState
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.cardId
import dev.justinsmith.bjj.core.newReviewState
import dev.justinsmith.bjj.core.schedule

class ReviewService(env: CoreEnv) {
    private val storage = env.storage
    private val now = env.now

    /** Applies a grade to a card: reschedules it and appends to the history, as one change. */
    suspend fun recordReview(item: StudyItem, grade: Grade): ReviewState {
        val timestamp = now()
        return storage.transaction {
            val previous = storage.reviews.get(item.cardId) ?: newReviewState(item.kind, item.id, timestamp)
            val next = schedule(previous, grade, timestamp)
            storage.reviews.put(next)
            storage.reviewLog.add(ReviewLogEntry(cardId = item.cardId, grade = grade, reviewedAt = timestamp))
            next
        }
    }

    /** Forgets all study progress. Items and videos are untouched. */
    suspend fun resetProgress() {
        storage.transaction {
            storage.reviews.clear()
            storage.reviewLog.clear()
        }
    }
}
