package dev.justinsmith.bjj.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.justinsmith.bjj.core.DEFAULT_POSITION_CATEGORY
import dev.justinsmith.bjj.core.DEFAULT_TRANSITION_TYPE
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.defaultEndRole
import dev.justinsmith.bjj.core.defaultRole
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.endSideLabel
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.isOnBlueBeltTest
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.roleLabel
import dev.justinsmith.bjj.core.services.ConceptInput
import dev.justinsmith.bjj.core.services.PositionInput
import dev.justinsmith.bjj.core.services.TransitionInput
import dev.justinsmith.bjj.core.techniqueDrill
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.CheckRow
import dev.justinsmith.bjj.design.components.ChoiceField
import dev.justinsmith.bjj.design.components.Field
import dev.justinsmith.bjj.design.components.Segmented
import dev.justinsmith.bjj.design.components.TextField
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.editor_concept_key_points
import dev.justinsmith.bjj.resources.editor_drill
import dev.justinsmith.bjj.resources.editor_one_per_line
import dev.justinsmith.bjj.resources.editor_position_bottom_goals
import dev.justinsmith.bjj.resources.editor_position_category
import dev.justinsmith.bjj.resources.editor_position_concepts
import dev.justinsmith.bjj.resources.editor_position_top_goals
import dev.justinsmith.bjj.resources.editor_technique_blue_belt
import dev.justinsmith.bjj.resources.editor_technique_choose
import dev.justinsmith.bjj.resources.editor_technique_drill_hint
import dev.justinsmith.bjj.resources.editor_technique_end_side
import dev.justinsmith.bjj.resources.editor_technique_from
import dev.justinsmith.bjj.resources.editor_technique_mistakes
import dev.justinsmith.bjj.resources.editor_technique_side_bottom
import dev.justinsmith.bjj.resources.editor_technique_side_top
import dev.justinsmith.bjj.resources.editor_technique_steps
import dev.justinsmith.bjj.resources.editor_technique_steps_hint
import dev.justinsmith.bjj.resources.editor_technique_to
import dev.justinsmith.bjj.resources.editor_technique_type
import dev.justinsmith.bjj.resources.editor_technique_who
import org.jetbrains.compose.resources.stringResource

// Each form keeps a working copy that starts from the stored item, once: later changes to the library cannot
// overwrite what is being typed.

private const val LIST_LINES = 5
private const val GOAL_LINES = 4
private const val STEP_LINES = 6
private const val MISTAKE_LINES = 3

