package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.ItemService
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.linkedVideos
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.ui.common.Named
import dev.justinsmith.bjj.ui.common.SearchUi
import dev.justinsmith.bjj.ui.common.VideoActions
import dev.justinsmith.bjj.ui.common.VideoCommands
import dev.justinsmith.bjj.ui.common.progressOf
import dev.justinsmith.bjj.ui.common.screenState
import dev.justinsmith.bjj.ui.common.searchUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A concept in the list: its name, what it is and how well it is known. */
data class ConceptTile(val id: String, val name: String, val summary: String, val progress: Progress)

sealed interface ConceptsUiState {
    data object Loading : ConceptsUiState

    data class Ready(
        /** In learning order. */
        val concepts: List<ConceptTile>,
        /** Names of starter concepts the library does not hold and has not dismissed. */
        val missingStarter: List<String>,
        val search: SearchUi,
    ) : ConceptsUiState
}

fun conceptsUiState(snapshot: Snapshot, query: String): ConceptsUiState.Ready {
    val lib = snapshot.library
    return ConceptsUiState.Ready(
        concepts = lib.concepts.map {
            ConceptTile(it.id, it.name, it.summary, progressOf(reviewOf(lib, CardKind.Concept, it.id)))
        },
        missingStarter = snapshot.missingStarterConcepts.map { it.name },
        search = searchUi(lib, query),
    )
}

class ConceptsViewModel(appData: AppData, private val items: ItemService) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<ConceptsUiState> = screenState(appData, query) { snapshot, query ->
        snapshot?.let { conceptsUiState(it, query) } ?: ConceptsUiState.Loading
    }

    fun search(text: String) {
        query.value = text
    }

    fun addStarter() {
        viewModelScope.launch { items.addMissingStarterConcepts() }
    }

    fun dismissStarter() {
        viewModelScope.launch { items.dismissMissingStarterConcepts() }
    }
}

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
