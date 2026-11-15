package dev.justinsmith.bjj.core

/**
 * Starter items worth offering to a library: no stored item of the same kind has the same id, and the user has
 * not dismissed them. Starter content is only inserted when storage is first created, so this is how items
 * added to the app later reach a library that already exists. Missing cannot tell "never had it" from "deleted
 * it on purpose"; dismissing covers the second.
 */
private fun <T : HasId> notStoredOrDismissed(
    starter: List<T>,
    stored: List<HasId>,
    dismissed: List<StarterDismissal>,
): List<T> {
    val skipIds = (stored + dismissed).mapTo(HashSet()) { it.id }
    return starter.filter { it.id !in skipIds }
}

/** Starter techniques to offer: missing, not dismissed, and the positions they connect still exist. */
fun findMissingStarterTechniques(
    starter: List<Transition>,
    stored: List<Transition>,
    positions: List<Position>,
    dismissed: List<StarterDismissal>,
): List<Transition> {
    val positionIds = positions.mapTo(HashSet()) { it.id }
    return notStoredOrDismissed(starter, stored, dismissed).filter {
        it.fromId in positionIds && (endsTheRoll(it.type) || it.toId in positionIds)
    }
}

/** Starter concepts to offer: missing and not dismissed. */
fun findMissingStarterConcepts(
    starter: List<Concept>,
    stored: List<Concept>,
    dismissed: List<StarterDismissal>,
): List<Concept> = notStoredOrDismissed(starter, stored, dismissed)

/**
 * The stored positions that should list a newly added concept, with the concept appended: each starter position
 * whose starter version lists it, if the library still has that position and it does not list the concept yet.
 * Only ever adds links, so the user's own edits to a position's concepts are kept.
 */
fun linkAddedConcepts(
    addedConceptIds: Set<String>,
    starterPositions: List<Position>,
    stored: List<Position>,
): List<Position> {
    val starterById = starterPositions.associateBy { it.id }
    return stored.mapNotNull { position ->
        val toAdd = starterById[position.id]?.conceptIds.orEmpty()
            .filter { it in addedConceptIds && it !in position.conceptIds }
        if (toAdd.isEmpty()) null else position.copy(conceptIds = position.conceptIds + toAdd)
    }
}
