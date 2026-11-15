package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.AppStorage
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StarterDismissal
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.cardId
import dev.justinsmith.bjj.core.cardKind
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.findMissingStarterConcepts
import dev.justinsmith.bjj.core.findMissingStarterTechniques
import dev.justinsmith.bjj.core.linkAddedConcepts
import dev.justinsmith.bjj.core.linkedKind
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.situationId
import dev.justinsmith.bjj.core.situationPositionId
import dev.justinsmith.bjj.core.stuckSituationIds

// What a form hands over: an item without the fields the service fills in (its id and its timestamps), so
// callers cannot get those wrong.

data class ConceptInput(
    val name: String,
    val summary: String,
    val keyPoints: List<String>,
    val drill: String,
    val order: Int,
    val notes: String,
)

data class PositionInput(
    val name: String,
    val category: PositionCategory,
    val summary: String,
    val topGoals: List<String>,
    val bottomGoals: List<String>,
    val conceptIds: List<String>,
    val order: Int,
    val notes: String,
)

data class TransitionInput(
    val name: String,
    val type: TransitionType,
    val fromId: String,
    val toId: String,
    val role: PositionRole?,
    val endRole: PositionRole?,
    val steps: List<String>,
    val commonMistakes: List<String>,
    val drill: String?,
    val blueBeltTest: Boolean?,
    val notes: String,
)

/** Creating, editing and deleting the three kinds of study item. */
class ItemService(env: CoreEnv) {
    private val storage = env.storage
    private val newId = env.newId
    private val now = env.now

    // Each save creates the item when `id` is null, otherwise replaces it keeping its creation time, and
    // returns its id.

    suspend fun saveConcept(id: String?, input: ConceptInput): String {
        val timestamp = now()
        val createdAt = id?.let { storage.concepts.get(it) }?.createdAt ?: timestamp
        val concept = Concept(
            id = id ?: newId(),
            name = input.name,
            summary = input.summary,
            keyPoints = input.keyPoints,
            drill = input.drill,
            order = input.order,
            notes = input.notes,
            createdAt = createdAt,
            updatedAt = timestamp,
        )
        storage.concepts.put(concept)
        return concept.id
    }

    suspend fun savePosition(id: String?, input: PositionInput): String {
        val timestamp = now()
        val createdAt = id?.let { storage.positions.get(it) }?.createdAt ?: timestamp
        val position = Position(
            id = id ?: newId(),
            name = input.name,
            category = input.category,
            summary = input.summary,
            topGoals = input.topGoals,
            bottomGoals = input.bottomGoals,
            conceptIds = input.conceptIds,
            order = input.order,
            notes = input.notes,
            createdAt = createdAt,
            updatedAt = timestamp,
        )
        storage.positions.put(position)
        return position.id
    }

    suspend fun saveTransition(id: String?, input: TransitionInput): String {
        val timestamp = now()
        val createdAt = id?.let { storage.transitions.get(it) }?.createdAt ?: timestamp
        val transition = Transition(
            id = id ?: newId(),
            name = input.name,
            type = input.type,
            fromId = input.fromId,
            // A technique that ends the roll goes nowhere.
            toId = if (endsTheRoll(input.type)) "" else input.toId,
            role = input.role,
            endRole = input.endRole,
            steps = input.steps,
            commonMistakes = input.commonMistakes,
            drill = input.drill,
            blueBeltTest = input.blueBeltTest,
            notes = input.notes,
            createdAt = createdAt,
            updatedAt = timestamp,
        )
        storage.transitions.put(transition)
        return transition.id
    }

    private suspend fun deletePosition(id: String) {
        val touching = storage.transitions.all().filter { it.fromId == id || it.toId == id }.map { it.id }
        for (transitionId in touching) {
            storage.transitions.remove(transitionId)
            storage.reviews.remove(cardId(CardKind.Transition, transitionId))
        }
        storage.unlinkVideos(ItemKind.Transition, touching)
        storage.unlinkFromTrainingLog(touching)
        storage.unlinkSituationsFromTrainingLog(id)
        for (role in PositionRole.entries) storage.reviews.remove(cardId(CardKind.Situation, situationId(id, role)))
        storage.positions.remove(id)
    }

    private suspend fun deleteConcept(id: String) {
        storage.concepts.remove(id)
        val referencing = storage.positions.all().filter { id in it.conceptIds }
        storage.positions.putMany(referencing.map { it.copy(conceptIds = it.conceptIds - id) })
    }

    /** The starter techniques the Blue belt page offers. Call inside a transaction. */
    private suspend fun offeredStarterTechniques(starter: List<Transition>): List<Transition> =
        findMissingStarterTechniques(
            starter,
            storage.transitions.all(),
            storage.positions.all(),
            storage.starterDismissals.all(),
        )

    /** The starter concepts the Concepts page offers. Call inside a transaction. */
    private suspend fun offeredStarterConcepts(starter: List<Concept>): List<Concept> =
        findMissingStarterConcepts(starter, storage.concepts.all(), storage.starterDismissals.all())

