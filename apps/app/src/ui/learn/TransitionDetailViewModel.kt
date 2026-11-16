package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.isOnBlueBeltTest
import dev.justinsmith.bjj.core.performerRole
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.linkedVideos
import dev.justinsmith.bjj.core.techniqueDrill
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.ui.common.Named
import dev.justinsmith.bjj.ui.common.TechniqueLinksUi
import dev.justinsmith.bjj.ui.common.TechniquePicture
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.screenState
import dev.justinsmith.bjj.ui.common.techniqueLinksUi
import dev.justinsmith.bjj.ui.common.techniquePicture
import kotlinx.coroutines.flow.StateFlow

/** A position at one end of a technique. */
data class PositionEnd(val id: String, val name: String, val category: PositionCategory)

private fun Position.asEnd() = PositionEnd(id, name, category)

sealed interface TransitionDetailUiState {
    data object Loading : TransitionDetailUiState

    /** No technique has this id: it was deleted, or the link is stale. */
    data object Missing : TransitionDetailUiState

    data class Ready(
        val transition: Transition,
        val progress: Progress,
        val onBlueBeltTest: Boolean,
        /** Where it starts and where it leads; null for a position the library no longer has, or no end at all. */
        val from: PositionEnd?,
        val to: PositionEnd?,
        /** Who does it, and which side of [to] they end on when that is known. */
        val performer: PositionRole,
        val endSide: PositionRole?,
        /** Null when it cannot be drawn. */
        val picture: TechniquePicture?,
        /** The drill to try, or "" when it has none. */
        val drill: String,
        val links: TechniqueLinksUi,
        /** The techniques either side of this one among those that start where it does. */
        val previous: Named?,
        val next: Named?,
        /** Its bookmarks, oldest first. */
        val videos: List<VideoBookmark>,
    ) : TransitionDetailUiState
}

fun transitionDetailUiState(snapshot: Snapshot, id: String): TransitionDetailUiState {
    val lib = snapshot.library
    val transition = lib.transitionById[id] ?: return TransitionDetailUiState.Missing
    val from = lib.positionById[transition.fromId]
    // Paging stays among the techniques that start where this one does, the way the position page lists them.
    val siblings = from?.let { connections(lib, it.id).outgoing }.orEmpty()
    val index = siblings.indexOfFirst { it.id == id }
    return TransitionDetailUiState.Ready(
        transition = transition,
        progress = progressOf(reviewOf(lib, CardKind.Transition, id)),
        onBlueBeltTest = isOnBlueBeltTest(transition),
        from = from?.asEnd(),
        to = lib.positionById[transition.toId]?.asEnd(),
        performer = performerRole(lib, transition),
        endSide = endRole(lib, transition),
        picture = techniquePicture(lib, transition),
        drill = techniqueDrill(transition),
        links = techniqueLinksUi(lib, id),
        previous = siblings.getOrNull(index - 1)?.let { Named(it.id, it.name) },
        next = siblings.getOrNull(index + 1)?.let { Named(it.id, it.name) },
        videos = linkedVideos(snapshot.videos, ItemKind.Transition, id),
    )
}

class TransitionDetailViewModel(key: TransitionDetail, appData: AppData, videos: VideoCommands) : ViewModel() {
    val uiState: StateFlow<TransitionDetailUiState> = screenState(appData) { snapshot ->
        snapshot?.let { transitionDetailUiState(it, key.id) } ?: TransitionDetailUiState.Loading
    }

    val videoActions: VideoActions = videos.actions(viewModelScope)
}
