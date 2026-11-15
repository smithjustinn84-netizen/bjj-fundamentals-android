package dev.justinsmith.bjj.storage

import androidx.room.ColumnInfo
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

// NEVER edit a committed schema (schemas/…/N.json) or what an existing version means: a database out in the
// world is at that version. A change to a table is a new version with a migration. A new field on a model is
// NOT a change here: every row is a document, stored whole as JSON, so the tables only change when the engine
// itself must do something new.

/** One stored thing of any kind: a document, keyed by the table it belongs to and its id. */
@Entity(tableName = "rows", primaryKeys = ["tbl", "id"])
internal data class RowEntity(
    val tbl: String,
    val id: String,
    val json: String,
)

/** One review, in the order they happened. The id is the engine's; a restored row keeps the one it had. */
@Entity(tableName = "review_log", indices = [Index("reviewed_at")])
internal data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "card_id") val cardId: String,
    val grade: String,
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long,
)

@Dao
internal interface RowDao {
    // By id, the order the web app's IndexedDB hands rows back in, so a tie in a sort breaks the same way.
    @Query("SELECT * FROM rows WHERE tbl = :tbl ORDER BY id")
    suspend fun all(tbl: String): List<RowEntity>

    @Query("SELECT * FROM rows WHERE tbl = :tbl AND id = :id")
    suspend fun get(tbl: String, id: String): RowEntity?

    @Query("SELECT COUNT(*) FROM rows WHERE tbl = :tbl")
    suspend fun count(tbl: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(rows: List<RowEntity>)

    @Query("DELETE FROM rows WHERE tbl = :tbl AND id = :id")
    suspend fun remove(tbl: String, id: String)

    @Query("DELETE FROM rows WHERE tbl = :tbl")
    suspend fun clear(tbl: String)

    @Query("DELETE FROM rows")
    suspend fun clearAll()
}

@Dao
internal interface ReviewLogDao {
    @Query("SELECT * FROM review_log ORDER BY id")
    suspend fun all(): List<ReviewLogEntity>

    @Query("SELECT * FROM review_log WHERE reviewed_at > :timestamp ORDER BY id")
    suspend fun since(timestamp: Long): List<ReviewLogEntity>

    @Query("SELECT COUNT(*) FROM review_log")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(rows: List<ReviewLogEntity>)

    @Query("DELETE FROM review_log")
    suspend fun clear()
}

@Database(entities = [RowEntity::class, ReviewLogEntity::class], version = 1)
@ConstructedBy(BjjDatabaseConstructor::class)
abstract class BjjDatabase : RoomDatabase() {
    internal abstract fun rows(): RowDao

    internal abstract fun reviewLog(): ReviewLogDao
}

/** Room generates the `actual` for each platform. */
@Suppress("KotlinNoActualForExpect")
expect object BjjDatabaseConstructor : RoomDatabaseConstructor<BjjDatabase> {
    override fun initialize(): BjjDatabase
}

/** The file the app keeps its database in. */
const val DATABASE_NAME = "bjj.db"
