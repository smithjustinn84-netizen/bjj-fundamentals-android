package dev.justinsmith.bjj.core

/** An in-memory snapshot of everything a screen needs to show lists and look items up. */
data class Library(
    /** In learning order. */
    val concepts: List<Concept>,
    /** In learning order. */
    val positions: List<Position>,
    /** By starting position, then name. */
    val transitions: List<Transition>,
    /** Keyed by card id (`kind:itemId`). */
    val reviews: Map<String, ReviewState>,
) {
    val conceptById: Map<String, Concept> = concepts.associateBy { it.id }
    val positionById: Map<String, Position> = positions.associateBy { it.id }
    val transitionById: Map<String, Transition> = transitions.associateBy { it.id }
}

/**
 * Orders two names the way a reader would: letters before their case. The web app asks the browser, which
 * knows every language; this is that order for the plain text the starter content and most names are.
 */
internal fun compareNames(a: String, b: String): Int {
    val folded = a.lowercase().compareTo(b.lowercase())
    // The same letters: lowercase first, as a dictionary has it.
    return if (folded != 0) folded else b.compareTo(a)
}

/** Sorts into learning order (transitions follow their starting position) and builds lookup maps. */
fun buildLibrary(
    concepts: List<Concept>,
    positions: List<Position>,
    transitions: List<Transition>,
    reviews: List<ReviewState>,
): Library {
    val sortedConcepts = concepts.sortedWith { a, b ->
        a.order.compareTo(b.order).orElse { compareNames(a.name, b.name) }
    }
    val sortedPositions = positions.sortedWith { a, b ->
        a.order.compareTo(b.order).orElse { compareNames(a.name, b.name) }
    }
    val positionRank = sortedPositions.withIndex().associate { (index, position) -> position.id to index }
    val rank = { t: Transition -> positionRank[t.fromId] ?: Int.MAX_VALUE }
    val sortedTransitions = transitions.sortedWith { a, b ->
        rank(a).compareTo(rank(b)).orElse { compareNames(a.name, b.name) }
    }
    return Library(sortedConcepts, sortedPositions, sortedTransitions, reviews.associateBy { it.id })
}

private inline fun Int.orElse(next: () -> Int): Int = if (this != 0) this else next()

/** The study card for an item, or null if it has never been reviewed. */
fun reviewOf(lib: Library, kind: CardKind, id: String): ReviewState? = lib.reviews[cardId(kind, id)]

/** Display name of any item, or null if it no longer exists. */
fun itemName(lib: Library, kind: ItemKind, id: String): String? = when (kind) {
    ItemKind.Concept -> lib.conceptById[id]?.name
    ItemKind.Position -> lib.positionById[id]?.name
    ItemKind.Transition -> lib.transitionById[id]?.name
}

data class Connections(
    /** Techniques that start here, including any that also end here. */
    val outgoing: List<Transition>,
    /** Techniques from elsewhere that end here. */
    val incoming: List<Transition>,
)

/** The techniques touching a position. Deleting the position deletes exactly these. */
fun connections(lib: Library, positionId: String): Connections = Connections(
    outgoing = lib.transitions.filter { it.fromId == positionId },
    incoming = lib.transitions.filter { it.toId == positionId && it.fromId != positionId },
)
