package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.seed.STARTER_DRILLS

/**
 * The drill for a technique, or "" when it has none. Techniques stored before the field existed fall back to
 * the starter content's drill; one the user has cleared stays cleared.
 */
fun techniqueDrill(transition: Transition): String = transition.drill ?: STARTER_DRILLS[transition.id].orEmpty()
