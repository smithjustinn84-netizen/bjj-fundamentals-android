package dev.justinsmith.bjj.ui.log

import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import dev.justinsmith.bjj.ui.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/** The training log's ViewModel over the starter content. */
class LogViewModelTest : ViewModelTest() {
    @Test
    fun `the log saves an entry once and says so when a save fails`() = runTest {
        val log = LogViewModel(appData, t.core.trainingLog, clock)
        val state = watch(log.uiState)
        val entry = TrainingEntryInput("2026-11-14", 60, "", "", "", "", emptyList(), listOf("mount:bottom"))
        assertEquals("2026-11-14", assertIs<LogUiState.Ready>(state()).today)
        log.save(null, entry)
        val saved = assertIs<LogUiState.Ready>(state())
        assertEquals(1, saved.writes.saved)
        assertEquals(1, saved.entries.size)
        assertEquals(listOf("mount:bottom"), saved.entries.single().stuck.map { it.id })
        assertEquals(listOf("mount:bottom"), saved.choices.recentlyStuck)
        assertEquals(1, t.storage.trainingLog.count())

        log.save(null, entry.copy(day = "yesterday"))
        val failed = assertIs<LogUiState.Ready>(state())
        assertEquals(1, failed.writes.saved)
        assertEquals(LogError.Save, failed.writes.error)
        assertFalse(failed.writes.saving)
    }
}
