package dev.justinsmith.bjj.bindings

import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.services.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * What every screen reads: one snapshot of everything stored, read again after each change. Null until the
 * first read lands. One for the whole app, so two screens never disagree about what is stored.
 */
class AppData(core: Core, scope: CoroutineScope) {
    val snapshot: StateFlow<Snapshot?> = core.changes
        .onStart { emit(Unit) }
        // A burst of changes needs one read, of how things stand at the end of it.
        .conflate()
        .map { core.queries.snapshot() }
        .stateIn(scope, SharingStarted.Eagerly, null)
}
