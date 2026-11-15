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

data class LogUiState(
    /** True while an entry is being stored: a second tap must not write a second entry. */
    val saving: Boolean = false,
    /** What went wrong with the last save or delete, to say on the page; empty when nothing did. */
    val error: String = "",
)

/** Saves and deletes for the training log. */
class LogViewModel(private val core: Core) : ViewModel() {
    private val _state = MutableStateFlow(LogUiState())
    val state: StateFlow<LogUiState> = _state.asStateFlow()

    fun save(id: String?, input: TrainingEntryInput, onSaved: () -> Unit) {
        if (_state.value.saving) return
        _state.value = LogUiState(saving = true)
        attempt("That did not save. Your entry is still in the form: try again.") {
            core.trainingLog.save(id, input)
            onSaved()
        }
    }

    fun remove(id: String) = attempt("That entry could not be deleted. Try again.") { core.trainingLog.remove(id) }

    private fun attempt(failure: String, write: suspend () -> Unit) {
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
