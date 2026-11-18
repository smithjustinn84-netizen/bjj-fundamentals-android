package dev.justinsmith.bjj.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.services.Snapshot
import kotlinx.coroutines.flow.Flow
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
fun <T> ViewModel.screenState(appData: AppData, toState: (Snapshot?) -> T): StateFlow<T> {
    val first = FirstState(appData.snapshot.value, Unit, toState(appData.snapshot.value))
    return started(appData.snapshot.map { first.take(it, Unit) ?: toState(it) }, first.state)
}

/** The same, for a screen whose state also depends on something of its [own]: a query, a choice, a session. */
fun <A, T> ViewModel.screenState(
    appData: AppData,
    own: StateFlow<A>,
    toState: (Snapshot?, A) -> T,
): StateFlow<T> {
    val first = FirstState(appData.snapshot.value, own.value, toState(appData.snapshot.value, own.value))
    return started(combine(appData.snapshot, own) { s, a -> first.take(s, a) ?: toState(s, a) }, first.state)
}

/**
 * The same, for a screen where something of its [own] changes often and touches little, such as what is typed
 * in a search box. [content] is worked out from what is stored alone, and only after a change; [withOwn] lays
 * the [own] part over it. So typing a letter works out the search again, not everything the page shows.
 */
fun <A, T> ViewModel.screenState(
    appData: AppData,
    own: StateFlow<A>,
    content: (Snapshot?) -> T,
    withOwn: (content: T, snapshot: Snapshot?, own: A) -> T,
): StateFlow<T> {
    val firstContent = FirstState(appData.snapshot.value, Unit, content(appData.snapshot.value))
    val contents = appData.snapshot.map { it to (firstContent.take(it, Unit) ?: content(it)) }
    val initial = withOwn(firstContent.state, appData.snapshot.value, own.value)
    return started(combine(contents, own) { (s, c), a -> withOwn(c, s, a) }, initial)
}

private fun <T> ViewModel.started(flow: Flow<T>, initial: T): StateFlow<T> =
    flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(KEEP_ALIVE_MS), initial)

/**
 * The state worked out as the ViewModel was made, from [snapshot] and [own], handed back once when the flow
 * starts from those same values, so a page opening does not work out its state twice. Only once: a page come
 * back to later is worked out afresh, since the clock has moved on.
 */
private class FirstState<A, T>(private val snapshot: Snapshot?, private val own: A, val state: T) {
    private var unused = true

    fun take(snapshot: Snapshot?, own: A): T? {
        val same = unused && snapshot === this.snapshot && own == this.own
        unused = false
        return if (same) state else null
    }
}
