package dev.justinsmith.bjj.ui

import dev.justinsmith.bjj.bindings.AppData
import dev.justinsmith.bjj.bindings.Clock
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import dev.justinsmith.bjj.ui.common.VideoCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * What a ViewModel's test stands on: a real core on in-memory storage holding the starter content, with reads and
 * writes finishing before each call returns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {
    protected val t: TestCore = testCore()
    protected val clock = Clock(CoreEnv(t.storage, newId = { "x" }, now = { t.clock.now }, zone = LocalZone.Utc))
    protected val appData = AppData(t.core, CoroutineScope(UnconfinedTestDispatcher()))
    protected val videos = VideoCommands(t.core.videos) { }

    @BeforeTest
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetMain() = Dispatchers.resetMain()

    /** Keeps a screen's state worked out, as a page showing it would, and reads it. */
    protected fun <T> TestScope.watch(state: StateFlow<T>): () -> T {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect { } }
        return { state.value }
    }
}
