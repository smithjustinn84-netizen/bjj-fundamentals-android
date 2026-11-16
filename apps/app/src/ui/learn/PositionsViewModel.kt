package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.positionOptions
import dev.justinsmith.bjj.core.positionProgress
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.linkedVideos
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.ui.common.Direction
import dev.justinsmith.bjj.ui.common.Named
import dev.justinsmith.bjj.ui.common.SearchUi
import dev.justinsmith.bjj.ui.common.TechniqueRow
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoCommands
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

sealed interface PositionDetailUiState {
    data object Loading : PositionDetailUiState

    /** No position has this id: it was deleted, or the link is stale. */
    data object Missing : PositionDetailUiState

    data class Ready(
        val position: Position,
        val progress: Progress,
        /** The side the page opens on. */
        val openOn: PositionRole,
        /** What each side can do from here. */
        val options: Map<PositionRole, List<TechniqueRow>>,
        /** False when no technique starts here at all, on either side. */
        val hasTechniques: Boolean,
        /** How many of the techniques from here are learned. */
        val learned: Int,
        val total: Int,
        val concepts: List<Named>,
        /** Techniques from elsewhere that end here. */
        val waysIn: List<TechniqueRow>,
        /** The positions either side of this one in learning order. */
        val previous: Named?,
        val next: Named?,
        /** Its bookmarks, oldest first. */
        val videos: List<VideoBookmark>,
    ) : PositionDetailUiState
}

fun positionDetailUiState(snapshot: Snapshot, id: String, side: PositionRole?): PositionDetailUiState {
    val lib = snapshot.library
    val position = lib.positionById[id] ?: return PositionDetailUiState.Missing
    val (outgoing, incoming) = connections(lib, id)
    val options = positionOptions(lib, id)
    val progress = positionProgress(lib, id)
    val index = lib.positions.indexOfFirst { it.id == id }
    return PositionDetailUiState.Ready(
        position = position,
        progress = progressOf(reviewOf(lib, CardKind.Position, id)),
        // A link from the log or Today can open the page on the side the student was stuck on.
        openOn = side ?: PositionRole.Top,
        options = PositionRole.entries.associateWith { techniqueRows(lib, options[it], Direction.Out) },
        hasTechniques = outgoing.isNotEmpty(),
        learned = progress.learned,
        total = progress.total,
        concepts = position.conceptIds.mapNotNull { lib.conceptById[it] }.map { Named(it.id, it.name) },
        waysIn = techniqueRows(lib, incoming, Direction.In),
        previous = lib.positions.getOrNull(index - 1)?.let { Named(it.id, it.name) },
        next = lib.positions.getOrNull(index + 1)?.let { Named(it.id, it.name) },
        videos = linkedVideos(snapshot.videos, ItemKind.Position, id),
    )
}

class PositionDetailViewModel(key: PositionDetail, appData: AppData, videos: VideoCommands) : ViewModel() {
    val uiState: StateFlow<PositionDetailUiState> = screenState(appData) { snapshot ->
        snapshot?.let { positionDetailUiState(it, key.id, key.side) } ?: PositionDetailUiState.Loading
    }

    val videoActions: VideoActions = videos.actions(viewModelScope)
}
