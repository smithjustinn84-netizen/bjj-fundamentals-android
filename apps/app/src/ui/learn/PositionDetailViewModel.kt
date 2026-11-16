package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Position
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
import dev.justinsmith.bjj.ui.common.TechniqueRow
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.screenState
import dev.justinsmith.bjj.ui.common.techniqueRows
import kotlinx.coroutines.flow.StateFlow

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
