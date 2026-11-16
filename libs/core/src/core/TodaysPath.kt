package dev.justinsmith.bjj.core

/** The steps of the path Today suggests, in the order a beginner takes them: survive first, attacks later. */
enum class PathStepKind { Survive, Concepts, Positions, Attack, Videos }

/**
 * One step of the suggested path. [next] is the first thing in it not studied yet, with its name; null when
 * the step has nothing left to start, or is not about study cards at all.
 */
data class PathStep(val kind: PathStepKind, val done: Boolean, val next: StudyItem? = null, val nextName: String = "")

/**
 * The path Today suggests through the library: get out of bad spots, learn the ideas, learn the positions,
 * then attack, and save a video along the way. A step about cards is done once the library has such cards and
 * every one has been studied at least once; the videos step once one bookmark is saved.
 */
fun todaysPath(lib: Library, videoCount: Int): List<PathStep> {
    val unstudied = { kind: CardKind, id: String -> cardId(kind, id) !in lib.reviews }
    val nextConcept = lib.concepts.firstOrNull { unstudied(CardKind.Concept, it.id) }
    val nextPosition = lessonOrder(lib)
        .filter { it.kind == CardKind.Position && unstudied(it.kind, it.id) }
        .firstNotNullOfOrNull { lib.positionById[it.id] }
    val notStarted = lessonTechniques(lib).filter { unstudied(CardKind.Transition, it.id) }
    val nextEscape = notStarted.firstOrNull { isEscape(it.type) }
    val nextAttack = notStarted.firstOrNull { !isEscape(it.type) }

    fun step(kind: PathStepKind, has: Boolean, cardKind: CardKind, id: String?, name: String?) = PathStep(
        kind = kind,
        done = has && id == null,
        next = id?.let { StudyItem(cardKind, it) },
        nextName = name.orEmpty(),
    )
    return listOf(
        step(
            PathStepKind.Survive,
            lib.transitions.any { isEscape(it.type) },
            CardKind.Transition,
            nextEscape?.id,
            nextEscape?.name,
        ),
        step(PathStepKind.Concepts, lib.concepts.isNotEmpty(), CardKind.Concept, nextConcept?.id, nextConcept?.name),
        step(
            PathStepKind.Positions,
            lib.positions.isNotEmpty(),
            CardKind.Position,
            nextPosition?.id,
            nextPosition?.name,
        ),
        step(
            PathStepKind.Attack,
            lib.transitions.any { !isEscape(it.type) },
            CardKind.Transition,
            nextAttack?.id,
            nextAttack?.name,
        ),
        PathStep(PathStepKind.Videos, done = videoCount > 0),
    )
}
