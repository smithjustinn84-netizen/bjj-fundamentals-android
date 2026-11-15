package dev.justinsmith.bjj.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.services.ConceptInput
import dev.justinsmith.bjj.core.services.PositionInput
import dev.justinsmith.bjj.core.services.TransitionInput
import kotlinx.coroutines.launch

/** Saves and deletes for the item forms. Each save tells [onSaved] the item's id once it is stored. */
class EditorViewModel(private val core: Core) : ViewModel() {
    fun saveConcept(id: String?, input: ConceptInput, onSaved: (String) -> Unit) {
        viewModelScope.launch { onSaved(core.items.saveConcept(id, input)) }
    }

    fun savePosition(id: String?, input: PositionInput, onSaved: (String) -> Unit) {
        viewModelScope.launch { onSaved(core.items.savePosition(id, input)) }
    }

    fun saveTransition(id: String?, input: TransitionInput, onSaved: (String) -> Unit) {
        viewModelScope.launch { onSaved(core.items.saveTransition(id, input)) }
    }

    fun delete(kind: ItemKind, id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            core.items.deleteItem(kind, id)
            onDeleted()
        }
    }
}
