package dev.justinsmith.bjj.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.Concept
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.defaultEndRole
import dev.justinsmith.bjj.core.defaultRole
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.itemName
import dev.justinsmith.bjj.core.performerRole
import dev.justinsmith.bjj.core.services.ConceptInput
import dev.justinsmith.bjj.core.services.ItemService
import dev.justinsmith.bjj.core.services.PositionInput
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.TransitionInput
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.ui.common.Named
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The order value that places a new item at the end of a list. */
fun nextOrder(orders: List<Int>): Int = (orders.maxOrNull() ?: 0).coerceAtLeast(0) + 1

/** How many techniques go when this item is deleted, so the confirmation can say so. */
fun cascadeCount(lib: Library, kind: ItemKind, id: String): Int = when (kind) {
    ItemKind.Position -> {
        val (outgoing, incoming) = connections(lib, id)
        outgoing.size + incoming.size
    }

    ItemKind.Concept, ItemKind.Transition -> 0
}

/** What a form starts from and offers to pick. `stored` is null for a new item. */
sealed interface EditorFields {
    data class ConceptFields(val stored: Concept?, val nextOrder: Int) : EditorFields

    data class PositionFields(
        val stored: Position?,
        val nextOrder: Int,
        /** Every concept, to tick the ones that matter here. */
        val concepts: List<Named>,
    ) : EditorFields

    data class TechniqueFields(
        val stored: Transition?,
        /** Every position, for where it starts and where it leads. */
        val positions: List<Position>,
        /** The position a new technique opened from one starts at; "" otherwise. */
        val startFromId: String,
        /**
         * The sides stored for who does it and where they end up, when they differ from the usual ones for its
         * type. Null for the usual ones, so they keep following the type if that is changed.
         */
        val pickedRole: PositionRole?,
        val pickedEnd: PositionRole?,
    ) : EditorFields
}

/** How the page ended: the route leaves when it sees one. */
sealed interface EditorOutcome {
    data class Saved(val id: String) : EditorOutcome

    data object Deleted : EditorOutcome
}

sealed interface EditorUiState {
    data object Loading : EditorUiState

    /** The item to edit does not exist: it was deleted, or the link is stale. */
    data object Missing : EditorUiState

    data class Ready(
        val fields: EditorFields,
        /** The item's name and how many techniques go with it, for the question before deleting it. */
        val name: String,
        val cascade: Int,
        /** Set once the item has been saved or deleted. */
        val outcome: EditorOutcome?,
    ) : EditorUiState
}

private fun pickedRole(lib: Library, stored: Transition): PositionRole? =
    performerRole(lib, stored).takeIf { it != defaultRole(stored.type, lib.positionById[stored.fromId]?.category) }

private fun pickedEnd(lib: Library, stored: Transition): PositionRole? =
    endRole(lib, stored).takeIf { it != defaultEndRole(stored.type, performerRole(lib, stored)) }

private fun editorFields(lib: Library, key: Editor): EditorFields = when (key.kind) {
    ItemKind.Concept -> EditorFields.ConceptFields(
        stored = key.id?.let { lib.conceptById[it] },
        nextOrder = nextOrder(lib.concepts.map { it.order }),
    )

    ItemKind.Position -> EditorFields.PositionFields(
        stored = key.id?.let { lib.positionById[it] },
        nextOrder = nextOrder(lib.positions.map { it.order }),
        concepts = lib.concepts.map { Named(it.id, it.name) },
    )

    ItemKind.Transition -> {
        val stored = key.id?.let { lib.transitionById[it] }
        EditorFields.TechniqueFields(
            stored = stored,
            positions = lib.positions,
            startFromId = key.fromPositionId?.takeIf { it in lib.positionById }.orEmpty(),
            pickedRole = stored?.let { pickedRole(lib, it) },
            pickedEnd = stored?.let { pickedEnd(lib, it) },
        )
    }
}

fun editorUiState(snapshot: Snapshot, key: Editor, outcome: EditorOutcome?): EditorUiState {
    val lib = snapshot.library
    val name = key.id?.let { itemName(lib, key.kind, it) }
    // Once saved or deleted the page is on its way out: do not flash "not found" as the item changes under it.
    if (key.id != null && name == null && outcome == null) return EditorUiState.Missing
    return EditorUiState.Ready(
        fields = editorFields(lib, key),
        name = name.orEmpty(),
        cascade = key.id?.let { cascadeCount(lib, key.kind, it) } ?: 0,
        outcome = outcome,
    )
}

/** Creates or edits the item [key] names, and deletes it. */
class EditorViewModel(private val key: Editor, appData: AppData, private val items: ItemService) : ViewModel() {
    private val outcome = MutableStateFlow<EditorOutcome?>(null)

    val uiState: StateFlow<EditorUiState> = screenState(appData, outcome) { snapshot, outcome ->
        snapshot?.let { editorUiState(it, key, outcome) } ?: EditorUiState.Loading
    }

    fun saveConcept(input: ConceptInput) = save { items.saveConcept(key.id, input) }

    fun savePosition(input: PositionInput) = save { items.savePosition(key.id, input) }

    fun saveTransition(input: TransitionInput) = save { items.saveTransition(key.id, input) }

    private fun save(write: suspend () -> String) {
        viewModelScope.launch { outcome.value = EditorOutcome.Saved(write()) }
    }

    fun delete() {
        val id = key.id ?: return
        viewModelScope.launch {
            items.deleteItem(key.kind, id)
            outcome.value = EditorOutcome.Deleted
        }
    }
}
