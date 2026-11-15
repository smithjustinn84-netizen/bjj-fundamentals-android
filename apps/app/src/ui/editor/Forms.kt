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
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.PositionCategory
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.defaultEndRole
import dev.justinsmith.bjj.core.defaultRole
import dev.justinsmith.bjj.core.endRole
import dev.justinsmith.bjj.core.endSideLabel
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.isOnBlueBeltTest
import dev.justinsmith.bjj.core.label
import dev.justinsmith.bjj.core.performerRole
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

// Each form keeps a working copy that starts from the stored item, once: later changes to the library cannot
// overwrite what is being typed.

private const val LIST_LINES = 5
private const val GOAL_LINES = 4
private const val STEP_LINES = 6
private const val MISTAKE_LINES = 3

@Composable
fun ConceptForm(context: FormContext, modifier: Modifier = Modifier) {
    val lib = context.library
    val stored = remember { context.itemId?.let { lib.conceptById[it] } }
    var name by remember { mutableStateOf(stored?.name.orEmpty()) }
    var summary by remember { mutableStateOf(stored?.summary.orEmpty()) }
    var keyPoints by remember { mutableStateOf(fromLines(stored?.keyPoints.orEmpty())) }
    var drill by remember { mutableStateOf(stored?.drill.orEmpty()) }
    val startOrder = remember { stored?.order ?: nextOrder(lib.concepts.map { it.order }) }
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
            context.viewModel.saveConcept(context.itemId, input, context.onSaved)
        },
        modifier = modifier,
    ) {
        NameField(name, { name = it })
        SummaryField(summary, { summary = it })
        TextField("Key points", keyPoints, { keyPoints = it }, hint = "One per line.", lines = LIST_LINES)
        TextField("Drill", drill, { drill = it }, lines = 2)
        OrderField(order, { order = it })
        NotesField(notes, { notes = it })
    }
}

@Composable
fun PositionForm(context: FormContext, modifier: Modifier = Modifier) {
    val lib = context.library
    val stored = remember { context.itemId?.let { lib.positionById[it] } }
    var name by remember { mutableStateOf(stored?.name.orEmpty()) }
    var summary by remember { mutableStateOf(stored?.summary.orEmpty()) }
    var category by remember { mutableStateOf(stored?.category ?: DEFAULT_POSITION_CATEGORY) }
    var topGoals by remember { mutableStateOf(fromLines(stored?.topGoals.orEmpty())) }
    var bottomGoals by remember { mutableStateOf(fromLines(stored?.bottomGoals.orEmpty())) }
    var conceptIds by remember { mutableStateOf(stored?.conceptIds.orEmpty()) }
    val startOrder = remember { stored?.order ?: nextOrder(lib.positions.map { it.order }) }
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
            context.viewModel.savePosition(context.itemId, input, context.onSaved)
        },
        modifier = modifier,
    ) {
        NameField(name, { name = it })
        SummaryField(summary, { summary = it })
        ChoiceField("Category", PositionCategory.entries, category, { it.label }, { category = it })
        TextField("Goals on top / attacking", topGoals, { topGoals = it }, hint = "One per line.", lines = GOAL_LINES)
        TextField("Goals on bottom / defending", bottomGoals, {
            bottomGoals = it
        }, hint = "One per line.", lines = GOAL_LINES)
        if (lib.concepts.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                BjjText("Related concepts", style = Type.label)
                for (concept in lib.concepts) {
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
    name: (PositionRole) -> String,
) {
    Field(label) { Segmented(PositionRole.entries, value, onChange, { name(it) }, Modifier.fillMaxWidth()) }
}

/** A technique's working copy: what the form shows and changes until it is saved. */
private class TransitionDraft(lib: Library, stored: Transition?, fromPositionId: String?) {
    var name by mutableStateOf(stored?.name.orEmpty())
    var type by mutableStateOf(stored?.type ?: DEFAULT_TRANSITION_TYPE)
    var fromId by mutableStateOf(stored?.fromId ?: fromPositionId?.takeIf { it in lib.positionById }.orEmpty())
    var toId by mutableStateOf(stored?.toId.orEmpty())

    // A stored side that is just the usual one for the technique's type starts as null, so it keeps following the
    // type if that is changed. Only a side that differs was a deliberate pick, and that one stays put.
    var pickedRole by mutableStateOf(stored?.let { pickedRole(lib, it) })
    var pickedEnd by mutableStateOf(stored?.let { pickedEnd(lib, it) })
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

private fun pickedRole(lib: Library, stored: Transition): PositionRole? =
    performerRole(lib, stored).takeIf { it != defaultRole(stored.type, lib.positionById[stored.fromId]?.category) }

private fun pickedEnd(lib: Library, stored: Transition): PositionRole? =
    endRole(lib, stored).takeIf { it != defaultEndRole(stored.type, performerRole(lib, stored)) }

@Composable
fun TransitionForm(context: FormContext, modifier: Modifier = Modifier) {
    val lib = context.library
    val draft =
        remember { TransitionDraft(lib, context.itemId?.let { lib.transitionById[it] }, context.fromPositionId) }
    val from: Position? = lib.positionById[draft.fromId]
    val to: Position? = lib.positionById[draft.toId]
    val ends = endsTheRoll(draft.type)
    val role = draft.pickedRole ?: defaultRole(draft.type, from?.category)
    val endSide = draft.pickedEnd ?: defaultEndRole(draft.type, role)
    FormShell(
        context,
        canSave = draft.name.isNotBlank() && from != null && (ends || to != null),
        onSave = { context.viewModel.saveTransition(context.itemId, draft.input(role, endSide), context.onSaved) },
        modifier = modifier,
    ) {
        NameField(draft.name, { draft.name = it })
        ChoiceField("Type", TransitionType.entries, draft.type, { it.label }, { draft.type = it })
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            ChoiceField("From position", lib.positions, from, {
                it.name
            }, { draft.fromId = it.id }, placeholder = "Choose…")
            if (!ends) {
                ChoiceField("Ends in", lib.positions, to, { it.name }, { draft.toId = it.id }, placeholder = "Choose…")
            }
        }
        // Role names before a starting position is chosen are plain; after that they come from its kind.
        SideField("Who does it", role, { draft.pickedRole = it }) { side -> roleName(from, side) }
        if (endSide != null && to != null && !ends) {
            SideField("You end up", endSide, { draft.pickedEnd = it }) { endSideLabel(to.category, it) }
        }
        TextField("Steps", draft.steps, { draft.steps = it }, hint = "One step per line, in order.", lines = STEP_LINES)
        TextField("Common mistakes", draft.mistakes, {
            draft.mistakes = it
        }, hint = "One per line.", lines = MISTAKE_LINES)
        TextField("Drill", draft.drill, {
            draft.drill = it
        }, hint = "A short solo or partner drill to practise it.", lines = 2)
        CheckRow("On the blue belt test", draft.blueBelt, { draft.blueBelt = it })
        NotesField(draft.notes, { draft.notes = it })
    }
}

private fun roleName(from: Position?, side: PositionRole): String = when {
    from != null -> roleLabel(from.category, side)
    side == PositionRole.Top -> "Top"
    else -> "Bottom"
}
