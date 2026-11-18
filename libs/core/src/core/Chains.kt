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
    "americana-mount" to "americana-defence",
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
        Member("omoplata-guard", "when they post a hand on the mat beside your ribs"),
    ),
    listOf(
        Member("upa-escape", "when they reach for your collar or post a hand close"),
        Member("elbow-knee-escape", "when they base out wide to stop the bridge"),
    ),
    listOf(
        Member("side-control-escape", "when space opens at your hip"),
        Member("side-control-escape-knees", "when they block your bottom knee"),
    ),
    listOf(
        Member("break-closed-guard", "when their hips stay flat and you can wedge from your knees"),
        Member("standing-guard-break", "when their ankles stay locked tight against the kneeling break"),
    ),
    listOf(
        Member("cross-collar-guard", "when they keep both hands low on your hips or belt"),
        Member("armbar-guard", "when they bring a hand up to strip the collar grip"),
        Member("scissor-sweep", "when they posture up away from the collar grip"),
    ),
    listOf(
        Member("cross-collar-choke", "when they keep their elbows tight to their ribs"),
        Member("ezekiel-mount", "when they hand-fight your collar grips"),
        Member("americana-mount", "when a wrist comes up beside their head to frame"),
        Member("arm-triangle-mount", "when an arm crosses their centerline to frame or push your shoulder"),
        Member("armbar-mount", "when they push your chest with a straight arm"),
        Member("mount-to-back", "when they turn onto their side to escape"),
    ),
    listOf(
        Member("americana", "when their far wrist comes up beside their head"),
        Member("kimura-side-control", "when their far arm pushes at your neck or shoulder"),
        Member("side-control-to-back", "when their near arm crosses to push your face"),
        Member("side-to-mount", "when their arms stay tucked and their hips stay flat"),
    ),
    listOf(
        Member("rear-naked-choke", "when they fight your lapel grips and leave the neck open"),
        Member("sliding-collar-choke", "when they tuck their chin against the bare-arm choke"),
    ),
    listOf(
        Member("rear-naked-choke-defence", "when their choking arm is already threatening your neck"),
        Member(
            "back-escape",
            "when your hands control the choking wrist and you can clear your shoulders to the mat",
        ),
    ),
    listOf(
        Member("half-guard-underhook-sweep", "when they press their weight in and leave the far armpit open"),
        Member("knee-shield-sweep", "when they block the underhook and lean on your shield"),
        Member("half-to-full-guard", "when they sit back to free their trapped leg"),
    ),
    listOf(
        Member("knee-slice", "when their bottom knee opens or a frame slips"),
        Member("half-guard-to-mount", "when they clamp your ankle and lie flat under the crossface"),
    ),
    listOf(
        Member("toreando-pass", "when their knees are loose and you can stand out of reach"),
        Member("double-under-pass", "when they pull you in and lift their legs high"),
    ),
    listOf(
        Member("butterfly-sweep", "when they kneel in front of you"),
        Member("tripod-sweep", "when they stand up in your guard"),
    ),
    listOf(
        Member("guard-retention", "when they pressure in close and you need to keep your legs in front"),
        Member("technical-stand-up", "when they back away and leave space to get back to your feet"),
    ),
    listOf(
        Member("turtle-to-back", "when a gap opens between their elbow and knee"),
        Member("turtle-breakdown", "when they seal their elbows to their knees"),
    ),
    listOf(
        Member("turtle-recover-guard", "when they sit beside you without trapping your far hip"),
        Member("turtle-sit-out", "when they lean their chest on your back with no hooks in"),
    ),
    listOf(
        Member("double-leg", "when they stand square with their feet level"),
        Member("single-leg", "when one foot steps out in front"),
        Member("hip-throw", "when they lean in and push forward"),
        Member("pull-guard", "when they stall or win the wrestling exchange"),
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
