package dev.justinsmith.bjj.core.testing

import dev.justinsmith.bjj.core.AppStorage
import dev.justinsmith.bjj.core.BackupData
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ReviewLogEntry
import dev.justinsmith.bjj.core.Setting
import dev.justinsmith.bjj.core.SettingId
import dev.justinsmith.bjj.core.StarterDismissal
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The behaviour every [AppStorage] must have. To add a storage engine, write a test class that extends this
 * with a way to make one; when its tests pass, the app will run on it.
 */
@Suppress("detekt.TooManyFunctions") // One promise per test, side by side.
abstract class StorageContract {
    /** Opens a storage on a database that does not exist yet. */
    protected abstract suspend fun create(initialData: BackupData): AppStorage

    /**
     * Opens the database the most recent [create] made, as an app restart would, or null for an engine that
     * keeps nothing.
     */
    protected open suspend fun reopen(initialData: BackupData): AppStorage? = null

    private val initialData = BackupData(
        concepts = listOf(concept(id = "c1", name = "One"), concept(id = "c2", name = "Two")),
        positions = listOf(position(id = "p1")),
    )

    private fun logEntry(reviewedAt: Long, id: Long? = null) =
        ReviewLogEntry(id = id, cardId = "concept:c1", grade = Grade.Good, reviewedAt = reviewedAt)

    private fun storageTest(block: suspend TestScope.(AppStorage) -> Unit) = runTest { block(create(initialData)) }

    /** Runs [write] and waits until the storage says something changed. */
    private suspend fun TestScope.awaitChange(storage: AppStorage, write: suspend () -> Unit) {
        // Listening before the write starts, so the report cannot slip past.
        val reported = async(start = CoroutineStart.UNDISPATCHED) { storage.changes.first() }
        write()
        reported.await()
    }

    @Test
    fun `starts with the initial data`() = storageTest { storage ->
        assertEquals(listOf("c1", "c2"), storage.concepts.all().map { it.id }.sorted())
        assertEquals(1, storage.positions.count())
        assertEquals(emptyList(), storage.videos.all())
    }

    @Test
    fun `gets and inserts and replaces and removes rows by id`() = storageTest { storage ->
        assertEquals("One", storage.concepts.get("c1")?.name)
        assertNull(storage.concepts.get("missing"))
        storage.concepts.put(concept(id = "c3", name = "Three"))
        storage.concepts.put(concept(id = "c1", name = "One, edited"))
        assertEquals(3, storage.concepts.count())
        assertEquals("One, edited", storage.concepts.get("c1")?.name)
        storage.concepts.remove("c1")
        storage.concepts.remove("never-existed")
        assertEquals(listOf("c2", "c3"), storage.concepts.all().map { it.id }.sorted())
    }

    @Test
    fun `has an empty starter dismissals table that reads and writes by id`() = storageTest { storage ->
        assertEquals(0, storage.starterDismissals.count())
        storage.starterDismissals.put(StarterDismissal("sweep", 5))
        assertEquals(listOf(StarterDismissal("sweep", 5)), storage.starterDismissals.all())
    }

    @Test
    fun `has an empty settings table that reads and writes by id`() = storageTest { storage ->
        assertEquals(0, storage.settings.count())
        storage.settings.put(Setting(SettingId.Theme, "dark", 5))
        storage.settings.put(Setting(SettingId.Theme, "light", 6))
        assertEquals(listOf(Setting(SettingId.Theme, "light", 6)), storage.settings.all())
    }

    @Test
    fun `has an empty training log table that reads and writes by id`() = storageTest { storage ->
        assertEquals(0, storage.trainingLog.count())
        val entry = trainingEntry(id = "class-1", techniqueIds = listOf("a", "b"), stuckIds = listOf("p1:top"))
        storage.trainingLog.put(entry)
        assertEquals(entry, storage.trainingLog.get("class-1"))
        storage.trainingLog.remove("class-1")
        assertEquals(emptyList(), storage.trainingLog.all())
    }

    @Test
    fun `stores a row whole with its lists and what it leaves out`() = storageTest { storage ->
        storage.concepts.put(concept(id = "c9", keyPoints = listOf("a", "b")))
        assertEquals(listOf("a", "b"), storage.concepts.get("c9")?.keyPoints)
        // A technique from before roles and drills existed comes back without them, not with defaults.
        val old = transition(id = "old")
        storage.transitions.put(old)
        assertEquals(old, storage.transitions.get("old"))
    }

