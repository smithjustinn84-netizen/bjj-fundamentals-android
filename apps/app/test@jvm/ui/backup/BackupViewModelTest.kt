package dev.justinsmith.bjj.ui.backup

import dev.justinsmith.bjj.core.BackupProblem
import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.ui.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The backup page's ViewModel over the starter content. */
class BackupViewModelTest : ViewModelTest() {
    @Test
    fun `a backup is checked on picking and replaces nothing until confirmed`() = runTest {
        val backup = BackupViewModel(appData, t.core.backups, t.core.reviews, clock)
        val state = watch(backup.uiState)
        assertTrue(checkNotNull(state().counts[BackupTable.Concepts]) > 0)
        var text = ""
        backup.export { name, exported ->
            assertEquals("bjj-fundamentals-backup-2026-11-14.json", name)
            text = exported
        }
        backup.picked("{ not a backup")
        assertEquals(BackupMessage.Rejected(BackupProblem.NotJson), state().message)
        assertTrue(state().isError)

        t.storage.concepts.clear()
        backup.picked(text)
        assertEquals(encodeBackup(checkNotNull(state().pending)), text)
        assertEquals("Sat 14 Nov 2026", state().pendingDay)
        assertEquals(0, t.storage.concepts.count())
        backup.cancelImport()
        assertNull(state().pending)
        assertNull(state().pendingDay)
        backup.picked(text)
        backup.confirmImport()
        assertTrue(t.storage.concepts.count() > 0)
        assertEquals(BackupMessage.Restored, state().message)
        assertFalse(state().isError)
    }
}
