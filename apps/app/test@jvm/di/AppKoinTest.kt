package dev.justinsmith.bjj.di

import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.platform.Links
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/** The container the app is built from. */
@OptIn(KoinExperimentalAPI::class)
class AppKoinTest {
    @Test
    fun `the container can build everything it declares`() {
        // What the platform supplies is taken as given; everything the app declares must be buildable from it.
        val platform = module { }
        // The keys are handed to a page's ViewModel as it is asked for, not held by the container.
        val keys = listOf(
            ReviewTab::class,
            ReviewSession::class,
            ConceptDetail::class,
            PositionDetail::class,
            TransitionDetail::class,
            Editor::class,
        )
        appModules(platform).drop(1).forEach {
            it.verify(extraTypes = listOf(CoreEnv::class, Links::class, CoroutineScope::class) + keys)
        }
    }
}
