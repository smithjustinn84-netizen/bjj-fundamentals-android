package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.seed.STARTER_END_ROLES
import dev.justinsmith.bjj.core.seed.STARTER_ROLES

/**
 * Who usually does a technique of this type, for one the user has not said. A sweep or escape is done by the
 * person underneath and a pass or takedown by the person attacking; a submission comes from the bottom in a
 * guard and from the top everywhere else.
 */
fun defaultRole(type: TransitionType, category: PositionCategory?): PositionRole = when (type) {
    TransitionType.Sweep, TransitionType.Escape -> PositionRole.Bottom
    TransitionType.Pass, TransitionType.Takedown, TransitionType.Transition -> PositionRole.Top
    TransitionType.Submission -> if (category == PositionCategory.Guard) PositionRole.Bottom else PositionRole.Top
}

/**
 * Who does a technique, as their role in its starting position. Techniques stored before the field existed
 * fall back to the starter content's answer, then to the usual role for their type.
 */
fun performerRole(positionById: Map<String, Position>, transition: Transition): PositionRole =
    transition.role
        ?: STARTER_ROLES[transition.id]
        ?: defaultRole(transition.type, positionById[transition.fromId]?.category)

fun performerRole(lib: Library, transition: Transition): PositionRole = performerRole(lib.positionById, transition)

/**
 * Which side a technique of this type usually leaves the person who did it on, for one the user has not said,
 * or null for one that [endsTheRoll]. A sweep, pass or takedown puts you on top, and anything else leaves you
 * on the side you started on.
 */
fun defaultEndRole(type: TransitionType, role: PositionRole): PositionRole? = when (type) {
    TransitionType.Submission -> null
    TransitionType.Sweep, TransitionType.Pass, TransitionType.Takedown -> PositionRole.Top
    TransitionType.Escape, TransitionType.Transition -> role
}

/**
 * Which side of the destination the person who did a technique ends up on, or null for one that [endsTheRoll].
 * Techniques stored before the field existed fall back to the starter content's answer, then to the usual
 * side for their type.
 */
fun endRole(positionById: Map<String, Position>, transition: Transition): PositionRole? {
    if (endsTheRoll(transition.type)) return null
    return transition.endRole
        ?: STARTER_END_ROLES[transition.id]
        ?: defaultEndRole(transition.type, performerRole(positionById, transition))
}

fun endRole(lib: Library, transition: Transition): PositionRole? = endRole(lib.positionById, transition)

/** The techniques each person can do from a position, in library order. An empty list is a gap in the game. */
data class PositionOptions(val top: List<Transition>, val bottom: List<Transition>) {
    operator fun get(role: PositionRole): List<Transition> = when (role) {
        PositionRole.Top -> top
        PositionRole.Bottom -> bottom
    }
}

fun positionOptions(lib: Library, positionId: String): PositionOptions {
    val (top, bottom) = lib.transitions
        .filter { it.fromId == positionId }
        .partition { performerRole(lib, it) == PositionRole.Top }
    return PositionOptions(top, bottom)
}
