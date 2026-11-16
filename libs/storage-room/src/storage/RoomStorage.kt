package dev.justinsmith.bjj.storage

import androidx.room.RoomDatabase
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dev.justinsmith.bjj.core.AppStorage
import dev.justinsmith.bjj.core.BackupData
import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.CoreJson
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.HasId
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.Repository
import dev.justinsmith.bjj.core.ReviewLogEntry
import dev.justinsmith.bjj.core.ReviewLogRepository
import dev.justinsmith.bjj.core.ReviewState
import dev.justinsmith.bjj.core.Setting
import dev.justinsmith.bjj.core.StarterDismissal
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.VideoBookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Opens the app's storage on the database [builder] names: the bundled SQLite, its queries on IO. [initialData]
 * is inserted once, as part of creating the database, and again only by [AppStorage.reset].
 */
fun roomStorage(builder: RoomDatabase.Builder<BjjDatabase>, initialData: BackupData): RoomStorage {
    val seed = SeedRows(initialData)
    val database = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addCallback(
            object : RoomDatabase.Callback() {
                // Room creates the tables and calls this in one transaction, so a database either has its
                // starter content or does not exist yet.
                override fun onCreate(connection: SQLiteConnection) = seed.insertInto(connection)
            },
        )
        .build()
    return RoomStorage(database, seed)
}

/** The initial data as the rows the database holds. */
internal class SeedRows(data: BackupData) {
    val rows: List<RowEntity> =
        table(BackupTable.Concepts, Concept.serializer(), data.concepts) +
            table(BackupTable.Positions, Position.serializer(), data.positions) +
            table(BackupTable.Transitions, Transition.serializer(), data.transitions) +
            table(BackupTable.Videos, VideoBookmark.serializer(), data.videos) +
            table(BackupTable.Reviews, ReviewState.serializer(), data.reviews) +
            table(BackupTable.StarterDismissals, StarterDismissal.serializer(), data.starterDismissals) +
            table(BackupTable.TrainingLog, TrainingEntry.serializer(), data.trainingLog) +
            table(BackupTable.Settings, Setting.serializer(), data.settings)
    val reviewLog: List<ReviewLogEntity> = data.reviewLog.map { it.entity() }

    private fun <T : HasId> table(table: BackupTable, serializer: KSerializer<T>, rows: List<T>) =
        rows.map { RowEntity(table.key, it.id, CoreJson.encodeToString(serializer, it)) }

    /** For the moment of creation, when there is only a bare connection to write through. */
    fun insertInto(connection: SQLiteConnection) {
        connection.prepare("INSERT INTO rows (tbl, id, json) VALUES (?, ?, ?)").use { insert ->
            for (row in rows) {
                insert.bindText(TBL, row.tbl)
                insert.bindText(ID, row.id)
                insert.bindText(JSON, row.json)
                insert.step()
                insert.reset()
            }
        }
        val log = "INSERT INTO review_log (id, card_id, grade, reviewed_at) VALUES (nullif(?, 0), ?, ?, ?)"
        connection.prepare(log).use { insert ->
            for (entry in reviewLog) {
                insert.bindLong(LOG_ID, entry.id)
                insert.bindText(LOG_CARD, entry.cardId)
                insert.bindText(LOG_GRADE, entry.grade)
                insert.bindLong(LOG_AT, entry.reviewedAt)
                insert.step()
                insert.reset()
            }
        }
    }

    // Where each value goes in the statements above, counting from 1.
    private companion object {
        const val TBL = 1
        const val ID = 2
        const val JSON = 3
        const val LOG_ID = 1
        const val LOG_CARD = 2
        const val LOG_GRADE = 3
        const val LOG_AT = 4
    }
}

/** An id of 0 asks the database for the next one. */
private fun ReviewLogEntry.entity() =
    ReviewLogEntity(id = id ?: 0, cardId = cardId, grade = grade.key, reviewedAt = reviewedAt)

private fun ReviewLogEntity.entry() = ReviewLogEntry(
    id = id,
    cardId = cardId,
    // Only this engine writes the column, and only from a Grade.
    grade = Grade.entries.first { it.key == grade },
    reviewedAt = reviewedAt,
)

