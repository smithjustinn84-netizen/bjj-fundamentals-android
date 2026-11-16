package dev.justinsmith.bjj.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.services.Snapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * How long a screen's state keeps being worked out after its page stops showing: long enough to ride out a
 * rotation, short enough that a page come back to is worked out afresh, so what is due follows the clock.
 */
private const val KEEP_ALIVE_MS = 5_000L

/**
 * A screen's state: [toState] of what is stored, again after every change. The snapshot is null until the
 * first read lands. It starts from the snapshot already read, so a page opens without a flash of "loading".
 */
fun <T> ViewModel.screenState(appData: AppData, toState: (Snapshot?) -> T): StateFlow<T> =
    appData.snapshot.map(toState).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(KEEP_ALIVE_MS),
        toState(appData.snapshot.value),
    )

/** The same, for a screen whose state also depends on something of its [own]: a query, a choice, a session. */
fun <A, T> ViewModel.screenState(
    appData: AppData,
    own: StateFlow<A>,
    toState: (Snapshot?, A) -> T,
): StateFlow<T> = combine(appData.snapshot, own, toState).stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(KEEP_ALIVE_MS),
    toState(appData.snapshot.value, own.value),
)
