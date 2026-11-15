package dev.justinsmith.bjj.core

/** Why a drill was picked, most pressing first. */
enum class DrillReason { Stuck, Slipping, Practised, Next }

data class DrillPick(val technique: Transition, val drill: String, val reason: DrillReason)

/**
 * One drill worth doing today, or null if no technique that needs work has one. It answers the most pressing
 * need first: a way out of where the student last got stuck, then a technique that keeps slipping in review,
 * then one practised in a recent class, then the next technique not yet learned, in lesson order. Learned
 * techniques and techniques without a drill are passed over.
 */
fun pickDrill(lib: Library, entries: List<TrainingEntry>): DrillPick? {
    val needsWork = { technique: Transition ->
        techniqueDrill(technique).isNotEmpty() && !isLearned(reviewOf(lib, CardKind.Transition, technique.id))
    }
    val candidates: List<Pair<DrillReason, () -> List<Transition>>> = listOf(
        DrillReason.Stuck to {
            recentlyStuck(entries, RECENT_CLASS_COUNT).flatMap { findSituation(lib, it)?.options.orEmpty() }
        },
        DrillReason.Slipping to {
            // Every sticking point, not Today's short list: that one can be full of cards that are not techniques.
            stickingPoints(lib, lib.reviews.size)
                .filter { it.kind == CardKind.Transition }
                .mapNotNull { lib.transitionById[it.id] }
        },
        DrillReason.Practised to {
            recentlyPractised(entries, RECENT_CLASS_COUNT).mapNotNull { lib.transitionById[it] }
        },
        DrillReason.Next to { lessonTechniques(lib) },
    )
    for ((reason, techniques) in candidates) {
        val technique = techniques().firstOrNull(needsWork) ?: continue
        return DrillPick(technique, techniqueDrill(technique), reason)
    }
    return null
}

data class PositionProgress(
    /** Techniques that start at the position. */
    val total: Int,
    /** How many of them have a learned study card. */
    val learned: Int,
)

fun positionProgress(lib: Library, positionId: String): PositionProgress {
    val techniques = lib.transitions.filter { it.fromId == positionId }
    return PositionProgress(
        total = techniques.size,
        learned = techniques.count { isLearned(reviewOf(lib, CardKind.Transition, it.id)) },
    )
}