    /** The starter videos about the items just added that the library does not hold. Call inside a transaction. */
    private suspend fun starterVideosFor(kind: ItemKind, addedIds: Set<String>, timestamp: Long) =
        storage.videos.all().mapTo(HashSet()) { it.id }.let { stored ->
            starterContent(timestamp).videos.filter {
                it.linkedKind == kind.linkedKind && it.linkedId in addedIds && it.id !in stored
            }
        }

    /**
     * Adds the starter techniques on offer (missing and not dismissed), leaving everything stored untouched. A
     * technique the library never had also gets its starter video bookmark; one the user deleted does not,
     * because deleting kept that bookmark, unlinked. Returns how many techniques were added.
     */
    suspend fun addMissingStarterTechniques(): Int {
        val timestamp = now()
        return storage.transaction {
            val missing = offeredStarterTechniques(starterContent(timestamp).transitions)
            val videos = starterVideosFor(ItemKind.Transition, missing.mapTo(HashSet()) { it.id }, timestamp)
            storage.transitions.putMany(missing)
            storage.videos.putMany(videos)
            missing.size
        }
    }

    /**
     * Stops offering the starter techniques on offer now. One that goes missing later is still offered.
     * Returns how many were dismissed.
     */
    suspend fun dismissMissingStarterTechniques(): Int {
        val timestamp = now()
        return storage.transaction {
            val offered = offeredStarterTechniques(starterContent(timestamp).transitions)
            storage.starterDismissals.putMany(offered.map { StarterDismissal(it.id, timestamp) })
            offered.size
        }
    }

    /**
     * Adds the starter concepts on offer (missing and not dismissed), leaving everything stored untouched
     * except that each one is linked to the stored starter positions whose starter version lists it. A concept
     * the library never had also gets its starter video bookmark; one the user deleted does not, because
     * deleting kept that bookmark, unlinked. Returns how many concepts were added.
     */
    suspend fun addMissingStarterConcepts(): Int {
        val timestamp = now()
        val starter = starterContent(timestamp)
        return storage.transaction {
            val missing = offeredStarterConcepts(starter.concepts)
            val addedIds = missing.mapTo(HashSet()) { it.id }
            val videos = starterVideosFor(ItemKind.Concept, addedIds, timestamp)
            val linked = linkAddedConcepts(addedIds, starter.positions, storage.positions.all())
            storage.concepts.putMany(missing)
            storage.videos.putMany(videos)
            storage.positions.putMany(linked.map { it.copy(updatedAt = timestamp) })
            missing.size
        }
    }

    /**
     * Stops offering the starter concepts on offer now. One that goes missing later is still offered. Returns
     * how many were dismissed.
     */
    suspend fun dismissMissingStarterConcepts(): Int {
        val timestamp = now()
        return storage.transaction {
            val offered = offeredStarterConcepts(starterContent(timestamp).concepts)
            storage.starterDismissals.putMany(offered.map { StarterDismissal(it.id, timestamp) })
            offered.size
        }
    }

    /**
     * Deletes an item and everything that only makes sense alongside it: its review card, techniques touching
     * a deleted position, and references from positions to a deleted concept.
     */
    suspend fun deleteItem(kind: ItemKind, id: String) {
        storage.transaction {
            when (kind) {
                ItemKind.Position -> deletePosition(id)

                ItemKind.Transition -> {
                    storage.transitions.remove(id)
                    storage.unlinkFromTrainingLog(listOf(id))
                }

                ItemKind.Concept -> deleteConcept(id)
            }
            storage.reviews.remove(cardId(kind.cardKind, id))
            storage.unlinkVideos(kind, listOf(id))
        }
    }
}

/** Bookmarks outlive the item they were about: they are kept, with the link cleared. */
private suspend fun AppStorage.unlinkVideos(kind: ItemKind, ids: Collection<String>) {
    val linked = videos.all().filter { it.linkedKind == kind.linkedKind && it.linkedId in ids }
    videos.putMany(linked.map { it.copy(linkedKind = LinkedKind.None, linkedId = "") })
}

/** A class write-up outlives the techniques it mentions: the entry is kept, without the deleted ones. */
private suspend fun AppStorage.unlinkFromTrainingLog(techniqueIds: Collection<String>) {
    val mentioning = trainingLog.all().filter { entry -> entry.techniqueIds.any { it in techniqueIds } }
    trainingLog.putMany(mentioning.map { it.copy(techniqueIds = it.techniqueIds - techniqueIds.toSet()) })
}

/** Likewise for the places an entry says the student got stuck, when their position is deleted. */
private suspend fun AppStorage.unlinkSituationsFromTrainingLog(positionId: String) {
    val here = { id: String -> situationPositionId(id) == positionId }
    val mentioning = trainingLog.all().filter { stuckSituationIds(it).any(here) }
    trainingLog.putMany(mentioning.map { it.copy(stuckIds = stuckSituationIds(it).filterNot(here)) })
}
