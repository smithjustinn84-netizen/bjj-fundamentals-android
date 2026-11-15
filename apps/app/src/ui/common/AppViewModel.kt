package dev.justinsmith.bjj.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.core.Core
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.VideoInput
import dev.justinsmith.bjj.platform.Links
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * What the pages that only show the library share: the snapshot, the clock to read it by, and the few things
 * done from a list or an item's page. A page with a form or a session of its own has its own ViewModel.
 */
class AppViewModel(appData: AppData, private val core: Core, env: CoreEnv, private val links: Links) : ViewModel() {
    val snapshot: StateFlow<Snapshot?> = appData.snapshot
    val zone: LocalZone = env.zone
    private val clock = env.now

    /** The time now. Read when drawing: what is due moves on without anything stored changing. */
    fun now(): Long = clock()

    fun addMissingStarterConcepts() = write { core.items.addMissingStarterConcepts() }

    fun dismissMissingStarterConcepts() = write { core.items.dismissMissingStarterConcepts() }

    fun addMissingStarterTechniques() = write { core.items.addMissingStarterTechniques() }

    fun dismissMissingStarterTechniques() = write { core.items.dismissMissingStarterTechniques() }

    fun deleteItem(kind: ItemKind, id: String) = write { core.items.deleteItem(kind, id) }

    fun addVideo(input: VideoInput) = write { core.videos.add(input) }

    fun removeVideo(id: String) = write { core.videos.remove(id) }

    fun setTheme(choice: ThemeChoice) = write { core.settings.setTheme(choice) }

    fun open(url: String) = links.open(url)

    private fun write(change: suspend () -> Unit) {
        viewModelScope.launch { change() }
    }
}
