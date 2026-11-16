package dev.justinsmith.bjj.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.reviewOf
import dev.justinsmith.bjj.core.services.ItemService
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.ui.common.SearchUi
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
