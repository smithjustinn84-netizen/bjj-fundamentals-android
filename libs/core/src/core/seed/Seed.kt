package dev.justinsmith.bjj.core.seed

import dev.justinsmith.bjj.core.BackupData
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.VideoBookmark

// Starter content: a small, beginner-level set of ideas. Kept deliberately short: the goal is to give a new
// student a map, not an encyclopedia. The rows themselves (Starter*.kt) are generated from the web app
// (tools/web-fixtures); a starter row leaves out the fields every row gets the same value for.

internal data class StarterConcept(
    val id: String,
    val name: String,
    val summary: String,
    val keyPoints: List<String>,
    val drill: String,
    val order: Int,
)

internal data class StarterPosition(
    val id: String,
    val name: String,
    val category: PositionCategory,
    val summary: String,
    val topGoals: List<String>,
    val bottomGoals: List<String>,
    val conceptIds: List<String>,
    val order: Int,
)

internal data class StarterTransition(
    val id: String,
    val name: String,
    val type: TransitionType,
    val fromId: String,
    val toId: String,
    val role: PositionRole,
    val endRole: PositionRole?,
    val steps: List<String>,
    val commonMistakes: List<String>,
    val drill: String,
)

internal data class StarterVideo(
    val id: String,
    val title: String,
    val youtubeId: String,
    val linkedKind: LinkedKind,
    val linkedId: String,
    val note: String,
)

/**
 * Deliberately every starter technique: each is a fundamental that blue belt tests commonly ask for, so a
 * technique added to the starter content lands on the list too. Kept as a set of ids so techniques stored
 * before the blue belt list existed get the same default as new ones (see `isOnBlueBeltTest`).
 */
val STARTER_BLUE_BELT_TEST_IDS: Set<String> = STARTER_TRANSITIONS.mapTo(LinkedHashSet()) { it.id }

/** Who performs each starter technique, for copies stored before techniques had a role (see `performerRole`). */
val STARTER_ROLES: Map<String, PositionRole> = STARTER_TRANSITIONS.associate { it.id to it.role }

/** The side each starter technique leaves its performer on, for copies stored before that was kept (see `endRole`). */
val STARTER_END_ROLES: Map<String, PositionRole> =
    STARTER_TRANSITIONS.mapNotNull { t -> t.endRole?.let { t.id to it } }.toMap()

/** The drill for each starter technique, for copies stored before techniques had one (see `techniqueDrill`). */
val STARTER_DRILLS: Map<String, String> = STARTER_TRANSITIONS.associate { it.id to it.drill }

/** Everything a brand-new installation starts with. Storage engines insert this once, when first created. */
fun starterContent(now: Long): BackupData = BackupData(
    concepts = STARTER_CONCEPTS.map {
        Concept(it.id, it.name, it.summary, it.keyPoints, it.drill, it.order, notes = "", now, now)
    },
    positions = STARTER_POSITIONS.map {
        Position(
            id = it.id,
            name = it.name,
            category = it.category,
            summary = it.summary,
            topGoals = it.topGoals,
            bottomGoals = it.bottomGoals,
            conceptIds = it.conceptIds,
            order = it.order,
            notes = "",
            createdAt = now,
            updatedAt = now,
        )
    },
    transitions = STARTER_TRANSITIONS.map {
        Transition(
            id = it.id,
            name = it.name,
            type = it.type,
            fromId = it.fromId,
            toId = it.toId,
            role = it.role,
            endRole = it.endRole,
            steps = it.steps,
            commonMistakes = it.commonMistakes,
            drill = it.drill,
            blueBeltTest = true,
            notes = "",
            createdAt = now,
            updatedAt = now,
        )
    },
    videos = STARTER_VIDEOS.map {
        VideoBookmark(
            it.id,
            it.title,
            it.youtubeId,
            startSeconds = 0,
            it.linkedKind,
            it.linkedId,
            it.note,
            createdAt = now,
        )
    },
)
