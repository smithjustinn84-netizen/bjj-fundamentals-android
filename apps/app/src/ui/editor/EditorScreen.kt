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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.services.ConceptInput
import dev.justinsmith.bjj.core.services.PositionInput
import dev.justinsmith.bjj.core.services.TransitionInput
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
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.itemKey
import dev.justinsmith.bjj.nav.listKey
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_cancel
import dev.justinsmith.bjj.resources.common_delete
import dev.justinsmith.bjj.resources.editor_back
import dev.justinsmith.bjj.resources.editor_delete_concept_title
import dev.justinsmith.bjj.resources.editor_delete_position_title
import dev.justinsmith.bjj.resources.editor_delete_technique_title
import dev.justinsmith.bjj.resources.editor_delete_text
import dev.justinsmith.bjj.resources.editor_delete_text_cascade
import dev.justinsmith.bjj.resources.editor_edit_concept
import dev.justinsmith.bjj.resources.editor_edit_position
import dev.justinsmith.bjj.resources.editor_edit_technique
import dev.justinsmith.bjj.resources.editor_name
import dev.justinsmith.bjj.resources.editor_new_concept
import dev.justinsmith.bjj.resources.editor_new_position
import dev.justinsmith.bjj.resources.editor_new_technique
import dev.justinsmith.bjj.resources.editor_notes
import dev.justinsmith.bjj.resources.editor_notes_hint
import dev.justinsmith.bjj.resources.editor_order
import dev.justinsmith.bjj.resources.editor_order_hint
import dev.justinsmith.bjj.resources.editor_save
import dev.justinsmith.bjj.resources.editor_summary
import dev.justinsmith.bjj.resources.editor_summary_hint
import dev.justinsmith.bjj.ui.common.BackLink
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Missing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** List fields are edited as one entry per line. */
fun toLines(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

fun fromLines(list: List<String>): String = list.joinToString("\n")

private val ItemKind.editTitle: StringResource
    get() = when (this) {
        ItemKind.Concept -> Res.string.editor_edit_concept
        ItemKind.Position -> Res.string.editor_edit_position
        ItemKind.Transition -> Res.string.editor_edit_technique
    }

private val ItemKind.newTitle: StringResource
    get() = when (this) {
        ItemKind.Concept -> Res.string.editor_new_concept
        ItemKind.Position -> Res.string.editor_new_position
        ItemKind.Transition -> Res.string.editor_new_technique
    }

private val ItemKind.deleteTitle: StringResource
    get() = when (this) {
        ItemKind.Concept -> Res.string.editor_delete_concept_title
        ItemKind.Position -> Res.string.editor_delete_position_title
        ItemKind.Transition -> Res.string.editor_delete_technique_title
    }

/** What every item form is given besides its own fields. A new kind of item only needs a form that takes these. */
class FormContext(
    val onCancel: () -> Unit,
    /** Null when creating: there is nothing to delete yet. */
    val onDelete: (() -> Unit)?,
)

/** What the forms and the delete question do, from the page's ViewModel. */
class EditorActions(
    val saveConcept: (ConceptInput) -> Unit,
    val savePosition: (PositionInput) -> Unit,
    val saveTransition: (TransitionInput) -> Unit,
    val delete: () -> Unit,
)

/** Creates or edits any item. Owns loading and deletion; the form for each kind owns its fields. */
@Composable
fun EditorRoute(key: Editor, nav: Nav, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<EditorViewModel> { parametersOf(key) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        EditorActions(viewModel::saveConcept, viewModel::savePosition, viewModel::saveTransition, viewModel::delete)
    }
    when (val ready = state) {
        EditorUiState.Loading -> Loading()

        EditorUiState.Missing -> Missing(key.kind.label, stringResource(Res.string.editor_back), nav.back)

        is EditorUiState.Ready -> {
            LaunchedEffect(ready.outcome) {
                when (val outcome = ready.outcome) {
                    null -> Unit

                    // Back from a saved edit is the item's page; a new item's page takes the form's place.
                    is EditorOutcome.Saved ->
                        if (key.id != null) nav.back() else nav.replace(itemKey(key.kind, outcome.id))

                    EditorOutcome.Deleted -> nav.selectTab(listKey(key.kind))
                }
            }
            EditorScreen(key, ready, nav.back, actions, modifier)
        }
    }
}

@Composable
private fun EditorScreen(
    key: Editor,
    state: EditorUiState.Ready,
    onBack: () -> Unit,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val kind = key.kind
    var confirming by remember { mutableStateOf(false) }
    val context = FormContext(onCancel = onBack, onDelete = key.id?.let { { confirming = true } })
    Column(modifier.fillMaxSize().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.widthIn(max = Sizes.readingMax).fillMaxSize().padding(horizontal = Spacing.pageGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            BackLink(stringResource(Res.string.common_cancel), onBack)
            PageTitle(stringResource(if (key.id != null) kind.editTitle else kind.newTitle))
            // Started afresh for each item, so a form begins from that item's values.
            androidx.compose.runtime.key(kind, key.id) {
                when (val fields = state.fields) {
                    is EditorFields.ConceptFields -> ConceptForm(fields, context, actions.saveConcept)
                    is EditorFields.PositionFields -> PositionForm(fields, context, actions.savePosition)
                    is EditorFields.TechniqueFields -> TransitionForm(fields, context, actions.saveTransition)
                }
            }
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = stringResource(kind.deleteTitle),
            text = if (state.cascade > 0) {
                pluralStringResource(Res.plurals.editor_delete_text_cascade, state.cascade, state.name, state.cascade)
            } else {
                stringResource(Res.string.editor_delete_text, state.name)
            },
            confirm = stringResource(Res.string.common_delete),
            cancel = stringResource(Res.string.common_cancel),
            onConfirm = {
                confirming = false
                actions.delete()
            },
            onDismiss = { confirming = false },
        )
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
            Button(
                stringResource(Res.string.editor_save),
                onSave,
                Modifier.weight(1f),
                ButtonKind.Primary,
                enabled = canSave,
            )
            Button(stringResource(Res.string.common_cancel), context.onCancel, Modifier.weight(1f))
            context.onDelete?.let { Button(stringResource(Res.string.common_delete), it, kind = ButtonKind.Danger) }
        }
    }
}

@Composable
fun NameField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(stringResource(Res.string.editor_name), value, onChange, modifier)
}

@Composable
fun SummaryField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        stringResource(Res.string.editor_summary),
        value,
        onChange,
        modifier,
        hint = stringResource(Res.string.editor_summary_hint),
        lines = 2,
    )
}

@Composable
fun NotesField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        stringResource(Res.string.editor_notes),
        value,
        onChange,
        modifier,
        hint = stringResource(Res.string.editor_notes_hint),
        lines = NOTES_LINES,
    )
}

private const val NOTES_LINES = 4

/** The learning order as typed: anything that is not a whole number saves as the order it had. */
@Composable
fun OrderField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        stringResource(Res.string.editor_order),
        value,
        onChange,
        modifier,
        hint = stringResource(Res.string.editor_order_hint),
        keyboardType = KeyboardType.Number,
    )
}
