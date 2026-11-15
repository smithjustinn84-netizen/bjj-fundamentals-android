package dev.justinsmith.bjj.core

// How the starter techniques answer one another. Kept as a lookup by starter id, not as stored fields, so it
// needs no migration and no backup change; a technique the user added has no links.

/** An attack and the technique that defends it. */
private val STARTER_DEFENCES: List<Pair<String, String>> = listOf(
    "armbar-guard" to "armbar-defence",
    "triangle-choke" to "triangle-defence",
    "guillotine-guard" to "guillotine-defence",
    "kimura-guard" to "kimura-defence",
    "americana" to "americana-defence",
    "rear-naked-choke" to "rear-naked-choke-defence",
    "double-leg" to "sprawl",
    "single-leg" to "sprawl",
)

/** A technique in a dilemma, and what the other person does that makes it the one to use. */
private class Member(val id: String, val whenText: String)

/**
 * Techniques that cover for each other: the reaction that stops one opens the next. The text says what the
 * other person does that makes this the one to use, written to follow the technique's name.
 */
private val STARTER_DILEMMAS: List<List<Member>> = listOf(
    listOf(
        Member("hip-bump-sweep", "when they sit back on their heels"),
        Member("kimura-guard", "when they post a hand on the mat to stop the bump"),
        Member("guillotine-guard", "when they pull the arm back and leave the head low"),
    ),
    listOf(
        Member("armbar-guard", "when an arm crosses your centre line"),
        Member("triangle-choke", "when they yank one arm free"),
        Member("pendulum-sweep", "when they stack their weight into you"),
    ),
    listOf(
        Member("upa-escape", "when they reach for your collar or post a hand close"),
        Member("elbow-knee-escape", "when they base out wide to stop the bridge"),
    ),
    listOf(
        Member("side-control-escape", "when space opens at your hip"),
        Member("side-control-escape-knees", "when they block your bottom knee"),
    ),
)

data class FollowUp(
    val technique: Transition,
    /** What the other person does that makes this the technique to switch to. */
    val whenText: String,
)

data class TechniqueLinks(
    /** Techniques that defend this one. */
    val defences: List<Transition>,
    /** Techniques this one defends. */
    val answers: List<Transition>,
    /** What to switch to when this one is stopped. */
    val followUps: List<FollowUp>,
) {
    val isEmpty: Boolean
        get() = defences.isEmpty() && answers.isEmpty() && followUps.isEmpty()
}

/** How a technique connects to others in the library. Links to techniques the library no longer has are left out. */
fun techniqueLinks(lib: Library, id: String): TechniqueLinks = TechniqueLinks(
    defences = STARTER_DEFENCES.filter { it.first == id }.mapNotNull { lib.transitionById[it.second] },
    answers = STARTER_DEFENCES.filter { it.second == id }.mapNotNull { lib.transitionById[it.first] },
    followUps = STARTER_DILEMMAS
        .filter { dilemma -> dilemma.any { it.id == id } }
        .flatten()
        .filter { it.id != id }
        .mapNotNull { member -> lib.transitionById[member.id]?.let { FollowUp(it, member.whenText) } },
)

/** Every starter id the links name, for the test that each is a starter technique. */
internal val CHAIN_IDS: Set<String> =
    STARTER_DEFENCES.flatMap { it.toList() }.toSet() + STARTER_DILEMMAS.flatten().map { it.id }
