package dev.justinsmith.bjj.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.formatClassCount
import dev.justinsmith.bjj.core.formatMatTime
import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.core.isDayKey
import dev.justinsmith.bjj.core.quickPicks
import dev.justinsmith.bjj.core.recentlyPractised
import dev.justinsmith.bjj.core.recentlyStuck
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import dev.justinsmith.bjj.core.situationName
import dev.justinsmith.bjj.core.stuckSituationIds
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
import dev.justinsmith.bjj.design.components.ButtonSize
import dev.justinsmith.bjj.design.components.Card
import dev.justinsmith.bjj.design.components.Chip
import dev.justinsmith.bjj.design.components.ConfirmDialog
import dev.justinsmith.bjj.design.components.Divider
import dev.justinsmith.bjj.design.components.LinkButton
import dev.justinsmith.bjj.design.components.Notice
import dev.justinsmith.bjj.design.components.SectionHead
import dev.justinsmith.bjj.design.components.TextField
import dev.justinsmith.bjj.design.components.ToggleChip
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.icons.BjjIcons
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.nav.Nav
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.common_cancel
import dev.justinsmith.bjj.resources.common_delete
import dev.justinsmith.bjj.resources.common_edit
import dev.justinsmith.bjj.resources.common_training_log
import dev.justinsmith.bjj.resources.log_about
import dev.justinsmith.bjj.resources.log_classes
import dev.justinsmith.bjj.resources.log_day
import dev.justinsmith.bjj.resources.log_day_error
import dev.justinsmith.bjj.resources.log_day_hint
import dev.justinsmith.bjj.resources.log_delete_text
import dev.justinsmith.bjj.resources.log_delete_title
import dev.justinsmith.bjj.resources.log_empty_text
import dev.justinsmith.bjj.resources.log_empty_title
import dev.justinsmith.bjj.resources.log_entry_focus
import dev.justinsmith.bjj.resources.log_entry_mat_time
import dev.justinsmith.bjj.resources.log_entry_struggled
import dev.justinsmith.bjj.resources.log_entry_stuck_in
import dev.justinsmith.bjj.resources.log_entry_taught
import dev.justinsmith.bjj.resources.log_entry_worked
import dev.justinsmith.bjj.resources.log_error_remove
import dev.justinsmith.bjj.resources.log_error_save
import dev.justinsmith.bjj.resources.log_focus
import dev.justinsmith.bjj.resources.log_focus_hint
import dev.justinsmith.bjj.resources.log_form_edit
import dev.justinsmith.bjj.resources.log_form_new
import dev.justinsmith.bjj.resources.log_minutes
import dev.justinsmith.bjj.resources.log_minutes_other
import dev.justinsmith.bjj.resources.log_notes_closed
import dev.justinsmith.bjj.resources.log_save_changes
import dev.justinsmith.bjj.resources.log_save_entry
import dev.justinsmith.bjj.resources.log_struggled
import dev.justinsmith.bjj.resources.log_stuck
import dev.justinsmith.bjj.resources.log_stuck_about
import dev.justinsmith.bjj.resources.log_stuck_choose
import dev.justinsmith.bjj.resources.log_stuck_others
import dev.justinsmith.bjj.resources.log_taught
import dev.justinsmith.bjj.resources.log_techniques
import dev.justinsmith.bjj.resources.log_techniques_all
import dev.justinsmith.bjj.resources.log_techniques_choose
import dev.justinsmith.bjj.resources.log_worked
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.Loading
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.SideBySide
import dev.justinsmith.bjj.ui.common.formatDay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** What a new entry starts with: most classes run about an hour. */
private const val DEFAULT_CLASS_MINUTES = 60

/** The class lengths offered as one tap; anything else is typed in. */
private val MINUTE_PRESETS = listOf(45, 60, 90, 120)

@Composable
fun LogRoute(nav: Nav) {
    val viewModel = koinViewModel<LogViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val ready = state) {
        LogUiState.Loading -> Loading()
        is LogUiState.Ready -> LogScreen(ready, nav, viewModel::save, viewModel::remove)
    }
}

@Composable
fun LogScreen(
    state: LogUiState.Ready,
    nav: Nav,
    onSave: (String?, TrainingEntryInput) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = state.entries
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    // Bumped after each save so the form starts empty for the next entry.
    var formRound by rememberSaveable { mutableIntStateOf(0) }
    // How many saves this page has started the form over for. A save the ViewModel reports beyond that is new.
    var savesSeen by rememberSaveable { mutableIntStateOf(state.writes.saved) }
    val startOver: () -> Unit = {
        editingId = null
        formRound++
    }
    LaunchedEffect(state.writes.saved) {
        if (state.writes.saved != savesSeen) {
            savesSeen = state.writes.saved
            startOver()
        }
    }
    val editing = entries.firstOrNull { it.entry.id == editingId }?.entry
    val summary = state.summary
    Page(modifier) {
        PageHead(
            stringResource(Res.string.common_training_log),
            about = stringResource(Res.string.log_about),
        )
        SideBySide(
            first = {
                Card {
                    key(editing?.id, formRound) {
                        EntryForm(state.choices, editing, state.today, state.writes, onSave, onCancel = startOver)
                    }
                }
            },
            second = {
                Card {
                    SectionHead(stringResource(Res.string.log_classes)) {
                        if (summary.classes > 0) {
                            val line = "${formatClassCount(summary.classes)} · ${formatMatTime(summary.minutes)}"
                            BjjText(line, style = Type.small, color = BjjTheme.palette.muted)
                        }
                    }
                    if (entries.isEmpty()) {
                        EmptyState(
                            BjjIcons.Log,
                            stringResource(Res.string.log_empty_title),
                            stringResource(Res.string.log_empty_text),
                        )
                    } else {
                        entries.forEachIndexed { index, row ->
                            // Keyed, so removing a class leaves each other row its own state.
                            key(row.entry.id) {
                                if (index > 0) Divider()
                                EntryRow(row, nav, onEdit = {
                                    editingId = row.entry.id
                                }, onRemove = { onRemove(row.entry.id) })
                            }
                        }
                    }
                }
            },
        )
    }
}

