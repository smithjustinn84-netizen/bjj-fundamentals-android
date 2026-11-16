package dev.justinsmith.bjj.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.services.SettingsService
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface MoreUiState {
    data object Loading : MoreUiState

    data class Ready(val videoCount: Int, val theme: ThemeChoice) : MoreUiState
}

class MoreViewModel(appData: AppData, private val settings: SettingsService) : ViewModel() {
    val uiState: StateFlow<MoreUiState> = screenState(appData) { snapshot ->
        snapshot?.let { MoreUiState.Ready(it.videos.size, it.themeChoice) } ?: MoreUiState.Loading
    }

    fun setTheme(choice: ThemeChoice) {
        viewModelScope.launch { settings.setTheme(choice) }
    }
}
