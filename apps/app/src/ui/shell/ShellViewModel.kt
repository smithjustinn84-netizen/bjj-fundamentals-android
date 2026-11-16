package dev.justinsmith.bjj.ui.shell

import androidx.lifecycle.ViewModel
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.services.studyStats
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.StateFlow

/** What the frame round every page shows: the colours, and the count on the Review tab. */
data class ShellUiState(
    /** Until the stored choice is read the device decides, which is also what a new library gets. */
    val themeChoice: ThemeChoice = ThemeChoice.Device,
    val dueCount: Int = 0,
)

class ShellViewModel(appData: AppData, private val clock: Clock) : ViewModel() {
    val uiState: StateFlow<ShellUiState> = screenState(appData) { snapshot ->
        snapshot?.let { ShellUiState(it.themeChoice, studyStats(it, clock.now(), clock.zone).dueCount) }
            ?: ShellUiState()
    }
}
