package dev.justinsmith.bjj.core

// A simplified SM-2 scheduler. Pure functions so they are easy to test, and number for number the web app's.

/** "Again" brings a card back within the same session. */
const val RELEARN_MS: Long = 10 * MINUTE_MS

/** SM-2's starting ease: each successful review multiplies the interval by roughly this much. */
private const val INITIAL_EASE = 2.5

/** Ease never drops below this, so a card that was hard once can still grow its interval. */
private const val MIN_EASE = 1.3
private const val AGAIN_EASE_PENALTY = 0.2
private const val HARD_EASE_PENALTY = 0.15
private const val EASY_EASE_BONUS = 0.15

/** "Hard" grows the interval far more slowly than the ease would. */
private const val HARD_INTERVAL_FACTOR = 1.2

/** "Easy" stretches the interval beyond what the ease alone would give. */
private const val EASY_INTERVAL_FACTOR = 1.3

/** First intervals, in days, before the ease takes over. */
private const val FIRST_INTERVAL_DAYS = 1
private const val SECOND_INTERVAL_DAYS = 3
private const val FIRST_EASY_INTERVAL_DAYS = 4

/** An interval this long means the card has stuck across several days. */
private const val LEARNED_INTERVAL_DAYS = 3

private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_YEAR = 365

fun cardId(kind: CardKind, itemId: String): String = "${kind.key}:$itemId"

fun newReviewState(kind: CardKind, itemId: String, now: Long): ReviewState = ReviewState(
    id = cardId(kind, itemId),
    kind = kind,
    itemId = itemId,
    ease = INITIAL_EASE,
    intervalDays = 0,
    repetitions = 0,
    lapses = 0,
    due = now,
    lastReviewed = 0,
)

fun schedule(prev: ReviewState, grade: Grade, now: Long): ReviewState {
    if (grade == Grade.Again) {
        return prev.copy(
            ease = maxOf(MIN_EASE, prev.ease - AGAIN_EASE_PENALTY),
            intervalDays = 0,
            repetitions = 0,
            lapses = prev.lapses + 1,
            due = now + RELEARN_MS,
            lastReviewed = now,
        )
    }
    val first = prev.repetitions == 0
    var ease = prev.ease
    var intervalDays: Int
    when (grade) {
        Grade.Again -> error("handled above")

        Grade.Hard -> {
            ease = maxOf(MIN_EASE, ease - HARD_EASE_PENALTY)
            intervalDays = if (first) {
                FIRST_INTERVAL_DAYS
            } else {
                maxOf(FIRST_INTERVAL_DAYS, jsRoundToInt(prev.intervalDays * HARD_INTERVAL_FACTOR))
            }
        }

        Grade.Good -> intervalDays = when {
            first -> FIRST_INTERVAL_DAYS
            prev.repetitions == 1 -> SECOND_INTERVAL_DAYS
            else -> jsRoundToInt(prev.intervalDays * ease)
        }

        Grade.Easy -> {
            // The new interval uses the ease as it was; the bonus is for next time.
            intervalDays = if (first) {
                FIRST_EASY_INTERVAL_DAYS
            } else {
                jsRoundToInt(prev.intervalDays * ease * EASY_INTERVAL_FACTOR)
            }
            ease += EASY_EASE_BONUS
        }
    }
    // A successful review never leaves the interval where it was; "hard" is allowed to.
    if (grade != Grade.Hard) intervalDays = maxOf(intervalDays, prev.intervalDays + 1)
    return prev.copy(
        ease = ease,
        intervalDays = intervalDays,
        repetitions = prev.repetitions + 1,
        due = now + intervalDays * DAY_MS,
        lastReviewed = now,
    )
}

/** Human-friendly preview of when a card would come back, for the grade buttons. */
fun describeInterval(prev: ReviewState, grade: Grade, now: Long): String {
    val wait = schedule(prev, grade, now).due - now
    if (wait < HOUR_MS) return "${jsRound(wait.toDouble() / MINUTE_MS)}m"
    val days = jsRound(wait.toDouble() / DAY_MS)
    return when {
        days < DAYS_PER_MONTH -> "${days}d"

        days < DAYS_PER_YEAR -> "${jsRound(days.toDouble() / DAYS_PER_MONTH)}mo"

        else -> {
            val tenths = jsRound(days * TENTHS / DAYS_PER_YEAR)
            "${tenths / TENTHS.toLong()}.${tenths % TENTHS.toLong()}y"
        }
    }
}

/** Years are shown to one decimal place. */
private const val TENTHS = 10.0

/** A card counts as "learned" once it has survived a multi-day interval. */
fun isLearned(state: ReviewState?): Boolean = state != null && state.intervalDays >= LEARNED_INTERVAL_DAYS

/**
 * Consecutive days (ending today, or yesterday if nothing yet today) on which the student studied or trained:
 * a day with at least one review, or one of [classDays], the day keys of the classes they logged.
 */
fun streakDays(reviewTimestamps: List<Long>, now: Long, zone: LocalZone, classDays: List<String> = emptyList()): Int {
    val days = reviewTimestamps.mapTo(HashSet()) { localDayKey(it, zone) }
    days += classDays
    var cursor = localDayNumber(now, zone)
    if (dayKeyOf(cursor) !in days) cursor--
    var streak = 0
    while (dayKeyOf(cursor) in days) {
        streak++
        cursor--
    }
    return streak
}
