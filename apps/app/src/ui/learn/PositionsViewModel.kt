package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.positionProgress
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.SearchUi
import dev.justinsmith.bjj.ui.common.TechniqueRow
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.screenState
import dev.justinsmith.bjj.ui.common.searchUi
import dev.justinsmith.bjj.ui.common.techniqueRows
import dev.justinsmith.bjj.ui.map.MapUi
import dev.justinsmith.bjj.ui.map.mapUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A position in the list: how well it is known, and how many of the techniques from it are learned. */
data class PositionTile(val id: String, val name: String, val progress: Progress, val learned: Int, val total: Int)

/** The positions of one category, in learning order. Never empty. */
data class PositionGroup(val category: PositionCategory, val positions: List<PositionTile>)

sealed interface PositionsUiState {
    data object Loading : PositionsUiState

    data class Ready(
        /** One group per category that has a position; empty when the library has none. */
        val groups: List<PositionGroup>,
        val map: MapUi,
        /** Techniques whose starting position is gone. */
        val orphans: List<TechniqueRow>,
        val search: SearchUi,
    ) : PositionsUiState
}

fun positionsUiState(snapshot: Snapshot, query: String): PositionsUiState.Ready {
    val lib = snapshot.library
    val tiles = lib.positions.associate { position ->
        val progress = positionProgress(lib, position.id)
        position.id to PositionTile(
            id = position.id,
            name = position.name,
            progress = progressOf(reviewOf(lib, CardKind.Position, position.id)),
            learned = progress.learned,
            total = progress.total,
        )
    }
    return PositionsUiState.Ready(
        groups = PositionCategory.entries.mapNotNull { category ->
            val inCategory = lib.positions.filter { it.category == category }.map { tiles.getValue(it.id) }
            if (inCategory.isEmpty()) null else PositionGroup(category, inCategory)
        },
        map = mapUi(lib),
        orphans = techniqueRows(lib, lib.transitions.filter { it.fromId !in lib.positionById }, Direction.Out),
        search = searchUi(lib, query),
    )
}

class PositionsViewModel(appData: AppData) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<PositionsUiState> = screenState(appData, query) { snapshot, query ->
        snapshot?.let { positionsUiState(it, query) } ?: PositionsUiState.Loading
    }

    fun search(text: String) {
        query.value = text
    }
}
