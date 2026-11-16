package dev.justinsmith.bjj.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.BackupException
import dev.justinsmith.bjj.core.BackupFile
import dev.justinsmith.bjj.core.BackupProblem
import dev.justinsmith.bjj.core.BackupTable
import dev.justinsmith.bjj.core.backupFilename
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.core.localDayKey
import dev.justinsmith.bjj.core.parseBackup
import dev.justinsmith.bjj.core.services.BackupService
import dev.justinsmith.bjj.core.services.ReviewService
import dev.justinsmith.bjj.ui.common.formatDay
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What just happened on the backup page. The screen puts it into words. */
sealed interface BackupMessage {
    data object Saved : BackupMessage

    data object Restored : BackupMessage

    data object ProgressReset : BackupMessage

    data object Erased : BackupMessage

    /** A picked file was not a backup this app can read; nothing was changed. */
    data class Rejected(val problem: BackupProblem) : BackupMessage
}

/** What the page has done and is waiting on. */
private data class BackupProgress(val message: BackupMessage? = null, val pending: BackupFile? = null)

data class BackupUiState(
    /** What just happened, to say at the top of the page; null when nothing has. */
    val message: BackupMessage? = null,
    /** A backup read and checked, waiting for the user to agree to replace everything with it. */
    val pending: BackupFile? = null,
    /** The day [pending] was made, in words; null when it does not say. */
    val pendingDay: String? = null,
    /** How many rows each table holds now; empty until what is stored has been read. */
    val counts: Map<BackupTable, Int> = emptyMap(),
) {
    val isError: Boolean get() = message is BackupMessage.Rejected
}

/** Export, import and starting over. Nothing is replaced until the user has agreed to it. */
class BackupViewModel(
    appData: AppData,
    private val backups: BackupService,
    private val reviews: ReviewService,
    private val clock: Clock,
) : ViewModel() {
    private val progress = MutableStateFlow(BackupProgress())

    val uiState: StateFlow<BackupUiState> = screenState(appData, progress) { snapshot, progress ->
        BackupUiState(
            message = progress.message,
            pending = progress.pending,
            pendingDay = progress.pending?.takeIf { it.exportedAt > 0 }?.let {
                formatDay(localDayKey(it.exportedAt, clock.zone))
            },
            counts = snapshot?.tableCounts.orEmpty(),
        )
    }

    private fun say(message: BackupMessage) {
        progress.value = BackupProgress(message)
    }

    /** Hands [save] the name and text of a backup of everything stored now. */
    fun export(save: (filename: String, text: String) -> Unit) {
        viewModelScope.launch {
            save(backupFilename(clock.now(), clock.zone), encodeBackup(backups.exportBackup()))
        }
    }

    fun exported(written: Boolean) {
        if (written) say(BackupMessage.Saved)
    }

    /** Checks the text of a picked file. A good one waits in [BackupUiState.pending]; nothing is changed yet. */
    fun picked(text: String?) {
        if (text == null) return
        try {
            progress.value = BackupProgress(pending = parseBackup(text))
        } catch (error: BackupException) {
            say(BackupMessage.Rejected(error.problem))
        }
    }

    fun cancelImport() {
        progress.update { it.copy(pending = null) }
    }

    /** Replaces everything stored with the backup that was waiting. */
    fun confirmImport() {
        val backup = progress.value.pending ?: return
        progress.update { it.copy(pending = null) }
        viewModelScope.launch {
            backups.importBackup(backup)
            say(BackupMessage.Restored)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            reviews.resetProgress()
            say(BackupMessage.ProgressReset)
        }
    }

    fun eraseAll() {
        viewModelScope.launch {
            backups.eraseAll()
            say(BackupMessage.Erased)
        }
    }
}
