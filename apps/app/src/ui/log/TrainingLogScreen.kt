package dev.justinsmith.bjj.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.justinsmith.bjj.core.Library
import dev.justinsmith.bjj.core.RECENT_CLASS_COUNT
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.allSituations
import dev.justinsmith.bjj.core.findSituation
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
import dev.justinsmith.bjj.core.trainingSummary
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.Button
import dev.justinsmith.bjj.design.components.ButtonKind
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
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.ui.common.EmptyState
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.Page
import dev.justinsmith.bjj.ui.common.PageHead
import dev.justinsmith.bjj.ui.common.WithSnapshot
import dev.justinsmith.bjj.ui.home.formatDay
import org.koin.compose.viewmodel.koinViewModel

/** What a new entry starts with: most classes run about an hour. */
private const val DEFAULT_CLASS_MINUTES = 60

/** The class lengths offered as one tap; anything else is typed in. */
private val MINUTE_PRESETS = listOf(45, 60, 90, 120)

@Composable
fun TrainingLogRoute(nav: Nav) {
    val log = koinViewModel<LogViewModel>()
    val state by log.state.collectAsStateWithLifecycle()
    WithSnapshot { snapshot, app ->
        TrainingLogScreen(
            lib = snapshot.library,
            entries = snapshot.trainingLog,
            today = dev.justinsmith.bjj.core.localDayKey(app.now(), app.zone),
            state = state,
            nav = nav,
            onSave = log::save,
            onRemove = log::remove,
        )
    }
}

@Composable
fun TrainingLogScreen(
    lib: Library,
    entries: List<TrainingEntry>,
    today: String,
    state: LogUiState,
    nav: Nav,
    onSave: (String?, TrainingEntryInput, () -> Unit) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    // Bumped after each save so the form starts empty for the next entry.
    var formRound by rememberSaveable { mutableIntStateOf(0) }
    val editing = entries.firstOrNull { it.id == editingId }
    val summary = trainingSummary(entries)
    Page(modifier) {
        PageHead(
            "Training log",
            about = "Write up each class while it is fresh. The one thing you pick to work on next time shows on " +
                "Today.",
        )
        Card {
            key(editing?.id, formRound) {
                EntryForm(lib, entries, editing, today, state, onSave) {
                    editingId = null
                    formRound++
                }
            }
        }
        Card {
            SectionHead("Classes") {
                if (summary.classes > 0) {
                    val line = "${formatClassCount(summary.classes)} · ${formatMatTime(summary.minutes)}"
                    BjjText(line, style = Type.small, color = BjjTheme.palette.muted)
                }
            }
            if (entries.isEmpty()) {
                EmptyState(
                    BjjIcons.Log,
                    "No classes logged yet",
                    "Your first entry will show here. A sentence or two is enough.",
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) Divider()
                    EntryRow(lib, entry, nav, onEdit = { editingId = entry.id }, onRemove = { onRemove(entry.id) })
                }
            }
        }
    }
}