/** What to say on the page when a write failed. */
private val LogError.message: StringResource
    get() = when (this) {
        LogError.Save -> Res.string.log_error_save
        LogError.Remove -> Res.string.log_error_remove
    }

/** A group of chips that stays folded away under a button until asked for. */
@Composable
private fun Folded(closedLabel: String, startOpen: Boolean, content: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(startOpen) }
    if (open) {
        content()
    } else {
        Button(closedLabel, { open = true }, size = ButtonSize.Small, icon = BjjIcons.Plus)
    }
}

/** A class write-up's working copy: what the form shows and changes until it is saved. */
private class EntryDraft(entry: TrainingEntry?, today: String) {
    var day by mutableStateOf(entry?.day ?: today)
    var minutes by mutableStateOf((entry?.minutes ?: DEFAULT_CLASS_MINUTES).toString())

    /** True while the length is typed in, not picked from the presets. */
    var otherMinutes by mutableStateOf(minutes.toIntOrNull() !in MINUTE_PRESETS)
    var taught by mutableStateOf(entry?.taught.orEmpty())
    var worked by mutableStateOf(entry?.worked.orEmpty())
    var struggled by mutableStateOf(entry?.struggled.orEmpty())
    var focus by mutableStateOf(entry?.focus.orEmpty())
    var techniqueIds by mutableStateOf(entry?.techniqueIds.orEmpty())
    var stuckIds by mutableStateOf(entry?.let(::stuckSituationIds).orEmpty())

    /** True once Save was pressed with a day that is not one. */
    var dayError by mutableStateOf(false)

    val hasNotes get() = taught.isNotEmpty() || worked.isNotEmpty() || struggled.isNotEmpty()

    fun toggleStuck(id: String, on: Boolean) {
        stuckIds = if (on) stuckIds + id else stuckIds - id
    }

    fun toggleTechnique(id: String, on: Boolean) {
        techniqueIds = if (on) techniqueIds + id else techniqueIds - id
    }

    fun input() = TrainingEntryInput(
        day = day.trim(),
        minutes = minutes.trim().toIntOrNull() ?: 0,
        taught = taught,
        worked = worked,
        struggled = struggled,
        focus = focus,
        techniqueIds = techniqueIds,
        stuckIds = stuckIds,
    )
}

