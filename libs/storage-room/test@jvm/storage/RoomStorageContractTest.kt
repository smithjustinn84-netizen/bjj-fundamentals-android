package dev.justinsmith.bjj.storage

import dev.justinsmith.bjj.core.AppStorage
import dev.justinsmith.bjj.core.BackupData
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.core.parseBackup
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.core.testing.StorageContract
import dev.justinsmith.bjj.core.testing.TEST_NOW
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The engine over a real database file, with its queries on IO as in the app. */
class RoomStorageContractTest : StorageContract() {
    private val directory: File = Files.createTempDirectory("bjj-storage").toFile()
    private var open: RoomStorage? = null

    private fun open(initialData: () -> BackupData): RoomStorage {
        open?.close()
        return roomStorage(bjjDatabaseBuilder(directory.resolve(DATABASE_NAME).absolutePath), initialData).also {
            open = it
        }
    }

    private fun open(initialData: BackupData): RoomStorage = open { initialData }

    @AfterTest
    fun cleanUp() {
        open?.close()
        directory.deleteRecursively()
    }

    override suspend fun create(initialData: BackupData): AppStorage = open(initialData)

    override suspend fun reopen(initialData: BackupData): AppStorage = open(initialData)

    @Test
    fun `holds the starter content and gives it back unchanged through a backup`() = runTest {
        val starter = starterContent(TEST_NOW)
        val core = Core(CoreEnv(open(starter), newId = { "id" }, now = { TEST_NOW }, zone = LocalZone.Utc))
        val exported = core.backups.exportBackup()
        assertEquals(starter.concepts.sortedBy { it.id }, exported.data.concepts)
        assertEquals(starter.transitions.sortedBy { it.id }, exported.data.transitions)
        assertEquals(starter.videos.size, exported.data.videos.size)
        core.backups.importBackup(parseBackup(encodeBackup(exported)))
        assertEquals(exported.data, core.backups.exportBackup().data)
    }

    @Test
    fun `builds the initial data only for a new database`() = runTest {
        var built = 0
        val starter = { starterContent(TEST_NOW).also { built++ } }
        open(starter).concepts.count()
        open(starter).concepts.count()
        assertEquals(1, built)
    }
}
