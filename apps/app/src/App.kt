package dev.justinsmith.bjj

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.justinsmith.bjj.core.Theme
import dev.justinsmith.bjj.core.ThemeChoice
import dev.justinsmith.bjj.core.resolveTheme
import dev.justinsmith.bjj.core.services.studyStats
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.nav.Backup
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.LogTab
import dev.justinsmith.bjj.nav.MoreTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TodayTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.Videos
import dev.justinsmith.bjj.nav.back
import dev.justinsmith.bjj.nav.currentTab
import dev.justinsmith.bjj.nav.push
import dev.justinsmith.bjj.nav.replaceTop
import dev.justinsmith.bjj.nav.selectTab
import dev.justinsmith.bjj.platform.SystemBars
import dev.justinsmith.bjj.ui.backup.BackupRoute
import dev.justinsmith.bjj.ui.common.AppViewModel
import dev.justinsmith.bjj.ui.common.Nav
import dev.justinsmith.bjj.ui.common.hasRail
import dev.justinsmith.bjj.ui.editor.EditorRoute
import dev.justinsmith.bjj.ui.home.HomeRoute
import dev.justinsmith.bjj.ui.learn.BlueBeltRoute
import dev.justinsmith.bjj.ui.learn.ConceptDetailRoute
import dev.justinsmith.bjj.ui.learn.ConceptsRoute
import dev.justinsmith.bjj.ui.learn.LessonsRoute
import dev.justinsmith.bjj.ui.learn.PositionDetailRoute
import dev.justinsmith.bjj.ui.learn.PositionsRoute
import dev.justinsmith.bjj.ui.learn.TransitionDetailRoute
import dev.justinsmith.bjj.ui.log.TrainingLogRoute
import dev.justinsmith.bjj.ui.more.MoreRoute
import dev.justinsmith.bjj.ui.review.ReviewRoute
import dev.justinsmith.bjj.ui.review.ReviewSessionRoute
import dev.justinsmith.bjj.ui.shell.TabBar
import dev.justinsmith.bjj.ui.shell.TabRail
import dev.justinsmith.bjj.ui.videos.VideosRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.viewmodel.koinViewModel

/** Every key the back stack can hold, so a saved stack can be read back. */
internal val navConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TodayTab::class)
            subclass(LearnTab::class)
            subclass(ReviewTab::class)
            subclass(LogTab::class)
            subclass(MoreTab::class)
            subclass(ReviewSession::class)
            subclass(ConceptDetail::class)
            subclass(PositionDetail::class)
            subclass(TransitionDetail::class)
            subclass(Videos::class)
            subclass(Backup::class)
            subclass(Editor::class)
        }
    }
}

/** The app: its theme, its one back stack, and the bar or rail that moves between the five tabs. */
@Composable
fun App(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<AppViewModel>()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    // Until the stored choice is read the device decides, which is also what a new library gets.
    val theme = resolveTheme(snapshot?.themeChoice ?: ThemeChoice.Device, isSystemInDarkTheme())
    BjjTheme(dark = theme == Theme.Dark) {
        SystemBars(dark = theme == Theme.Dark)
        val backStack = rememberNavBackStack(navConfiguration, TodayTab)
        val nav = remember(backStack) {
            Nav(
                open = backStack::push,
                back = backStack::back,
                selectTab = backStack::selectTab,
                replace = backStack::replaceTop,
            )
        }
        val current = backStack.currentTab()
        val dueCount = snapshot?.let { studyStats(it, viewModel.now(), viewModel.zone).dueCount } ?: 0
        // A review session takes the whole window: nothing to wander off to until it is done or closed.
        val showBar = backStack.lastOrNull() !is ReviewSession
        val rail = hasRail()
        val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        Box(modifier.fillMaxSize().background(BjjTheme.palette.bg).windowInsetsPadding(insets)) {
            if (rail) {
                Row(Modifier.fillMaxSize()) {
                    if (showBar) TabRail(current, dueCount, nav.selectTab)
                    Pages(backStack, nav, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Pages(backStack, nav, Modifier.weight(1f))
                    if (showBar) TabBar(current, dueCount, nav.selectTab)
                }
            }
        }
    }
}

@Composable
private fun Pages(backStack: NavBackStack<NavKey>, nav: Nav, modifier: Modifier = Modifier) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.back() },
        // Passing this replaces NavDisplay's default list, so the saveable-state holder is repeated. The
        // ViewModel store decorator follows it: it needs the holder above it.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        modifier = modifier,
        entryProvider = remember(nav) {
            entryProvider {
                entry<TodayTab> { HomeRoute(nav) }
                entry<LearnTab> { key ->
                    when (key.section) {
                        LearnSection.Concepts -> ConceptsRoute(nav)
                        LearnSection.Positions -> PositionsRoute(nav)
                        LearnSection.Lessons -> LessonsRoute(nav)
                        LearnSection.BlueBelt -> BlueBeltRoute(nav)
                    }
                }
                entry<ReviewTab> { key -> ReviewRoute(key.deck, key.lessonId, nav) }
                entry<ReviewSession> { key -> ReviewSessionRoute(key, nav) }
                entry<LogTab> { TrainingLogRoute(nav) }
                entry<MoreTab> { MoreRoute(nav) }
                entry<ConceptDetail> { key -> ConceptDetailRoute(key.id, nav) }
                entry<PositionDetail> { key -> PositionDetailRoute(key.id, key.side, nav) }
                entry<TransitionDetail> { key -> TransitionDetailRoute(key.id, nav) }
                entry<Videos> { VideosRoute(nav) }
                entry<Backup> { BackupRoute(nav) }
                entry<Editor> { key -> EditorRoute(key, nav) }
            }
        },
    )
}
