package dev.justinsmith.bjj.core

private val DAY_KEY = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
private const val MONTHS_PER_YEAR = 12

/** The longest a month gets. A day key is a label for sorting and display, so 31 is allowed in every month. */
private const val MAX_DAY_OF_MONTH = 31
private const val MINUTES_PER_HOUR = 60

/** Whether a text is a calendar day written `YYYY-MM-DD`, the form [localDayKey] and a date picker both produce. */
fun isDayKey(value: String): Boolean {
    val (_, month, day) = DAY_KEY.find(value)?.destructured ?: return false
    return month.toInt() in 1..MONTHS_PER_YEAR && day.toInt() in 1..MAX_DAY_OF_MONTH
}

/** Most recent class first; two classes on one day keep the later write-up on top. */
fun newestFirst(entries: List<TrainingEntry>): List<TrainingEntry> =
    entries.sortedWith(compareByDescending<TrainingEntry> { it.day }.thenByDescending { it.createdAt })

/** The situations an entry says the student got stuck in. Entries stored before the field existed have none. */
fun stuckSituationIds(entry: TrainingEntry): List<String> = entry.stuckIds.orEmpty()

data class TrainingSummary(
    val classes: Int,
    val minutes: Int,
    /** The focus set after the most recent class that set one, or "" if none has. */
    val focus: String,
)

fun trainingSummary(entries: List<TrainingEntry>): TrainingSummary = TrainingSummary(
    classes = entries.size,
    minutes = entries.sumOf { it.minutes },
    focus = newestFirst(entries).firstOrNull { it.focus.isNotEmpty() }?.focus.orEmpty(),
)

/** Mat time for display: "45 min", "2 h", "1 h 30 min". */
fun formatMatTime(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}

/** "1 class", "3 classes". [plural] only handles nouns that take a plain "s". */
fun formatClassCount(count: Int): String = "$count ${if (count == 1) "class" else "classes"}"

/** How many of the latest classes count as recent when deciding what to study first. */
const val RECENT_CLASS_COUNT = 3

private fun recent(entries: List<TrainingEntry>, classCount: Int) = newestFirst(entries).take(maxOf(0, classCount))

/** Ids of the techniques practised in the latest classes, most recent class first, each id once. */
fun recentlyPractised(entries: List<TrainingEntry>, classCount: Int): List<String> =
    recent(entries, classCount).flatMap { it.techniqueIds }.distinct()

/**
 * The choices a class write-up offers up front, before the full list: what the entry already has, then what
 * recent classes had, each id once and only ids that still exist.
 */
fun quickPicks(picked: List<String>, recent: List<String>, known: (String) -> Boolean): List<String> =
    (picked + recent).distinct().filter(known)

/**
 * Where the student got stuck in the most recent of the latest classes that named anywhere, as situation ids.
 * An older class is left out: its sticking points no longer come first in review.
 */
fun recentlyStuck(entries: List<TrainingEntry>, classCount: Int): List<String> =
    recent(entries, classCount).map(::stuckSituationIds).firstOrNull { it.isNotEmpty() }.orEmpty()

/**
 * The cards to study before any other new card, as card ids, from the latest classes. Where the student got
 * stuck comes first: that situation's card, then each technique they have from there. Then come the
 * techniques they practised. Each id appears once.
 */
fun studyFirst(lib: Library, entries: List<TrainingEntry>, classCount: Int): List<String> {
    val stuck = recent(entries, classCount).flatMap(::stuckSituationIds).flatMap { id ->
        val situation = findSituation(lib, id) ?: return@flatMap emptyList()
        listOf(cardId(CardKind.Situation, id)) + situation.options.map { cardId(CardKind.Transition, it.id) }
    }
    val practised = recentlyPractised(entries, classCount).map { cardId(CardKind.Transition, it) }
    return (stuck + practised).distinct()
}
