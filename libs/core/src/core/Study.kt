package dev.justinsmith.bjj.core

/**
 * The cards of a deck, in the order new ones are taught. The concept and position decks follow their own
 * numbered lists, as their pages do; every other deck follows the lessons (see [lessonOrder]).
 */
private fun deckItems(lib: Library, deck: Deck): List<StudyItem> = when (deck) {
    Deck.Concept -> lib.concepts.map { StudyItem(CardKind.Concept, it.id) }
    Deck.Position -> lib.positions.map { StudyItem(CardKind.Position, it.id) }
    Deck.All, Deck.Transition, Deck.Situation, Deck.BlueBelt -> lessonOrder(lib).filter { isInDeck(lib, deck, it) }
}

/** What a card is called in a list, or null if what it was about no longer exists. */
fun cardName(lib: Library, item: StudyItem): String? = when (item.kind) {
    CardKind.Concept -> itemName(lib, ItemKind.Concept, item.id)
    CardKind.Position -> itemName(lib, ItemKind.Position, item.id)
    CardKind.Transition -> itemName(lib, ItemKind.Transition, item.id)
    CardKind.Situation -> findSituation(lib, item.id)?.let(::situationName)
}

data class StudyQueue(
    /** Cards already studied whose due time has passed, most overdue first. */
    val due: List<StudyItem>,
    /** Cards never studied: the ones the training log puts first, then the rest in lesson order. */
    val fresh: List<StudyItem>,
    /** What a session will show: all due cards, then up to the limit of fresh ones. */
    val queue: List<StudyItem>,
    /** How many of the queue's fresh cards the training log put first. */
    val fromClass: Int,
)

private fun isInDeck(lib: Library, deck: Deck, item: StudyItem): Boolean = when (deck) {
    Deck.All -> true
    Deck.Concept -> item.kind == CardKind.Concept
    Deck.Position -> item.kind == CardKind.Position
    Deck.Transition -> item.kind == CardKind.Transition
    Deck.Situation -> item.kind == CardKind.Situation
    Deck.BlueBelt -> item.kind == CardKind.Transition && lib.transitionById[item.id]?.let(::isOnBlueBeltTest) == true
}

/**
 * [firstCardIds] are the cards the training log says to study first, in order (see `studyFirst`). Their fresh
 * cards jump the queue of new cards: what happened on the mat this week is learned while it is fresh.
 */
private fun queueOf(
    lib: Library,
    items: List<StudyItem>,
    newLimit: Int,
    now: Long,
    firstCardIds: List<String>,
): StudyQueue {
    val studied = items.mapNotNull { item -> reviewOf(lib, item.kind, item.id)?.let { item to it.due } }
    val due = studied.filter { it.second <= now }.sortedBy { it.second }.map { it.first }
    val fresh = items.filter { reviewOf(lib, it.kind, it.id) == null }
    val place = { item: StudyItem -> firstCardIds.indexOf(item.cardId) }
    val (practised, rest) = fresh.partition { place(it) >= 0 }
    val fromClass = practised.sortedBy(place)
    val ordered = fromClass + rest
    val newCards = ordered.take(maxOf(0, newLimit))
    return StudyQueue(
        due = due,
        fresh = ordered,
        queue = due + newCards,
        fromClass = newCards.count { place(it) >= 0 },
    )
}

/** A session on one deck. */
fun buildQueue(
    lib: Library,
    deck: Deck,
    newLimit: Int,
    now: Long,
    firstCardIds: List<String> = emptyList(),
): StudyQueue = queueOf(lib, deckItems(lib, deck), newLimit, now, firstCardIds)

/** A session on one lesson's cards only, new ones in the lesson's own order. */
fun buildLessonQueue(lib: Library, lesson: Lesson, newLimit: Int, now: Long): StudyQueue =
    queueOf(lib, lessonCards(lib, lesson), newLimit, now, emptyList())

data class ProgressSummary(
    val total: Int,
    val started: Int,
    val learned: Int,
    /** Whole-number percentage of cards learned. */
    val percentLearned: Int,
)

fun progressSummary(lib: Library): ProgressSummary {
    val ids = lessonOrder(lib).map { it.cardId }
    val learned = ids.count { isLearned(lib.reviews[it]) }
    return ProgressSummary(
        total = ids.size,
        started = ids.count { it in lib.reviews },
        learned = learned,
        percentLearned = if (ids.isEmpty()) 0 else jsRoundToInt(learned.toDouble() / ids.size * PERCENT),
    )
}

private const val PERCENT = 100

/** How many new cards a session brings in unless the student moves the slider. */
const val DEFAULT_NEW_CARDS = 5

/** How many sticking points to show at once: enough to work on before the next class, few enough to read at once. */
const val STICKING_POINT_LIMIT = 5

/** One miss is normal on a new card; a card is a sticking point once it has been forgotten this many times. */
private const val STICKING_POINT_MIN_LAPSES = 2

/**
 * Cards that keep slipping: forgotten more than once and not learned at the moment. Most often forgotten
 * first, then the ones the scheduler finds hardest (lowest ease), then lesson order.
 */
fun stickingPoints(lib: Library, limit: Int): List<StudyItem> = lessonOrder(lib)
    .mapNotNull { item -> reviewOf(lib, item.kind, item.id)?.let { item to it } }
    .filter { (_, state) -> state.lapses >= STICKING_POINT_MIN_LAPSES && !isLearned(state) }
    .sortedWith(compareByDescending<Pair<StudyItem, ReviewState>> { it.second.lapses }.thenBy { it.second.ease })
    .take(maxOf(0, limit))
    .map { it.first }

/** The concepts not learned yet, in learning order, up to [limit]: what to read next. */
fun conceptsToLearn(lib: Library, limit: Int): List<Concept> =
    lib.concepts.filter { !isLearned(reviewOf(lib, CardKind.Concept, it.id)) }.take(maxOf(0, limit))
