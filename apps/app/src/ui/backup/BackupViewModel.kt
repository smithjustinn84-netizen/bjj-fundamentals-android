package dev.justinsmith.bjj.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.core.BackupException
import dev.justinsmith.bjj.core.BackupFile
import dev.justinsmith.bjj.core.BackupProblem
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.backupFilename
import dev.justinsmith.bjj.core.encodeBackup
import dev.justinsmith.bjj.core.parseBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class BackupUiState(
    /** What just happened, to say at the top of the page; null when nothing has. */
    val message: BackupMessage? = null,
    /** A backup read and checked, waiting for the user to agree to replace everything with it. */
    val pending: BackupFile? = null,
) {
    val isError: Boolean get() = message is BackupMessage.Rejected
}

/** Export, import and starting over. Nothing is replaced until the user has agreed to it. */
class BackupViewModel(private val core: Core, private val env: CoreEnv) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private fun say(message: BackupMessage) {
        _state.value = BackupUiState(message)
    }

    /** Hands [save] the name and text of a backup of everything stored now. */
    fun export(save: (filename: String, text: String) -> Unit) {
        viewModelScope.launch { save(backupFilename(env.now(), env.zone), encodeBackup(core.backups.exportBackup())) }
    }

    fun exported(written: Boolean) {
        if (written) say(BackupMessage.Saved)
    }

    /** Checks the text of a picked file. A good one waits in [BackupUiState.pending]; nothing is changed yet. */
    fun picked(text: String?) {
        if (text == null) return
        try {
            _state.value = BackupUiState(pending = parseBackup(text))
        } catch (error: BackupException) {
            say(BackupMessage.Rejected(error.problem))
        }
    }

    fun cancelImport() {
        _state.update { it.copy(pending = null) }
    }

    /** Replaces everything stored with the backup that was waiting. */
    fun confirmImport() {
        val backup = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }
        viewModelScope.launch {
            core.backups.importBackup(backup)
            say(BackupMessage.Restored)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            core.reviews.resetProgress()
            say(BackupMessage.ProgressReset)
        }
    }

    fun eraseAll() {
        viewModelScope.launch {
            core.backups.eraseAll()
            say(BackupMessage.Erased)
        }
    }
}
