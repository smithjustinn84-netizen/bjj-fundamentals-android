package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.linkedVideos
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.ui.common.Named
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.StateFlow

sealed interface ConceptDetailUiState {
    data object Loading : ConceptDetailUiState

    /** No concept has this id: it was deleted, or the link is stale. */
    data object Missing : ConceptDetailUiState

    data class Ready(
        val concept: Concept,
        val progress: Progress,
        /** The positions that list this concept. */
        val usedIn: List<Named>,
        /** The concepts either side of this one in learning order. */
        val previous: Named?,
        val next: Named?,
        /** Its bookmarks, oldest first. */
        val videos: List<VideoBookmark>,
    ) : ConceptDetailUiState
}

fun conceptDetailUiState(snapshot: Snapshot, id: String): ConceptDetailUiState {
    val lib = snapshot.library
    val concept = lib.conceptById[id] ?: return ConceptDetailUiState.Missing
    val index = lib.concepts.indexOfFirst { it.id == id }
    return ConceptDetailUiState.Ready(
        concept = concept,
        progress = progressOf(reviewOf(lib, CardKind.Concept, id)),
        usedIn = lib.positions.filter { id in it.conceptIds }.map { Named(it.id, it.name) },
        previous = lib.concepts.getOrNull(index - 1)?.let { Named(it.id, it.name) },
        next = lib.concepts.getOrNull(index + 1)?.let { Named(it.id, it.name) },
        videos = linkedVideos(snapshot.videos, ItemKind.Concept, id),
    )
}

class ConceptDetailViewModel(key: ConceptDetail, appData: AppData, videos: VideoCommands) : ViewModel() {
    val uiState: StateFlow<ConceptDetailUiState> = screenState(appData) { snapshot ->
        snapshot?.let { conceptDetailUiState(it, key.id) } ?: ConceptDetailUiState.Loading
    }

    val videoActions: VideoActions = videos.actions(viewModelScope)
}
