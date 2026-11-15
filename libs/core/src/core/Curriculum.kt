package dev.justinsmith.bjj.core

/** One thing that can be studied as a flashcard. */
data class StudyItem(
    val kind: CardKind,
    /** The item's id, or for a situation its `situationId`. */
    val id: String,
)

val StudyItem.cardId: String
    get() = cardId(kind, id)

/**
 * The order a position's techniques are taught in. Escapes come in the survive phase, before any of the
 * others; of the rest, the ones that improve your position come before the one that ends the roll
 * ("position before submission").
 */
private val TransitionType.lessonRank: Int
    get() = when (this) {
        TransitionType.Escape -> 0
        TransitionType.Sweep -> 1
        TransitionType.Pass -> 2
        TransitionType.Transition -> 3
        TransitionType.Takedown -> 4
        TransitionType.Submission -> 5
    }

/** An escape that ends where it began only stops one attack: the person is still stuck there afterwards. */
private fun staysPut(technique: Transition): Boolean = technique.toId == technique.fromId

/** A position's escapes: the ways out of it first, then the defences that only stop an attack there. */
private fun escapesFrom(techniques: List<Transition>): List<Transition> {
    val (defences, waysOut) = techniques.filter { isEscape(it.type) }.partition(::staysPut)
    return waysOut + defences
}

/** One from each list in turn, techniques leading, until both run out. */
private fun alternate(techniques: List<StudyItem>, concepts: List<StudyItem>): List<StudyItem> = buildList {
    for (i in 0 until maxOf(techniques.size, concepts.size)) {
        techniques.getOrNull(i)?.let(::add)
        concepts.getOrNull(i)?.let(::add)
    }
}

/** Hands each card out once: a card taught already is skipped wherever it comes up again. */
private class Syllabus {
    val cards = mutableListOf<StudyItem>()
    private val taught = HashSet<StudyItem>()

    /** The cards among [items] not taught yet, now counted as taught, without adding them to [cards]. */
    fun claim(items: List<StudyItem>): List<StudyItem> = items.filter(taught::add)

    fun teach(items: List<StudyItem>) {
        cards += claim(items)
    }

    fun teach(kind: CardKind, id: String) = teach(listOf(StudyItem(kind, id)))
}

private fun List<Concept>.conceptCards() = map { StudyItem(CardKind.Concept, it.id) }

private fun List<Transition>.techniqueCards() = map { StudyItem(CardKind.Transition, it.id) }

/**
 * Every card once, in the order a coach would teach a beginner: survive first, attack later.
 *
 * The foundation lessons come first, each as a block (see [lessonCards]); they open with surviving side
 * control and mount. Everything the lessons do not cover, including anything the user added, follows by rule.
 * The rule opens with the first concept in the student's own order. Its survive phase visits every position,
 * the pins first because that is where a beginner spends the first months: the position, its escapes (ways
 * out before defences that leave you there) woven with the concepts it lists that are still new, then the
 * situation card for each side that has an escape. A concept no position lists follows each lesson, and any
 * still left close the phase. Its attack phase then visits the positions in their own order for the remaining
 * techniques, submissions last, and the remaining situations. Techniques whose starting position is gone come
 * at the very end.
 */
fun lessonOrder(lib: Library): List<StudyItem> {
    val syllabus = Syllabus()
    val from = { position: Position -> lib.transitions.filter { it.fromId == position.id } }

    for (lesson in FOUNDATION_LESSONS) syllabus.teach(lessonCards(lib, lesson))

    syllabus.teach(lib.concepts.take(1).conceptCards())
    val listed = lib.positions.flatMapTo(HashSet()) { it.conceptIds }
    val unlisted = lib.concepts.drop(1).filter { it.id !in listed }

    val (pins, others) = lib.positions.partition { isPin(it.category) }
    (pins + others).forEachIndexed { lesson, position ->
        syllabus.teach(CardKind.Position, position.id)
        val itsConcepts = lib.concepts.filter { it.id in position.conceptIds }
        // Claimed list by list, techniques first, as the web app does: a card in both keeps its first place.
        val escapes = syllabus.claim(escapesFrom(from(position)).techniqueCards())
        syllabus.cards += alternate(escapes, syllabus.claim(itsConcepts.conceptCards()))
        val options = positionOptions(lib, position.id)
        for (role in PositionRole.entries) {
            if (options[role].any { isEscape(it.type) }) {
                syllabus.teach(CardKind.Situation, situationId(position.id, role))
            }
        }
        syllabus.teach(unlisted.drop(lesson).take(1).conceptCards())
    }
    syllabus.teach(unlisted.conceptCards())

    for (position in lib.positions) {
        syllabus.teach(from(position).sortedBy { it.type.lessonRank }.techniqueCards())
        val options = positionOptions(lib, position.id)
        for (role in PositionRole.entries) {
            if (options[role].isNotEmpty()) syllabus.teach(CardKind.Situation, situationId(position.id, role))
        }
    }

    syllabus.teach(lib.concepts.conceptCards())
    syllabus.teach(lib.transitions.techniqueCards())
    return syllabus.cards
}

/** The techniques in the order their cards are taught: escapes from the pins first. */
fun lessonTechniques(lib: Library): List<Transition> = lessonOrder(lib).mapNotNull { item ->
    if (item.kind == CardKind.Transition) lib.transitionById[item.id] else null
}
