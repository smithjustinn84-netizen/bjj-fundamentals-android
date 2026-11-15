package dev.justinsmith.bjj.core

import kotlinx.coroutines.flow.Flow

// What the app needs from wherever its data is kept. The services are written against this and nothing else,
// so the engine behind it (Room today) can be swapped without touching them. An engine is right when it passes
// the storage contract in `core-testing`.

/** One table, read and written by id. */
interface Repository<T : HasId> {
    suspend fun all(): List<T>

    suspend fun get(id: String): T?

    suspend fun count(): Int

    /** Insert, or replace the row with the same id. */
    suspend fun put(row: T)

    suspend fun putMany(rows: List<T>)

    /** Removing an id that does not exist is not an error. */
    suspend fun remove(id: String)

    suspend fun clear()
}

/** The review history: append-only, with a numeric id assigned by the engine. */
interface ReviewLogRepository {
    suspend fun all(): List<ReviewLogEntry>

    /** Entries reviewed strictly after [timestamp]. */
    suspend fun since(timestamp: Long): List<ReviewLogEntry>

    suspend fun count(): Int

    /** Appends an entry under the next id; any id it carries is ignored. */
    suspend fun add(entry: ReviewLogEntry)

    /** Used when restoring a backup. Rows that carry an id keep it. */
    suspend fun putMany(rows: List<ReviewLogEntry>)

    suspend fun clear()
}

interface AppStorage {
    val concepts: Repository<Concept>
    val positions: Repository<Position>
    val transitions: Repository<Transition>
    val videos: Repository<VideoBookmark>
    val reviews: Repository<ReviewState>
    val reviewLog: ReviewLogRepository
    val starterDismissals: Repository<StarterDismissal>
    val trainingLog: Repository<TrainingEntry>
    val settings: Repository<Setting>

    /**
     * All or nothing: if [work] throws, every write it made is undone and the error passed on. Inside [work],
     * only call this storage.
     */
    suspend fun <T> transaction(work: suspend () -> T): T

    /** Emits after any committed change, once for a whole transaction: when to read again. */
    val changes: Flow<Unit>

    /** Delete everything and return to the data the storage was created with. */
    suspend fun reset()
}
