package dev.justinsmith.bjj.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.RECENT_CLASS_COUNT
import dev.justinsmith.bjj.core.Situation
import dev.justinsmith.bjj.core.TrainingEntry
import dev.justinsmith.bjj.core.TrainingSummary
import dev.justinsmith.bjj.core.allSituations
import dev.justinsmith.bjj.core.findSituation
import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.core.localDayKey
import dev.justinsmith.bjj.core.recentlyPractised
import dev.justinsmith.bjj.core.recentlyStuck
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.TrainingEntryInput
import dev.justinsmith.bjj.core.services.TrainingLogService
import dev.justinsmith.bjj.core.situationName
import dev.justinsmith.bjj.core.stuckSituationIds
import dev.justinsmith.bjj.core.trainingSummary
import dev.justinsmith.bjj.ui.common.Named
import dev.justinsmith.bjj.ui.common.screenState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A write to the log that failed. */
enum class LogError { Save, Remove }

/** How the log's own writes stand. */
data class LogWrites(
    /** True while an entry is being stored: a second tap must not write a second entry. */
    val saving: Boolean = false,
    /** What went wrong with the last save or delete, to say on the page; null when nothing did. */
    val error: LogError? = null,
    /** How many entries this page has saved: when it goes up, the form starts over for the next one. */
    val saved: Int = 0,
)

/** A class as its row shows it: where the student got stuck and what was practised, for what still exists. */
data class LogEntryUi(val entry: TrainingEntry, val stuck: List<Situation>, val techniques: List<Named>)

/** The techniques that start at one position, for the form's full list. */
data class TechniqueGroup(val position: String, val techniques: List<Named>)

/** What the form offers to tick. */
data class LogChoices(
    /** Every situation, by id and name. */
    val situations: List<Named>,
    /** Ids of the situations recent classes got stuck in, most recent first. */
    val recentlyStuck: List<String>,
    /** Every technique, grouped by where it starts; a position nothing starts from has no group. */
    val techniqueGroups: List<TechniqueGroup>,
    /** The name of every technique by id, whether or not its starting position still exists. */
    val techniqueNames: Map<String, String>,
    /** Ids of the techniques recent classes practised, most recent first. */
    val recentlyPractised: List<String>,
)

sealed interface LogUiState {
    data object Loading : LogUiState

    data class Ready(
        /** Most recent class first. */
        val entries: List<LogEntryUi>,
        val summary: TrainingSummary,
        /** Today as a day key, which a new entry starts on. */
        val today: String,
        val choices: LogChoices,
        val writes: LogWrites,
    ) : LogUiState
}

fun logUiState(snapshot: Snapshot, writes: LogWrites, today: String): LogUiState.Ready {
    val lib = snapshot.library
    val entries = snapshot.trainingLog
    return LogUiState.Ready(
        entries = entries.map { entry ->
            LogEntryUi(
                entry = entry,
                stuck = stuckSituationIds(entry).mapNotNull { findSituation(lib, it) },
                techniques = entry.techniqueIds.mapNotNull { lib.transitionById[it] }.map { Named(it.id, it.name) },
            )
        },
        summary = trainingSummary(entries),
        today = today,
        choices = LogChoices(
            situations = allSituations(lib).map { Named(it.id, situationName(it)) },
            recentlyStuck = recentlyStuck(entries, RECENT_CLASS_COUNT),
            techniqueGroups = lib.positions.mapNotNull { position ->
                val techniques = lib.transitions.filter { it.fromId == position.id }.map { Named(it.id, it.name) }
                if (techniques.isEmpty()) null else TechniqueGroup(position.name, techniques)
            },
            techniqueNames = lib.transitions.associate { it.id to it.name },
            recentlyPractised = recentlyPractised(entries, RECENT_CLASS_COUNT),
        ),
        writes = writes,
    )
}

/** The training log: every class written up, and saving and deleting them. */
class LogViewModel(appData: AppData, private val trainingLog: TrainingLogService, private val clock: Clock) :
    ViewModel() {
    private val writes = MutableStateFlow(LogWrites())

    val uiState: StateFlow<LogUiState> = screenState(appData, writes) { snapshot, writes ->
        snapshot?.let { logUiState(it, writes, localDayKey(clock.now(), clock.zone)) } ?: LogUiState.Loading
    }

    fun save(id: String?, input: TrainingEntryInput) {
        if (writes.value.saving) return
        writes.update { it.copy(saving = true, error = null) }
        attempt(LogError.Save) {
            trainingLog.save(id, input)
            writes.update { it.copy(saved = it.saved + 1) }
        }
    }

    fun remove(id: String) = attempt(LogError.Remove) { trainingLog.remove(id) }

    private fun attempt(failure: LogError, write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
                writes.update { it.copy(saving = false, error = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) { // Whatever was thrown, say so.
                writes.update { it.copy(saving = false, error = failure) }
            }
        }
    }
}
