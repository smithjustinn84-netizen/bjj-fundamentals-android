package dev.justinsmith.bjj.core.services

import dev.justinsmith.bjj.core.AppStorage
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.ReviewLogEntry
import dev.justinsmith.bjj.core.ReviewLogRepository
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.core.parseBackup
import dev.justinsmith.bjj.core.testing.testCore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BackupServiceTest {
    private val base = StudyItem(CardKind.Concept, "base")

    @Test
    fun `export then wipe then import restores everything through the file`() = runTest {
        val (core, storage) = testCore().let { it.core to it.storage }
        core.videos.add(VideoInput("Scissor sweep", "dQw4w9WgXcQ", 95, LinkedKind.Transition, "scissor-sweep", ""))
        core.reviews.recordReview(base, Grade.Good)
        core.items.deleteItem(ItemKind.Transition, "triangle-choke")
        core.items.dismissMissingStarterTechniques()
        core.settings.setTheme(ThemeChoice.Dark)
        val before = core.backups.tableCounts()
        val text = encodeBackup(core.backups.exportBackup())
        storage.videos.clear()
        storage.starterDismissals.clear()
        core.reviews.resetProgress()
        core.items.deleteItem(ItemKind.Position, "mount")
        core.backups.eraseAll()
        assertEquals(ThemeChoice.Device, core.queries.themeChoice())
        core.backups.importBackup(parseBackup(text))
        assertEquals(before, core.backups.tableCounts())
        assertEquals(1, storage.reviews.get("concept:base")?.intervalDays)
        assertNotNull(storage.positions.get("mount"))
        assertNotNull(storage.starterDismissals.get("triangle-choke"))
        assertEquals(ThemeChoice.Dark, core.queries.themeChoice())
    }

    @Test
    fun `a fresh export of the starter content can always be imported again`() = runTest {
        val exported = testCore().core.backups.exportBackup()
        assertEquals(exported, parseBackup(encodeBackup(exported)))
    }

    @Test
    fun `import removes rows that are not in the backup`() = runTest {
        val (core, storage) = testCore().let { it.core to it.storage }
        val backup = core.backups.exportBackup()
        val addedLater = core.items.saveConcept(null, ConceptInput("Later", "", emptyList(), "", 50, ""))
        core.backups.importBackup(backup)
        assertNull(storage.concepts.get(addedLater))
    }

    @Test
    fun `leaves everything untouched when an import fails part-way`() = runTest {
        // The review log is written after six tables have already been replaced.
        val core = testCore { storage ->
            object : AppStorage by storage {
                override val reviewLog = object : ReviewLogRepository by storage.reviewLog {
                    override suspend fun putMany(rows: List<ReviewLogEntry>) = error("disk full")
                }
            }
        }.core
        val before = core.backups.tableCounts()
        val backup = core.backups.exportBackup()
        val emptied = backup.copy(data = backup.data.copy(positions = emptyList()))
        assertEquals("disk full", assertFailsWith<IllegalStateException> { core.backups.importBackup(emptied) }.message)
        assertEquals(before, core.backups.tableCounts())
    }

    @Test
    fun `erasing everything returns to the starter content`() = runTest {
        val (core, storage) = testCore().let { it.core to it.storage }
        val before = core.backups.tableCounts()
        core.items.deleteItem(ItemKind.Position, "mount")
        core.reviews.recordReview(base, Grade.Good)
        core.backups.eraseAll()
        assertEquals(before, core.backups.tableCounts())
        assertNotNull(storage.positions.get("mount"))
    }
}
