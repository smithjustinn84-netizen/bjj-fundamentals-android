package dev.justinsmith.bjj

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.justinsmith.bjj.core.Theme
import dev.justinsmith.bjj.core.resolveTheme
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.nav.Backup
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.LogTab
import dev.justinsmith.bjj.nav.MoreTab
import dev.justinsmith.bjj.nav.Nav
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
import dev.justinsmith.bjj.ui.common.hasRail
import dev.justinsmith.bjj.ui.common.hasWideRail
import dev.justinsmith.bjj.ui.editor.EditorRoute
import dev.justinsmith.bjj.ui.home.HomeRoute
import dev.justinsmith.bjj.ui.learn.BlueBeltRoute
import dev.justinsmith.bjj.ui.learn.ConceptDetailRoute
import dev.justinsmith.bjj.ui.learn.ConceptsRoute
import dev.justinsmith.bjj.ui.learn.LessonsRoute
import dev.justinsmith.bjj.ui.learn.PositionDetailRoute
import dev.justinsmith.bjj.ui.learn.PositionsRoute
import dev.justinsmith.bjj.ui.learn.TransitionDetailRoute
import dev.justinsmith.bjj.ui.log.LogRoute
import dev.justinsmith.bjj.ui.more.MoreRoute
import dev.justinsmith.bjj.ui.review.ReviewRoute
import dev.justinsmith.bjj.ui.review.ReviewSessionRoute
import dev.justinsmith.bjj.ui.shell.ShellViewModel
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
    val viewModel = koinViewModel<ShellViewModel>()
    val shell by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = resolveTheme(shell.themeChoice, isSystemInDarkTheme())
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
        val dueCount = shell.dueCount
        // A review session takes the whole window: nothing to wander off to until it is done or closed.
        val showBar = backStack.lastOrNull() !is ReviewSession
        val rail = hasRail()
        val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        val ground = modifier.fillMaxSize().background(BjjTheme.palette.bg).windowInsetsPadding(insets)
        Shell(rail, ground) {
            Pages(backStack, nav)
            if (showBar) {
                if (rail) {
                    TabRail(
                        current,
                        dueCount,
                        hasWideRail(),
                        nav.selectTab,
                    )
                } else {
                    TabBar(current, dueCount, nav.selectTab)
                }
            }
        }
    }
}

/**
 * The pages and, when there is one, the bar under them or the rail to their left. The pages come first in the tree
 * whichever it is, so what they save (a half-filled form, an open picker) is found again after the window turns
 * and the bar becomes the rail: a page's saved values are found by their place in the tree.
 */
@Composable
private fun Shell(rail: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val pages = measurables.first()
        val bar = measurables.getOrNull(1)?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        if (rail) {
            val side = bar?.width ?: 0
            val shown = pages.measure(Constraints.fixed(width - side, height))
            layout(width, height) {
                bar?.place(0, 0)
                shown.place(side, 0)
            }
        } else {
            val under = bar?.height ?: 0
            val shown = pages.measure(Constraints.fixed(width, height - under))
            layout(width, height) {
                shown.place(0, 0)
                bar?.place(0, height - under)
            }
        }
    }
}

@Composable
private fun Pages(backStack: NavBackStack<NavKey>, nav: Nav, modifier: Modifier = Modifier) {
    val still = reducedMotion()
    val ground = BjjTheme.palette.bg
    // A page going back slides off over the one underneath, so it has to be a sheet you cannot see through.
    val opaque = remember(ground) {
        NavEntryDecorator<NavKey> { entry -> Box(Modifier.fillMaxSize().background(ground)) { entry.Content() } }
    }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.back() },
        // Passing this replaces NavDisplay's default list, so the saveable-state holder is repeated. The
        // ViewModel store decorator follows it: it needs the holder above it.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            opaque,
        ),
        modifier = modifier,
        transitionSpec = { pageEnter(still) },
        popTransitionSpec = { pageLeave(still, NavigationEvent.EDGE_NONE) },
        // The same tween, which the back gesture scrubs with the finger and runs backwards if it lets go.
        predictivePopTransitionSpec = { edge -> pageLeave(still, edge) },
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
                entry<ReviewTab> { key -> ReviewRoute(key, nav) }
                entry<ReviewSession> { key -> ReviewSessionRoute(key, nav) }
                entry<LogTab> { LogRoute(nav) }
                entry<MoreTab> { MoreRoute(nav) }
                entry<ConceptDetail> { key -> ConceptDetailRoute(key, nav) }
                entry<PositionDetail> { key -> PositionDetailRoute(key, nav) }
                entry<TransitionDetail> { key -> TransitionDetailRoute(key, nav) }
                entry<Videos> { VideosRoute(nav) }
                entry<Backup> { BackupRoute(nav) }
                entry<Editor> { key -> EditorRoute(key, nav) }
            }
        },
    )
}

/**
 * A page rises a little into place as it fades in, and the one it replaces just goes; nothing moves when
 * animations are off.
 */
private fun pageEnter(still: Boolean): ContentTransform =
    if (still) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        (fadeIn(tween(Motion.BASE)) + slideInVertically(tween(Motion.BASE)) { it / RISE_FROM }) togetherWith
            fadeOut(tween(Motion.FAST))
    }

/**
 * Back is the other way round: the page underneath is already there, and the one on top slides off it, away
 * from the edge the back gesture came from. It does not fade: the gesture can hold the page halfway, and two
 * pages of text showing through each other cannot be read. It must not reuse [pageEnter], or going back looks
 * like opening the page underneath.
 */
private fun pageLeave(still: Boolean, @NavigationEvent.SwipeEdge edge: Int): ContentTransform =
    if (still) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val towardsStart = edge == NavigationEvent.EDGE_RIGHT
        EnterTransition.None togetherWith
            slideOutHorizontally(tween(Motion.BASE)) { width -> if (towardsStart) -width else width }
    }

/** A page starts this fraction of its height below where it lands. */
private const val RISE_FROM = 40