@Composable
private fun ColumnScope.EntryForm(
    choices: LogChoices,
    entry: TrainingEntry?,
    today: String,
    state: LogWrites,
    onSave: (String?, TrainingEntryInput) -> Unit,
    onCancel: () -> Unit,
) {
    val draft = remember { EntryDraft(entry, today) }
    BjjText(
        if (entry != null) {
            stringResource(Res.string.log_form_edit, formatDay(entry.day))
        } else {
            stringResource(Res.string.log_form_new)
        },
        style = Type.h2,
    )
    TextField(
        stringResource(Res.string.log_day),
        draft.day,
        { draft.day = it },
        hint = stringResource(Res.string.log_day_hint, formatDay(draft.day), today),
        keyboardType = KeyboardType.Number,
    )
    MinutesField(draft)
    StuckField(choices, draft)
    TechniquesField(choices, draft)
    TextField(
        stringResource(Res.string.log_focus),
        draft.focus,
        { draft.focus = it },
        hint = stringResource(Res.string.log_focus_hint),
    )
    // The chips cover a quick entry; writing is there for the days there is more to say.
    Folded(stringResource(Res.string.log_notes_closed), startOpen = draft.hasNotes) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            TextField(stringResource(Res.string.log_taught), draft.taught, { draft.taught = it }, lines = 2)
            TextField(stringResource(Res.string.log_worked), draft.worked, { draft.worked = it }, lines = 2)
            TextField(stringResource(Res.string.log_struggled), draft.struggled, { draft.struggled = it }, lines = 2)
        }
    }
    if (draft.dayError) Notice(stringResource(Res.string.log_day_error, today), isError = true)
    state.error?.let { Notice(stringResource(it.message), isError = true) }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Button(
            stringResource(if (entry != null) Res.string.log_save_changes else Res.string.log_save_entry),
            {
                draft.dayError = !isDayKey(draft.day.trim())
                if (!draft.dayError) onSave(entry?.id, draft.input())
            },
            Modifier.weight(1f),
            ButtonKind.Primary,
            enabled = !state.saving,
        )
        if (entry != null) Button(stringResource(Res.string.common_cancel), onCancel, Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.MinutesField(draft: EntryDraft) {
    val label = stringResource(Res.string.log_minutes)
    BjjText(label, style = Type.label)
    Wrap {
        for (preset in MINUTE_PRESETS) {
            ToggleChip("$preset", checked = !draft.otherMinutes && draft.minutes.toIntOrNull() == preset, onChange = {
                draft.otherMinutes = false
                draft.minutes = "$preset"
            })
        }
        ToggleChip(
            stringResource(Res.string.log_minutes_other),
            checked = draft.otherMinutes,
            onChange = { draft.otherMinutes = true },
        )
    }
    if (draft.otherMinutes) {
        TextField(label, draft.minutes, { draft.minutes = it }, keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun ColumnScope.StuckField(choices: LogChoices, draft: EntryDraft) {
    val situations = choices.situations
    if (situations.isEmpty()) return
    // Offered up front; remembered so the row does not reshuffle as chips are toggled.
    val quickIds = remember {
        val known = situations.map { it.id }.toSet()
        quickPicks(draft.stuckIds, choices.recentlyStuck) { it in known }
    }
    val (quick, others) = situations.partition { it.id in quickIds }
    BjjText(stringResource(Res.string.log_stuck), style = Type.label)
    BjjText(
        stringResource(Res.string.log_stuck_about),
        style = Type.small,
        color = BjjTheme.palette.muted,
    )
    if (quick.isNotEmpty()) {
        Wrap { for (s in quick) ToggleChip(s.name, s.id in draft.stuckIds, { draft.toggleStuck(s.id, it) }) }
    }
    if (others.isNotEmpty()) {
        val more = if (quick.isEmpty()) Res.string.log_stuck_choose else Res.string.log_stuck_others
        Folded(stringResource(more), startOpen = false) {
            Wrap {
                for (s in others) ToggleChip(s.name, s.id in draft.stuckIds, { draft.toggleStuck(s.id, it) })
            }
        }
    }
}

@Composable
private fun ColumnScope.TechniquesField(choices: LogChoices, draft: EntryDraft) {
    val groups = choices.techniqueGroups
    if (groups.isEmpty()) return
    val quick = remember {
        quickPicks(draft.techniqueIds, choices.recentlyPractised) { it in choices.techniqueNames }
    }
    BjjText(stringResource(Res.string.log_techniques), style = Type.label)
    if (quick.isNotEmpty()) {
        Wrap {
            for (id in quick) {
                val name = choices.techniqueNames[id].orEmpty()
                ToggleChip(name, id in draft.techniqueIds, { draft.toggleTechnique(id, it) })
            }
        }
    }
    val more = if (quick.isEmpty()) Res.string.log_techniques_choose else Res.string.log_techniques_all
    Folded(stringResource(more), startOpen = false) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            for ((position, techniques) in groups) {
                BjjText(position, style = Type.small, color = BjjTheme.palette.muted)
                Wrap {
                    for (t in techniques) {
                        ToggleChip(t.name, t.id in draft.techniqueIds, {
                            draft.toggleTechnique(t.id, it)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(row: LogEntryUi, nav: Nav, onEdit: () -> Unit, onRemove: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val entry = row.entry
    val techniques = row.techniques
    val stuck = row.stuck
    val notes = listOf(
        Res.string.log_entry_taught to entry.taught,
        Res.string.log_entry_worked to entry.worked,
        Res.string.log_entry_struggled to entry.struggled,
        Res.string.log_entry_focus to entry.focus,
    )
        .filter { it.second.isNotEmpty() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        BjjText(formatDay(entry.day), style = Type.h3)
        BjjText(
            stringResource(Res.string.log_entry_mat_time, formatMatTime(entry.minutes)),
            style = Type.small,
            color = BjjTheme.palette.muted,
        )
        for ((line, text) in notes) BjjText(stringResource(line, text))
        if (stuck.isNotEmpty()) {
            BjjText(stringResource(Res.string.log_entry_stuck_in), style = Type.strong)
            Wrap { for (s in stuck) Chip(situationName(s), { nav.open(PositionDetail(s.position.id, s.role)) }) }
        }
        if (techniques.isNotEmpty()) {
            Wrap { for (t in techniques) Chip(t.name, { nav.open(TransitionDetail(t.id)) }) }
        }
        Row {
            LinkButton(stringResource(Res.string.common_edit), onEdit)
            LinkButton(stringResource(Res.string.common_delete), { confirming = true }, danger = true)
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = stringResource(Res.string.log_delete_title),
            text = stringResource(Res.string.log_delete_text, formatDay(entry.day)),
            confirm = stringResource(Res.string.common_delete),
            cancel = stringResource(Res.string.common_cancel),
            onConfirm = {
                confirming = false
                onRemove()
            },
            onDismiss = { confirming = false },
        )
    }
}
