package dev.justinsmith.bjj.nav

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

/** How the one back stack moves: what Back, the bar and a page's links each do to it. */
class RoutesTest {
    private fun stackOf(vararg keys: NavKey) = NavBackStack(*keys)

    @Test
    fun `back leaves the page on top and never the root`() {
        val stack = stackOf(TodayTab, LearnTab(), ConceptDetail("base"))
        stack.back()
        assertEquals(listOf(TodayTab, LearnTab()), stack.toList())
        stack.back()
        stack.back()
        assertEquals(listOf<NavKey>(TodayTab), stack.toList())
    }

    @Test
    fun `opening the page that is showing does not stack a second copy`() {
        val stack = stackOf(TodayTab)
        stack.push(ConceptDetail("base"))
        stack.push(ConceptDetail("base"))
        stack.push(ConceptDetail("posture"))
        assertEquals(listOf(TodayTab, ConceptDetail("base"), ConceptDetail("posture")), stack.toList())
    }

    @Test
    fun `a tab replaces everything above Today so Back from it lands on Today`() {
        val stack = stackOf(TodayTab, LearnTab(), ConceptDetail("base"))
        stack.selectTab(LogTab)
        assertEquals(listOf(TodayTab, LogTab), stack.toList())
        stack.back()
        assertEquals(listOf<NavKey>(TodayTab), stack.toList())
    }

    @Test
    fun `the Today tab clears down to the root`() {
        val stack = stackOf(TodayTab, MoreTab, Videos)
        stack.selectTab(TodayTab)
        assertEquals(listOf<NavKey>(TodayTab), stack.toList())
    }

    @Test
    fun `replacing the top page keeps Back going to what was under it`() {
        val stack = stackOf(TodayTab, LearnTab(), ConceptDetail("base"))
        stack.replaceTop(ConceptDetail("posture"))
        assertEquals(listOf(TodayTab, LearnTab(), ConceptDetail("posture")), stack.toList())
        stack.back()
        assertEquals(listOf(TodayTab, LearnTab()), stack.toList())
    }
}