/**
 * Marks a coroutine as running inside [RoomStorage.transaction], so a nested one joins it and reports nothing,
 * and notes whether anything was written: a transaction that only read has no change to report.
 */
private class InTransaction : AbstractCoroutineContextElement(Key) {
    @Volatile
    var wrote = false

    companion object Key : CoroutineContext.Key<InTransaction>
}

/**
 * [AppStorage] over Room. Every row is a document: its table's name, its id and the row as JSON, the same JSON
 * a backup holds. So a new field on a model changes nothing here, and the engine knows nothing about BJJ.
 */
class RoomStorage internal constructor(private val database: BjjDatabase, private val seed: SeedRows) : AppStorage {
    private val rows = database.rows()
    private val log = database.reviewLog()
    private val changed =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // Changes this process commits. Nothing else writes to the database, so there is nothing else to watch.
    override val changes: Flow<Unit> = changed

    /** A write on its own is committed when it returns; inside a transaction the transaction reports for it. */
    private suspend fun committed() {
        val transaction = currentCoroutineContext()[InTransaction]
        if (transaction == null) changed.tryEmit(Unit) else transaction.wrote = true
    }

    private inner class Table<T : HasId>(table: BackupTable, private val serializer: KSerializer<T>) : Repository<T> {
        private val tbl = table.key

        private fun RowEntity.row(): T = CoreJson.decodeFromString(serializer, json)

        override suspend fun all(): List<T> = rows.all(tbl).map { it.row() }

        override suspend fun get(id: String): T? = rows.get(tbl, id)?.row()

        override suspend fun count(): Int = rows.count(tbl)

        override suspend fun put(row: T) = putMany(listOf(row))

        override suspend fun putMany(rows: List<T>) {
            this@RoomStorage.rows.put(rows.map { RowEntity(tbl, it.id, CoreJson.encodeToString(serializer, it)) })
            committed()
        }

        override suspend fun remove(id: String) {
            rows.remove(tbl, id)
            committed()
        }

        override suspend fun clear() {
            rows.clear(tbl)
            committed()
        }
    }

    override val concepts: Repository<Concept> = Table(BackupTable.Concepts, Concept.serializer())
    override val positions: Repository<Position> = Table(BackupTable.Positions, Position.serializer())
    override val transitions: Repository<Transition> = Table(BackupTable.Transitions, Transition.serializer())
    override val videos: Repository<VideoBookmark> = Table(BackupTable.Videos, VideoBookmark.serializer())
    override val reviews: Repository<ReviewState> = Table(BackupTable.Reviews, ReviewState.serializer())
    override val starterDismissals: Repository<StarterDismissal> =
        Table(BackupTable.StarterDismissals, StarterDismissal.serializer())
    override val trainingLog: Repository<TrainingEntry> = Table(BackupTable.TrainingLog, TrainingEntry.serializer())
    override val settings: Repository<Setting> = Table(BackupTable.Settings, Setting.serializer())

    override val reviewLog: ReviewLogRepository = object : ReviewLogRepository {
        override suspend fun all(): List<ReviewLogEntry> = log.all().map { it.entry() }

        override suspend fun since(timestamp: Long): List<ReviewLogEntry> = log.since(timestamp).map { it.entry() }

        override suspend fun count(): Int = log.count()

        override suspend fun add(entry: ReviewLogEntry) = putMany(listOf(entry.copy(id = null)))

        override suspend fun putMany(rows: List<ReviewLogEntry>) {
            log.put(rows.map { it.entity() })
            committed()
        }

        override suspend fun clear() {
            log.clear()
            committed()
        }
    }

    override suspend fun <T> transaction(work: suspend () -> T): T {
        if (currentCoroutineContext()[InTransaction] != null) return work()
        val transaction = InTransaction()
        // Every DAO call made inside uses this connection, so they commit or roll back together.
        val result = database.useWriterConnection { connection ->
            connection.immediateTransaction { withContext(transaction) { work() } }
        }
        if (transaction.wrote) changed.tryEmit(Unit)
        return result
    }

    override suspend fun reset() = transaction {
        rows.clearAll()
        log.clear()
        rows.put(seed.rows)
        log.put(seed.reviewLog)
        committed()
    }

    /** Closes the database. For a test reopening it; the app keeps its storage for as long as it runs. */
    fun close() = database.close()
}
