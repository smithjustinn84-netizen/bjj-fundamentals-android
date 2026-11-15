package dev.justinsmith.bjj.core.testing

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.ReviewState
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.newReviewState

// Minimal valid entities for tests. Name only the fields a test cares about.

fun concept(
    id: String = "c1",
    name: String = "Concept",
    summary: String = "",
    keyPoints: List<String> = emptyList(),
    drill: String = "",
    order: Int = 1,
    notes: String = "",
    createdAt: Long = 0,
    updatedAt: Long = 0,
) = Concept(id, name, summary, keyPoints, drill, order, notes, createdAt, updatedAt)

fun position(
    id: String = "p1",
    name: String = "Position",
    category: PositionCategory = PositionCategory.Guard,
    summary: String = "",
    topGoals: List<String> = emptyList(),
    bottomGoals: List<String> = emptyList(),
    conceptIds: List<String> = emptyList(),
    order: Int = 1,
    notes: String = "",
    createdAt: Long = 0,
    updatedAt: Long = 0,
) = Position(id, name, category, summary, topGoals, bottomGoals, conceptIds, order, notes, createdAt, updatedAt)

fun transition(
    id: String = "t1",
    name: String = "Transition",
    type: TransitionType = TransitionType.Sweep,
    fromId: String = "p1",
    toId: String = "p1",
    role: PositionRole? = null,
    endRole: PositionRole? = null,
    steps: List<String> = emptyList(),
    commonMistakes: List<String> = emptyList(),
    drill: String? = null,
    blueBeltTest: Boolean? = null,
    notes: String = "",
    createdAt: Long = 0,
    updatedAt: Long = 0,
) = Transition(
    id = id,
    name = name,
    type = type,
    fromId = fromId,
    toId = toId,
    role = role,
    endRole = endRole,
    steps = steps,
    commonMistakes = commonMistakes,
    drill = drill,
    blueBeltTest = blueBeltTest,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** A study card for an item, starting from a never-reviewed card due at time 0. */
fun review(
    kind: CardKind,
    itemId: String,
    intervalDays: Int = 0,
    repetitions: Int = 0,
    lapses: Int = 0,
    ease: Double? = null,
    due: Long = 0,
    lastReviewed: Long = 0,
): ReviewState = newReviewState(kind, itemId, 0).let {
    it.copy(
        intervalDays = intervalDays,
        repetitions = repetitions,
        lapses = lapses,
        ease = ease ?: it.ease,
        due = due,
        lastReviewed = lastReviewed,
    )
}

fun trainingEntry(
    id: String = "log1",
    day: String = "2026-11-14",
    minutes: Int = 60,
    taught: String = "",
    worked: String = "",
    struggled: String = "",
    focus: String = "",
    techniqueIds: List<String> = emptyList(),
    stuckIds: List<String>? = null,
    createdAt: Long = 0,
    updatedAt: Long = 0,
) = TrainingEntry(id, day, minutes, taught, worked, struggled, focus, techniqueIds, stuckIds, createdAt, updatedAt)
