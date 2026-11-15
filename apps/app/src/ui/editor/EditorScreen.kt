package dev.justinsmith.bjj.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.connections
import dev.justinsmith.bjj.core.itemName
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.plural
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.ConfirmDialog
import dev.justinsmith.bjj.design.components.PageTitle
import dev.justinsmith.bjj.design.components.TextField
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.nav.listKey
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.Missing
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.WithSnapshot
import org.koin.compose.viewmodel.koinViewModel

/** List fields are edited as one entry per line. */
fun toLines(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

fun fromLines(list: List<String>): String = list.joinToString("\n")

/** The order value that places a new item at the end of a list. */
fun nextOrder(orders: List<Int>): Int = (orders.maxOrNull() ?: 0).coerceAtLeast(0) + 1

/** What else goes when this item is deleted, so the confirmation can say so. */
fun cascadeWarning(lib: Library, kind: ItemKind, id: String): String = when (kind) {
    ItemKind.Position -> {
        val (outgoing, incoming) = connections(lib, id)
        val count = outgoing.size + incoming.size
        if (count > 0) " This also deletes ${plural(count, "technique")} connected to it." else ""
    }

    ItemKind.Concept, ItemKind.Transition -> ""
}

/** What every item form is given. A new kind of item only needs a form that takes these. */
class FormContext(
    /** The item being edited, or null when creating a new one. */
    val itemId: String?,
    val library: Library,
    /** The position a new technique should start from, when it was opened from one. */
    val fromPositionId: String?,
    val viewModel: EditorViewModel,
    val onSaved: (String) -> Unit,
    val onCancel: () -> Unit,
    /** Null when creating: there is nothing to delete yet. */
    val onDelete: (() -> Unit)?,
)

/** Creates or edits any item. Owns loading and deletion; the form for each kind owns its fields. */
@Composable
fun EditorRoute(key: Editor, nav: Nav, modifier: Modifier = Modifier) {
    val editor = koinViewModel<EditorViewModel>()
    WithSnapshot { snapshot, _ ->
        val lib = snapshot.library
        val kind = key.kind
        val existingName = key.id?.let { itemName(lib, kind, it) }
        var confirming by remember { mutableStateOf(false) }
        // Once saved or deleted the page is on its way out: do not flash "not found" as the item changes under it.
        var leaving by remember { mutableStateOf(false) }
        if (key.id != null && existingName == null && !leaving) {
            Missing(kind.label, "Back", nav.back)
            return@WithSnapshot
        }
        val context = FormContext(
            itemId = key.id,
            library = lib,
            fromPositionId = key.fromPositionId,
            viewModel = editor,
            onSaved = { savedId ->
                leaving = true
                // Back from a saved edit is the item's page; a new item's page takes the form's place.
                if (key.id != null) nav.back() else nav.replace(itemKey(kind, savedId))
            },
            onCancel = nav.back,
            onDelete = key.id?.let { { confirming = true } },
        )
        Column(modifier.fillMaxSize().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.widthIn(max = Sizes.readingMax).fillMaxSize().padding(horizontal = Spacing.pageGutter),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                BackLink("Cancel", nav.back)
                PageTitle(if (key.id != null) "Edit ${kind.label.lowercase()}" else "New ${kind.label.lowercase()}")
                // Started afresh for each item, so a form begins from that item's values.
                androidx.compose.runtime.key(kind, key.id) {
                    when (kind) {
                        ItemKind.Concept -> ConceptForm(context)
                        ItemKind.Position -> PositionForm(context)
                        ItemKind.Transition -> TransitionForm(context)
                    }
                }
            }
        }
        val id = key.id
        if (confirming && id != null) {
            ConfirmDialog(
                title = "Delete ${kind.label.lowercase()}?",
                text = "Delete \"${existingName.orEmpty()}\"?${cascadeWarning(lib, kind, id)}",
                confirm = "Delete",
                cancel = "Cancel",
                onConfirm = {
                    confirming = false
                    leaving = true
                    editor.delete(kind, id) { nav.selectTab(listKey(kind)) }
                },
                onDismiss = { confirming = false },
            )
        }
    }
}

/**
 * The card and the Save, Cancel and Delete row shared by every item form. The row stays at the foot of the
 * window, above the keyboard, so saving never needs a scroll. Save waits for a name.
 */
@Composable
fun FormShell(
    context: FormContext,
    canSave: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    fields: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = Spacing.s4)) {
            Card(content = fields)
        }
        Box(Modifier.fillMaxWidth().height(Sizes.hairline).background(BjjTheme.palette.border))
        Row(
            Modifier.fillMaxWidth().padding(vertical = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Button("Save", onSave, Modifier.weight(1f), ButtonKind.Primary, enabled = canSave)
            Button("Cancel", context.onCancel, Modifier.weight(1f))
            context.onDelete?.let { Button("Delete", it, kind = ButtonKind.Danger) }
        }
    }
}

@Composable
fun NameField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField("Name", value, onChange, modifier)
}

@Composable
fun SummaryField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField("Summary", value, onChange, modifier, hint = "One plain-language sentence.", lines = 2)
}

@Composable
fun NotesField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        "My notes",
        value,
        onChange,
        modifier,
        hint = "Anything from class you want to remember.",
        lines = NOTES_LINES,
    )
}

private const val NOTES_LINES = 4

/** The learning order as typed: anything that is not a whole number saves as the order it had. */
@Composable
fun OrderField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        "Learning order",
        value,
        onChange,
        modifier,
        hint = "Lower numbers come first in lists and reviews.",
        keyboardType = KeyboardType.Number,
    )
}
