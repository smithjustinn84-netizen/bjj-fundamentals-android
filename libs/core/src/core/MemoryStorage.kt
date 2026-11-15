package dev.justinsmith.bjj.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A complete [AppStorage] that keeps everything in memory. Used by the tests (no database needed) and as the
 * simplest reference for anyone writing a new engine. Not for the app: nothing is kept, it is for one thread,
 * and overlapping transactions are not isolated from each other.
 */
class MemoryStorage(private val initialData: BackupData) : AppStorage {
    // Rows are immutable, so holding and handing out the same ones shares nothing a caller could change.
    private var data = initialData
    private var nextLogId = firstFreeLogId(initialData)
    private var openTransactions = 0
    private val changed =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val changes: Flow<Unit> = changed

    private fun committed() {
        // Inside a transaction nothing is committed yet; `transaction` reports once at the end.
        if (openTransactions == 0) changed.tryEmit(Unit)
    }

    private inner class Table<T : HasId>(
        private val read: (BackupData) -> List<T>,
        private val write: BackupData.(List<T>) -> BackupData,
    ) : Repository<T> {
        private fun update(change: (List<T>) -> List<T>) {
            data = data.write(change(read(data)))
            committed()
        }

        override suspend fun all(): List<T> = read(data)

        override suspend fun get(id: String): T? = read(data).firstOrNull { it.id == id }

        override suspend fun count(): Int = read(data).size

        override suspend fun put(row: T) = putMany(listOf(row))

        override suspend fun putMany(rows: List<T>) = update { stored ->
            // A replaced row keeps its place; a new one goes to the end.
            val incoming = rows.associateByTo(LinkedHashMap()) { it.id }
            stored.map { incoming.remove(it.id) ?: it } + incoming.values
        }

        override suspend fun remove(id: String) = update { stored -> stored.filter { it.id != id } }

        override suspend fun clear() = update { emptyList() }
    }

    override val concepts: Repository<Concept> = Table({ it.concepts }) { copy(concepts = it) }
    override val positions: Repository<Position> = Table({ it.positions }) { copy(positions = it) }
    override val transitions: Repository<Transition> = Table({ it.transitions }) { copy(transitions = it) }
    override val videos: Repository<VideoBookmark> = Table({ it.videos }) { copy(videos = it) }
    override val reviews: Repository<ReviewState> = Table({ it.reviews }) { copy(reviews = it) }
    override val starterDismissals: Repository<StarterDismissal> =
        Table({ it.starterDismissals }) { copy(starterDismissals = it) }
    override val trainingLog: Repository<TrainingEntry> = Table({ it.trainingLog }) { copy(trainingLog = it) }
    override val settings: Repository<Setting> = Table({ it.settings }) { copy(settings = it) }

    override val reviewLog: ReviewLogRepository = object : ReviewLogRepository {
        private fun append(entries: List<ReviewLogEntry>) {
            val stored = entries.map { entry ->
                val id = entry.id ?: nextLogId
                nextLogId = maxOf(nextLogId, id + 1)
                entry.copy(id = id)
            }
            data = data.copy(reviewLog = data.reviewLog + stored)
            committed()
        }

        override suspend fun all(): List<ReviewLogEntry> = data.reviewLog

        override suspend fun since(timestamp: Long) = data.reviewLog.filter { it.reviewedAt > timestamp }

        override suspend fun count(): Int = data.reviewLog.size

        override suspend fun add(entry: ReviewLogEntry) = append(listOf(entry.copy(id = null)))

        override suspend fun putMany(rows: List<ReviewLogEntry>) = append(rows)

        override suspend fun clear() {
            data = data.copy(reviewLog = emptyList())
            committed()
        }
    }

    override suspend fun <T> transaction(work: suspend () -> T): T {
        // Only the outermost transaction holds a snapshot; it undoes everything, nested writes included.
        val before = if (openTransactions == 0) data to nextLogId else null
        openTransactions++
        try {
            return work().also {
                openTransactions--
                committed()
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) { // Whatever went wrong, undo and pass it on.
            openTransactions--
            if (before != null) {
                data = before.first
                nextLogId = before.second
            }
            throw error
        }
    }

    override suspend fun reset() {
        data = initialData
        nextLogId = firstFreeLogId(initialData)
        committed()
    }
}

private fun firstFreeLogId(data: BackupData): Long = (data.reviewLog.maxOfOrNull { it.id ?: 0 } ?: 0) + 1
