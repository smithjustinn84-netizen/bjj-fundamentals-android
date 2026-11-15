package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.seed.STARTER_BLUE_BELT_TEST_IDS

/**
 * Whether a technique is on the blue belt test list. Techniques stored before the flag existed fall back to
 * the starter default, so an existing library shows the same list as a fresh one until the user edits it.
 */
fun isOnBlueBeltTest(transition: Transition): Boolean =
    transition.blueBeltTest ?: (transition.id in STARTER_BLUE_BELT_TEST_IDS)

data class BeltTestGroup(
    val type: TransitionType,
    /** In library order: by starting position, then name. */
    val techniques: List<Transition>,
)

data class BeltTestList(
    /** One group per technique type that has something on the list, in the types' own order. */
    val groups: List<BeltTestGroup>,
    val total: Int,
    /** How many of the listed techniques have a learned study card. */
    val learned: Int,
)

/** The techniques on the blue belt test, grouped by type, with how many of them have been learned. */
fun blueBeltTestList(lib: Library): BeltTestList {
    val listed = lib.transitions.filter(::isOnBlueBeltTest)
    val groups = TransitionType.entries
        .map { type -> BeltTestGroup(type, listed.filter { it.type == type }) }
        .filter { it.techniques.isNotEmpty() }
    val learned = listed.count { isLearned(reviewOf(lib, CardKind.Transition, it.id)) }
    return BeltTestList(groups, total = listed.size, learned = learned)
}