@Composable
fun ConceptForm(
    fields: EditorFields.ConceptFields,
    context: FormContext,
    onSave: (ConceptInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stored = remember { fields.stored }
    var name by remember { mutableStateOf(stored?.name.orEmpty()) }
    var summary by remember { mutableStateOf(stored?.summary.orEmpty()) }
    var keyPoints by remember { mutableStateOf(fromLines(stored?.keyPoints.orEmpty())) }
    var drill by remember { mutableStateOf(stored?.drill.orEmpty()) }
    val startOrder = remember { stored?.order ?: fields.nextOrder }
    var order by remember { mutableStateOf(startOrder.toString()) }
    var notes by remember { mutableStateOf(stored?.notes.orEmpty()) }
    FormShell(
        context,
        canSave = name.isNotBlank(),
        onSave = {
            val input = ConceptInput(
                name = name.trim(),
                summary = summary.trim(),
                keyPoints = toLines(keyPoints),
                drill = drill.trim(),
                order = order.trim().toIntOrNull() ?: startOrder,
                notes = notes.trim(),
            )
            onSave(input)
        },
        modifier = modifier,
    ) {
        NameField(name, { name = it })
        SummaryField(summary, { summary = it })
        TextField(
            stringResource(Res.string.editor_concept_key_points),
            keyPoints,
            { keyPoints = it },
            hint = stringResource(Res.string.editor_one_per_line),
            lines = LIST_LINES,
        )
        TextField(stringResource(Res.string.editor_drill), drill, { drill = it }, lines = 2)
        OrderField(order, { order = it })
        NotesField(notes, { notes = it })
    }
}

@Composable
fun PositionForm(
    fields: EditorFields.PositionFields,
    context: FormContext,
    onSave: (PositionInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stored = remember { fields.stored }
    var name by remember { mutableStateOf(stored?.name.orEmpty()) }
    var summary by remember { mutableStateOf(stored?.summary.orEmpty()) }
    var category by remember { mutableStateOf(stored?.category ?: DEFAULT_POSITION_CATEGORY) }
    var topGoals by remember { mutableStateOf(fromLines(stored?.topGoals.orEmpty())) }
    var bottomGoals by remember { mutableStateOf(fromLines(stored?.bottomGoals.orEmpty())) }
    var conceptIds by remember { mutableStateOf(stored?.conceptIds.orEmpty()) }
    val startOrder = remember { stored?.order ?: fields.nextOrder }
    var order by remember { mutableStateOf(startOrder.toString()) }
    var notes by remember { mutableStateOf(stored?.notes.orEmpty()) }
    FormShell(
        context,
        canSave = name.isNotBlank(),
        onSave = {
            val input = PositionInput(
                name = name.trim(),
                category = category,
                summary = summary.trim(),
                topGoals = toLines(topGoals),
                bottomGoals = toLines(bottomGoals),
                conceptIds = conceptIds,
                order = order.trim().toIntOrNull() ?: startOrder,
                notes = notes.trim(),
            )
            onSave(input)
        },
        modifier = modifier,
    ) {
        NameField(name, { name = it })
        SummaryField(summary, { summary = it })
        ChoiceField(
            stringResource(Res.string.editor_position_category),
            PositionCategory.entries,
            category,
            { it.label },
            { category = it },
        )
        TextField(
            stringResource(Res.string.editor_position_top_goals),
            topGoals,
            { topGoals = it },
            hint = stringResource(Res.string.editor_one_per_line),
            lines = GOAL_LINES,
        )
        TextField(
            stringResource(Res.string.editor_position_bottom_goals),
            bottomGoals,
            { bottomGoals = it },
            hint = stringResource(Res.string.editor_one_per_line),
            lines = GOAL_LINES,
        )
        if (fields.concepts.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                BjjText(stringResource(Res.string.editor_position_concepts), style = Type.label)
                for (concept in fields.concepts) {
                    CheckRow(
                        concept.name,
                        checked = concept.id in conceptIds,
                        onChange = { on -> conceptIds = if (on) conceptIds + concept.id else conceptIds - concept.id },
                    )
                }
            }
        }
        OrderField(order, { order = it })
        NotesField(notes, { notes = it })
    }
}

/** A question answered with a switch between the two sides. */
@Composable
private fun SideField(
    label: String,
    value: PositionRole,
    onChange: (PositionRole) -> Unit,
    name: @Composable (PositionRole) -> String,
) {
    Field(label) { Segmented(PositionRole.entries, value, onChange, { name(it) }, Modifier.fillMaxWidth()) }
}

/** A technique's working copy: what the form shows and changes until it is saved. */
private class TransitionDraft(fields: EditorFields.TechniqueFields) {
    private val stored = fields.stored
    var name by mutableStateOf(stored?.name.orEmpty())
    var type by mutableStateOf(stored?.type ?: DEFAULT_TRANSITION_TYPE)
    var fromId by mutableStateOf(stored?.fromId ?: fields.startFromId)
    var toId by mutableStateOf(stored?.toId.orEmpty())

    // A stored side that is just the usual one for the technique's type starts as null, so it keeps following the
    // type if that is changed. Only a side that differs was a deliberate pick, and that one stays put.
    var pickedRole by mutableStateOf(fields.pickedRole)
    var pickedEnd by mutableStateOf(fields.pickedEnd)
    var steps by mutableStateOf(fromLines(stored?.steps.orEmpty()))
    var mistakes by mutableStateOf(fromLines(stored?.commonMistakes.orEmpty()))
    var drill by mutableStateOf(stored?.let(::techniqueDrill).orEmpty())
    var blueBelt by mutableStateOf(stored?.let(::isOnBlueBeltTest) ?: false)
    var notes by mutableStateOf(stored?.notes.orEmpty())

    fun input(role: PositionRole, endSide: PositionRole?) = TransitionInput(
        name = name.trim(),
        type = type,
        fromId = fromId,
        toId = toId,
        role = role,
        // A technique that ends the roll leads nowhere, so it has no end side to store.
        endRole = endSide,
        steps = toLines(steps),
        commonMistakes = toLines(mistakes),
        drill = drill.trim(),
        blueBeltTest = blueBelt,
        notes = notes.trim(),
    )
}

@Composable
fun TransitionForm(
    fields: EditorFields.TechniqueFields,
    context: FormContext,
    onSave: (TransitionInput) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = remember { TransitionDraft(fields) }
    val positions = fields.positions
    val from: Position? = positions.firstOrNull { it.id == draft.fromId }
    val to: Position? = positions.firstOrNull { it.id == draft.toId }
    val ends = endsTheRoll(draft.type)
    val role = draft.pickedRole ?: defaultRole(draft.type, from?.category)
    val endSide = draft.pickedEnd ?: defaultEndRole(draft.type, role)
    FormShell(
        context,
        canSave = draft.name.isNotBlank() && from != null && (ends || to != null),
        onSave = { onSave(draft.input(role, endSide)) },
        modifier = modifier,
    ) {
        NameField(draft.name, { draft.name = it })
        ChoiceField(
            stringResource(Res.string.editor_technique_type),
            TransitionType.entries,
            draft.type,
            { it.label },
            { draft.type = it },
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            val choose = stringResource(Res.string.editor_technique_choose)
            ChoiceField(
                stringResource(Res.string.editor_technique_from),
                positions,
                from,
                { it.name },
                { draft.fromId = it.id },
                placeholder = choose,
            )
            if (!ends) {
                ChoiceField(
                    stringResource(Res.string.editor_technique_to),
                    positions,
                    to,
                    { it.name },
                    { draft.toId = it.id },
                    placeholder = choose,
                )
            }
        }
        // Role names before a starting position is chosen are plain; after that they come from its kind.
        SideField(stringResource(Res.string.editor_technique_who), role, { draft.pickedRole = it }) { side ->
            roleName(from, side)
        }
        if (endSide != null && to != null && !ends) {
            SideField(stringResource(Res.string.editor_technique_end_side), endSide, { draft.pickedEnd = it }) {
                endSideLabel(to.category, it)
            }
        }
        TextField(
            stringResource(Res.string.editor_technique_steps),
            draft.steps,
            { draft.steps = it },
            hint = stringResource(Res.string.editor_technique_steps_hint),
            lines = STEP_LINES,
        )
        TextField(
            stringResource(Res.string.editor_technique_mistakes),
            draft.mistakes,
            { draft.mistakes = it },
            hint = stringResource(Res.string.editor_one_per_line),
            lines = MISTAKE_LINES,
        )
        TextField(
            stringResource(Res.string.editor_drill),
            draft.drill,
            { draft.drill = it },
            hint = stringResource(Res.string.editor_technique_drill_hint),
            lines = 2,
        )
        CheckRow(stringResource(Res.string.editor_technique_blue_belt), draft.blueBelt, { draft.blueBelt = it })
        NotesField(draft.notes, { draft.notes = it })
    }
}

@Composable
private fun roleName(from: Position?, side: PositionRole): String = when {
    from != null -> roleLabel(from.category, side)
    side == PositionRole.Top -> stringResource(Res.string.editor_technique_side_top)
    else -> stringResource(Res.string.editor_technique_side_bottom)
}
