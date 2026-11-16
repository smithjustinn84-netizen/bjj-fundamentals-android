package dev.justinsmith.bjj.ui

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.CoreEnv
import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import dev.justinsmith.bjj.di.appModules
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.nav.LearnSection
import dev.justinsmith.bjj.nav.LearnTab
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.ReviewSession
import dev.justinsmith.bjj.nav.ReviewTab
import dev.justinsmith.bjj.nav.TodayTab
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.nav.currentTab
import dev.justinsmith.bjj.nav.sameDestination
import dev.justinsmith.bjj.nav.studyItemKey
import dev.justinsmith.bjj.platform.Links
import dev.justinsmith.bjj.ui.home.formatDay
import dev.justinsmith.bjj.ui.home.todayLabel
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What holds the screens together: the container, the back stack's tabs and how dates are written. */
@OptIn(KoinExperimentalAPI::class)
class AppWiringTest {
    private val t: TestCore = testCore()

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

    @Test
    fun `the bar keeps a tab lit on any page of it and under pages opened from it`() {
        val stack = listOf(TodayTab, LearnTab(LearnSection.Lessons), ConceptDetail("base"))
        assertTrue(stack.currentTab().sameDestination(LearnTab()))
        assertFalse(stack.currentTab().sameDestination(ReviewTab()))
        assertEquals(TodayTab, listOf(TodayTab).currentTab())
        assertEquals(PositionDetail("mount"), studyItemKey(StudyItem(CardKind.Situation, "mount:bottom")))
    }

    @Test
    fun `dates are written in words`() {
        // Noon on Saturday 14 November 2026.
        assertEquals("Saturday 14 November", todayLabel(t.clock.now, LocalZone.Utc))
        assertEquals("Sat 14 Nov 2026", formatDay("2026-11-14"))
        assertEquals("Thu 1 Jan 1970", formatDay("1970-01-01"))
        assertEquals("not a day", formatDay("not a day"))
    }
}