    @Test
    fun `putMany inserts and replaces and clear empties one table only`() = storageTest { storage ->
        storage.concepts.putMany(listOf(concept(id = "c2", name = "Two, edited"), concept(id = "c4")))
        assertEquals(3, storage.concepts.count())
        assertEquals("Two, edited", storage.concepts.get("c2")?.name)
        storage.concepts.clear()
        assertEquals(0, storage.concepts.count())
        assertEquals(1, storage.positions.count())
    }

    @Test
    fun `the review log assigns increasing ids and reads strictly after a time and keeps restored ids`() =
        storageTest { storage ->
            storage.reviewLog.add(logEntry(100))
            storage.reviewLog.add(logEntry(200))
            val (first, second) = storage.reviewLog.all().map { assertNotNull(it.id) }
            assertTrue(second > first)
            assertEquals(listOf(200L), storage.reviewLog.since(100).map { it.reviewedAt })
            storage.reviewLog.clear()
            storage.reviewLog.putMany(listOf(logEntry(300, id = 41)))
            storage.reviewLog.add(logEntry(400))
            assertEquals(listOf<Long?>(41, 42), storage.reviewLog.all().map { it.id })
            assertEquals(2, storage.reviewLog.count())
        }

    @Test
    fun `a transaction stores all of its writes and returns the result`() = storageTest { storage ->
        val result = storage.transaction {
            storage.concepts.remove("c1")
            storage.positions.put(position(id = "p2"))
            storage.positions.count() + 40
        }
        assertEquals(42, result)
        assertNull(storage.concepts.get("c1"))
        assertNotNull(storage.positions.get("p2"))
    }

    @Test
    fun `a failing transaction undoes every write across tables and passes the error on`() = storageTest { storage ->
        val error = assertFailsWith<IllegalStateException> {
            storage.transaction {
                storage.concepts.clear()
                storage.positions.put(position(id = "p2"))
                storage.reviewLog.add(logEntry(1))
                error("boom")
            }
        }
        assertEquals("boom", error.message)
        assertEquals(2, storage.concepts.count())
        assertNull(storage.positions.get("p2"))
        assertEquals(0, storage.reviewLog.count())
    }

    @Test
    fun `a transaction inside a transaction is part of it`() = storageTest { storage ->
        assertFailsWith<IllegalStateException> {
            storage.transaction {
                storage.transaction { storage.concepts.put(concept(id = "inner")) }
                error("boom")
            }
        }
        assertNull(storage.concepts.get("inner"))
    }

    @Test
    fun `reports a change after a write and after a transaction`() = storageTest { storage ->
        awaitChange(storage) { storage.concepts.put(concept(id = "c5")) }
        awaitChange(storage) {
            storage.transaction {
                storage.concepts.remove("c5")
                storage.reviewLog.add(logEntry(1))
            }
        }
    }

    @Test
    fun `a transaction that only reads reports no change`() = storageTest { storage ->
        // Whoever reads again after each change reads in a transaction: reporting that one would never end.
        val reported = async(start = CoroutineStart.UNDISPATCHED) { storage.changes.first() }
        assertEquals(2, storage.transaction { storage.concepts.count() })
        testScheduler.runCurrent()
        assertFalse(reported.isCompleted)
        storage.transaction { storage.transaction { storage.concepts.put(concept(id = "c6")) } }
        reported.await()
    }

    @Test
    fun `reset returns to the initial data and reports it and leaves the storage usable`() = storageTest { storage ->
        storage.concepts.clear()
        storage.positions.put(position(id = "p2"))
        storage.reviewLog.add(logEntry(1))
        awaitChange(storage) { storage.reset() }
        assertEquals(listOf("c1", "c2"), storage.concepts.all().map { it.id }.sorted())
        assertEquals(1, storage.positions.count())
        assertEquals(0, storage.reviewLog.count())
        storage.concepts.put(concept(id = "c7"))
        assertEquals(3, storage.concepts.count())
    }

    @Test
    fun `keeps data across a restart and does not put the initial data back`() = runTest {
        val first = create(initialData)
        first.concepts.remove("c1")
        first.positions.put(position(id = "p2", name = "Added"))
        first.reviewLog.add(logEntry(100))
        // An engine that keeps nothing has nothing to check.
        val second = reopen(initialData) ?: return@runTest
        assertNull(second.concepts.get("c1"))
        assertNotNull(second.concepts.get("c2"))
        assertEquals("Added", second.positions.get("p2")?.name)
        assertEquals(1, second.reviewLog.count())
    }
}
