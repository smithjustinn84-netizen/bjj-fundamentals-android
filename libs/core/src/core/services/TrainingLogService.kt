package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.isDayKey
import dev.justinsmith.bjj.core.situationPositionId
import dev.justinsmith.bjj.core.situationRole

data class TrainingEntryInput(
    val day: String,
    val minutes: Int,
    val taught: String,
    val worked: String,
    val struggled: String,
    val focus: String,
    val techniqueIds: List<String>,
    val stuckIds: List<String>,
)

/** Writing up classes: what was taught, how rolling went, and what to work on next. */
class TrainingLogService(env: CoreEnv) {
    private val storage = env.storage
    private val newId = env.newId
    private val now = env.now

    /**
     * Creates the entry when [id] is null, otherwise replaces it keeping its creation time. Returns its id.
     * Text is trimmed, minutes are at least zero, and only techniques that exist and situations whose position
     * exists are kept. Throws [IllegalArgumentException] for a day that is not one.
     */
    suspend fun save(id: String?, input: TrainingEntryInput): String {
        require(isDayKey(input.day)) { "\"${input.day}\" is not a day like 2026-11-14." }
        val timestamp = now()
        val entryId = id ?: newId()
        return storage.transaction {
            val existing = id?.let { storage.trainingLog.get(it) }
            val known = storage.transitions.all().mapTo(HashSet()) { it.id }
            val positions = storage.positions.all().mapTo(HashSet()) { it.id }
            storage.trainingLog.put(
                TrainingEntry(
                    id = entryId,
                    day = input.day,
                    minutes = maxOf(0, input.minutes),
                    taught = input.taught.trim(),
                    worked = input.worked.trim(),
                    struggled = input.struggled.trim(),
                    focus = input.focus.trim(),
                    techniqueIds = input.techniqueIds.distinct().filter { it in known },
                    stuckIds = input.stuckIds.distinct().filter {
                        situationRole(it) != null && situationPositionId(it) in positions
                    },
                    createdAt = existing?.createdAt ?: timestamp,
                    updatedAt = timestamp,
                ),
            )
            entryId
        }
    }

    suspend fun remove(id: String) = storage.trainingLog.remove(id)
}
