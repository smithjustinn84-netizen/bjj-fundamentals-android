package dev.justinsmith.bjj.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A write to the log that failed. */
enum class LogError { Save, Remove }

data class LogUiState(
    /** True while an entry is being stored: a second tap must not write a second entry. */
    val saving: Boolean = false,
    /** What went wrong with the last save or delete, to say on the page; null when nothing did. */
    val error: LogError? = null,
)

/** Saves and deletes for the training log. */
class LogViewModel(private val core: Core) : ViewModel() {
    private val _state = MutableStateFlow(LogUiState())
    val state: StateFlow<LogUiState> = _state.asStateFlow()

    fun save(id: String?, input: TrainingEntryInput, onSaved: () -> Unit) {
        if (_state.value.saving) return
        _state.value = LogUiState(saving = true)
        attempt(LogError.Save) {
            core.trainingLog.save(id, input)
            onSaved()
        }
    }

    fun remove(id: String) = attempt(LogError.Remove) { core.trainingLog.remove(id) }

    private fun attempt(failure: LogError, write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                _state.value = LogUiState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) { // Whatever was thrown, say so.
                _state.update { LogUiState(error = failure) }
            }
        }
    }
}
