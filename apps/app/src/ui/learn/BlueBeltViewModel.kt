package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.blueBeltTestList
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.ItemService
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A technique on the test list. [from] is the name of where it starts, or null when that position is gone. */
data class BeltTestRow(val id: String, val name: String, val progress: Progress, val from: String?)

/** The listed techniques of one type. Never empty. */
data class BeltTestGroupUi(val type: TransitionType, val techniques: List<BeltTestRow>)

sealed interface BlueBeltUiState {
    data object Loading : BlueBeltUiState

    data class Ready(
        /** One group per technique type that has something on the list. */
        val groups: List<BeltTestGroupUi>,
        val learned: Int,
        val total: Int,
        /** Names of starter techniques the library does not hold and has not dismissed. */
        val missingStarter: List<String>,
    ) : BlueBeltUiState
}

fun blueBeltUiState(snapshot: Snapshot): BlueBeltUiState.Ready {
    val lib = snapshot.library
    val list = blueBeltTestList(lib)
    return BlueBeltUiState.Ready(
        groups = list.groups.map { group ->
            BeltTestGroupUi(
                group.type,
                group.techniques.map {
                    BeltTestRow(
                        id = it.id,
                        name = it.name,
                        progress = progressOf(reviewOf(lib, CardKind.Transition, it.id)),
                        from = lib.positionById[it.fromId]?.name,
                    )
                },
            )
        },
        learned = list.learned,
        total = list.total,
        missingStarter = snapshot.missingStarterTechniques.map { it.name },
    )
}

class BlueBeltViewModel(appData: AppData, private val items: ItemService) : ViewModel() {
    val uiState: StateFlow<BlueBeltUiState> = screenState(appData) { snapshot ->
        snapshot?.let(::blueBeltUiState) ?: BlueBeltUiState.Loading
    }

    fun addStarter() {
        viewModelScope.launch { items.addMissingStarterTechniques() }
    }

    fun dismissStarter() {
        viewModelScope.launch { items.dismissMissingStarterTechniques() }
    }
}
