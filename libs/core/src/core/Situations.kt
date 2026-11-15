package dev.justinsmith.bjj.core

/**
 * A situation is one side of one position: "under mount", "passing the closed guard". It is studied as a card
 * of its own, because knowing each technique is not the same as knowing which ones you have from here.
 */
data class Situation(
    val position: Position,
    val role: PositionRole,
    /** What this person can do from here, in library order. Never empty: a side with nothing has no situation. */
    val options: List<Transition>,
)

/** The id a situation's study card is stored under. The role comes last so a position id may hold any text. */
fun situationId(positionId: String, role: PositionRole): String = "$positionId:${role.key}"

val Situation.id: String
    get() = situationId(position.id, role)

/** The position a situation id names, whether or not it still exists. */
fun situationPositionId(id: String): String = id.substring(0, maxOf(0, id.lastIndexOf(':')))

/** The role a situation id ends with, or null if it is not a situation id at all. */
fun situationRole(id: String): PositionRole? = PositionRole.entries.firstOrNull { id.endsWith(":${it.key}") }

/** The situation an id names, or null if its position is gone or that side has nothing to do there. */
fun findSituation(lib: Library, id: String): Situation? {
    val position = lib.positionById[situationPositionId(id)] ?: return null
    val role = situationRole(id) ?: return null
    val options = positionOptions(lib, position.id)[role]
    return if (options.isEmpty()) null else Situation(position, role, options)
}

/** Every situation in the library: positions in learning order, each side that has something to do. */
fun allSituations(lib: Library): List<Situation> = lib.positions.flatMap { position ->
    val options = positionOptions(lib, position.id)
    PositionRole.entries.filter { options[it].isNotEmpty() }.map { Situation(position, it, options[it]) }
}

/** "Mount, underneath", "Closed Guard, passing": how a situation is named in lists and questions. */
fun situationName(situation: Situation): String =
    "${situation.position.name}, ${endSideLabel(situation.position.category, situation.role)}"

/** What the person on one side of a position is trying to do there. */
fun roleGoals(position: Position, role: PositionRole): List<String> = when (role) {
    PositionRole.Top -> position.topGoals
    PositionRole.Bottom -> position.bottomGoals
}
