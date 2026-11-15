package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.seed.STARTER_LESSONS

/**
 * A class-sized lesson: a few concepts, the position they matter in and the techniques that use them, with a
 * script for practising them with a partner. Lessons are a lookup by starter id, not stored data, so they need
 * no migration; progress is worked out from the review state of the cards.
 */
data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val conceptIds: List<String>,
    val positionIds: List<String>,
    val transitionIds: List<String>,
    /** About fifteen minutes with a partner, built from the drills of the lesson's techniques. */
    val practice: String,
)

/** The foundation track, in the order a beginner should take it: survive, then the guard, then the rest. */
val FOUNDATION_LESSONS: List<Lesson> = STARTER_LESSONS

fun findLesson(id: String?): Lesson? = FOUNDATION_LESSONS.firstOrNull { it.id == id }

/**
 * A lesson's cards in the order they are taught: its first concept, its positions, its techniques one at a
 * time with a remaining concept after each, then the situation card for each side the lesson gave a technique
 * to. Items the library no longer has are left out.
 */
fun lessonCards(lib: Library, lesson: Lesson): List<StudyItem> {
    val concepts = lesson.conceptIds.filter { it in lib.conceptById }
    val positions = lesson.positionIds.filter { it in lib.positionById }
    val techniques = lesson.transitionIds.mapNotNull { lib.transitionById[it] }
    val otherConcepts = concepts.drop(1)
    return buildList {
        concepts.firstOrNull()?.let { add(StudyItem(CardKind.Concept, it)) }
        positions.mapTo(this) { StudyItem(CardKind.Position, it) }
        for (i in 0 until maxOf(techniques.size, otherConcepts.size)) {
            techniques.getOrNull(i)?.let { add(StudyItem(CardKind.Transition, it.id)) }
            otherConcepts.getOrNull(i)?.let { add(StudyItem(CardKind.Concept, it)) }
        }
        for (positionId in positions) {
            for (role in PositionRole.entries) {
                val taught = techniques.any { it.fromId == positionId && performerRole(lib, it) == role }
                val id = situationId(positionId, role)
                if (taught && findSituation(lib, id) != null) add(StudyItem(CardKind.Situation, id))
            }
        }
    }
}

data class LessonProgress(
    /** The lesson's cards. */
    val total: Int,
    /** How many of them have been reviewed at least once. */
    val started: Int,
    val learned: Int,
)

fun lessonProgress(lib: Library, lesson: Lesson): LessonProgress {
    val states = lessonCards(lib, lesson).map { reviewOf(lib, it.kind, it.id) }
    return LessonProgress(
        total = states.size,
        started = states.count {
            it != null
        },
        learned = states.count(::isLearned),
    )
}

/** The first lesson with a card still to learn, or null once every lesson is learned or has no cards here. */
fun currentLesson(lib: Library): Lesson? = FOUNDATION_LESSONS.firstOrNull { lesson ->
    lessonProgress(lib, lesson).let { it.learned < it.total }
}