/** A group of chips that stays folded away under a link until asked for. */
@Composable
private fun Folded(closedLabel: String, startOpen: Boolean, content: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(startOpen) }
    if (open) content() else LinkButton(closedLabel, { open = true })
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
    lib: Library,
    /** Every entry so far: recent ones decide which choices are offered up front. */
    entries: List<TrainingEntry>,
    entry: TrainingEntry?,
    today: String,
    state: LogUiState,
    onSave: (String?, TrainingEntryInput, () -> Unit) -> Unit,
    onDone: () -> Unit,
) {
    val draft = remember { EntryDraft(entry, today) }
    val muted = BjjTheme.palette.muted
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.Bottom) {
        BjjText(
            if (entry !=
                null
            ) {
                "Edit ${formatDay(entry.day)}"
            } else {
                "Log a class"
            },
            Modifier.weight(1f),
            style = Type.h2,
        )
        TextField("Day", draft.day, { draft.day = it }, Modifier.width(DAY_WIDTH), keyboardType = KeyboardType.Number)
    }
    BjjText("${formatDay(draft.day)}. Write the day as year-month-day, like $today.", style = Type.small, color = muted)
    MinutesField(draft)
    StuckField(lib, entries, draft)
    TechniquesField(lib, entries, draft)
    TextField(
        "One thing to work on next time",
        draft.focus,
        { draft.focus = it },
        hint = "Shown on Today until your next entry sets a new one.",
    )
    // The chips cover a quick entry; writing is there for the days there is more to say.
    Folded("Notes: what was taught, what worked", startOpen = draft.hasNotes) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            TextField("What was taught", draft.taught, { draft.taught = it }, lines = 2)
            TextField("What worked in rolling", draft.worked, { draft.worked = it }, lines = 2)
            TextField("Where I got stuck, in my own words", draft.struggled, { draft.struggled = it }, lines = 2)
        }
    }
    if (draft.dayError) Notice("Write the day as year-month-day with a four-digit year, like $today.", isError = true)
    if (state.error.isNotEmpty()) Notice(state.error, isError = true)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Button(
            if (entry != null) "Save changes" else "Save entry",
            {
                draft.dayError = !isDayKey(draft.day.trim())
                if (!draft.dayError) onSave(entry?.id, draft.input(), onDone)
            },
            Modifier.weight(1f),
            ButtonKind.Primary,
            enabled = !state.saving,
        )
        if (entry != null) Button("Cancel", onDone, Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.MinutesField(draft: EntryDraft) {
    BjjText("Minutes on the mat", style = Type.label)
    Wrap {
        for (preset in MINUTE_PRESETS) {
            ToggleChip("$preset", checked = !draft.otherMinutes && draft.minutes.toIntOrNull() == preset, onChange = {
                draft.otherMinutes = false
                draft.minutes = "$preset"
            })
        }
        ToggleChip("Other", checked = draft.otherMinutes, onChange = { draft.otherMinutes = true })
    }
    if (draft.otherMinutes) {
        TextField("Minutes on the mat", draft.minutes, { draft.minutes = it }, keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun ColumnScope.StuckField(lib: Library, entries: List<TrainingEntry>, draft: EntryDraft) {
    val situations = allSituations(lib)
    if (situations.isEmpty()) return
    // Offered up front; remembered so the row does not reshuffle as chips are toggled.
    val quickIds = remember {
        quickPicks(draft.stuckIds, recentlyStuck(entries, RECENT_CLASS_COUNT)) { findSituation(lib, it) != null }
    }
    val (quick, others) = situations.partition { it.id in quickIds }
    BjjText("Where I got stuck", style = Type.label)
    BjjText(
        "Cards you have not studied yet for these, and for your techniques from them, come first in review.",
        style = Type.small,
        color = BjjTheme.palette.muted,
    )
    if (quick.isNotEmpty()) {
        Wrap { for (s in quick) ToggleChip(situationName(s), s.id in draft.stuckIds, { draft.toggleStuck(s.id, it) }) }
    }
    if (others.isNotEmpty()) {
        Folded(if (quick.isEmpty()) "Choose positions" else "Other positions", startOpen = false) {
            Wrap {
                for (s in others) ToggleChip(situationName(s), s.id in draft.stuckIds, { draft.toggleStuck(s.id, it) })
            }
        }
    }
}

@Composable
private fun ColumnScope.TechniquesField(lib: Library, entries: List<TrainingEntry>, draft: EntryDraft) {
    val groups = lib.positions.map { p ->
        p to lib.transitions.filter { it.fromId == p.id }
    }.filter { it.second.isNotEmpty() }
    if (groups.isEmpty()) return
    val quick = remember {
        quickPicks(draft.techniqueIds, recentlyPractised(entries, RECENT_CLASS_COUNT)) { it in lib.transitionById }
    }
    BjjText("Techniques practised", style = Type.label)
    if (quick.isNotEmpty()) {
        Wrap {
            for (id in quick) {
                val name = lib.transitionById[id]?.name.orEmpty()
                ToggleChip(name, id in draft.techniqueIds, { draft.toggleTechnique(id, it) })
            }
        }
    }
    Folded(if (quick.isEmpty()) "Choose techniques" else "All techniques", startOpen = false) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            for ((position, techniques) in groups) {
                BjjText(position.name, style = Type.small, color = BjjTheme.palette.muted)
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

private val DAY_WIDTH = 132.dp

@Composable
private fun EntryRow(lib: Library, entry: TrainingEntry, nav: Nav, onEdit: () -> Unit, onRemove: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    val techniques = entry.techniqueIds.mapNotNull { lib.transitionById[it] }
    val stuck = stuckSituationIds(entry).mapNotNull { findSituation(lib, it) }
    val notes = listOf(
        "Taught" to entry.taught,
        "Worked" to entry.worked,
        "Got stuck" to entry.struggled,
        "Next time" to entry.focus,
    )
        .filter { it.second.isNotEmpty() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        BjjText(formatDay(entry.day), style = Type.h3)
        BjjText("${formatMatTime(entry.minutes)} on the mat", style = Type.small, color = BjjTheme.palette.muted)
        for ((label, text) in notes) BjjText("$label: $text")
        if (stuck.isNotEmpty()) {
            BjjText("Stuck in:", style = Type.strong)
            Wrap { for (s in stuck) Chip(situationName(s), { nav.open(PositionDetail(s.position.id, s.role)) }) }
        }
        if (techniques.isNotEmpty()) {
            Wrap { for (t in techniques) Chip(t.name, { nav.open(TransitionDetail(t.id)) }) }
        }
        Row {
            LinkButton("Edit", onEdit)
            LinkButton("Delete", { confirming = true }, danger = true)
        }
    }
    if (confirming) {
        ConfirmDialog(
            title = "Delete entry?",
            text = "Delete the entry for ${formatDay(entry.day)}?",
            confirm = "Delete",
            cancel = "Cancel",
            onConfirm = {
                confirming = false
                onRemove()
            },
            onDismiss = { confirming = false },
        )
    }
}
