package dev.justinsmith.bjj.nav

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.situationPositionId
import kotlinx.serialization.Serializable

// Every destination is a NavKey. A new one must also be registered in `navConfiguration` (App.kt) and given an
// `entry<…>` in the entry provider, or it cannot be saved or shown.

/** One of the five destinations of the bar, the same on every window size. */
sealed interface Tab : NavKey

/** Today, the first tab and the root of the back stack. */
@Serializable
data object TodayTab : Tab

/** The pages behind Learn, picked with the switch at the top of each. Techniques are reached from positions. */
@Serializable
enum class LearnSection { Concepts, Positions, Lessons, BlueBelt }

/** Learn, on one of its pages. */
@Serializable
data class LearnTab(val section: LearnSection = LearnSection.Concepts) : Tab

/**
 * Review: what is ready, and the way into a session. [deck] is the key of the deck to open on and [lessonId]
 * the lesson whose cards to study instead; both null for everything.
 */
@Serializable
data class ReviewTab(val deck: String? = null, val lessonId: String? = null) : Tab

/** The training log. */
@Serializable
data object LogTab : Tab

/** More: videos, new items, appearance, backup. */
@Serializable
data object MoreTab : Tab

/** A review session, full screen: the bar goes while it runs. [newCards] is how many new cards it may bring in. */
@Serializable
data class ReviewSession(val deck: String, val lessonId: String?, val newCards: Int) : NavKey

@Serializable
data class ConceptDetail(val id: String) : NavKey

/** A position's page, opened on [side] when one is given. */
@Serializable
data class PositionDetail(val id: String, val side: PositionRole? = null) : NavKey

@Serializable
data class TransitionDetail(val id: String) : NavKey

/** Every video bookmark. */
@Serializable
data object Videos : NavKey

@Serializable
data object Backup : NavKey

/**
 * The form for one item: a new one when [id] is null. [fromPositionId] is the position a new technique should
 * start from.
 */
@Serializable
data class Editor(val kind: ItemKind, val id: String? = null, val fromPositionId: String? = null) : NavKey

/** The bar's tabs, in its order, each as it opens when tapped. */
val TABS: List<Tab> = listOf(TodayTab, LearnTab(), ReviewTab(), LogTab, MoreTab)

/** Whether two tabs are the same destination of the bar, whatever page of it each is on. */
fun Tab.sameDestination(other: Tab): Boolean = this::class == other::class

/** The page for one item. */
fun itemKey(kind: ItemKind, id: String): NavKey = when (kind) {
    ItemKind.Concept -> ConceptDetail(id)
    ItemKind.Position -> PositionDetail(id)
    ItemKind.Transition -> TransitionDetail(id)
}

/** The page that says the most about a study card: the item's own, or for a situation its position's. */
fun studyItemKey(item: StudyItem): NavKey = when (item.kind) {
    CardKind.Concept -> ConceptDetail(item.id)
    CardKind.Position -> PositionDetail(item.id)
    CardKind.Transition -> TransitionDetail(item.id)
    CardKind.Situation -> PositionDetail(situationPositionId(item.id))
}

/** The Learn page an item of this kind is listed on. */
fun listKey(kind: ItemKind): Tab = when (kind) {
    ItemKind.Concept -> LearnTab(LearnSection.Concepts)
    ItemKind.Position, ItemKind.Transition -> LearnTab(LearnSection.Positions)
}

/** Shows [tab]: Today is the root, so it pops back to it; any other tab replaces what is above Today. */
fun NavBackStack<NavKey>.selectTab(tab: Tab) {
    while (size > 1) removeLastOrNull()
    if (tab != TodayTab) add(tab)
}

/** Opens [key] over what is showing, unless it is showing already: a double tap must not stack two. */
fun NavBackStack<NavKey>.push(key: NavKey) {
    if (lastOrNull() != key) add(key)
}

/** Leaves the page on top. The root stays. */
fun NavBackStack<NavKey>.back() {
    if (size > 1) removeLastOrNull()
}

/** Puts [key] where the page on top is: Back from it goes to what was under that page. */
fun NavBackStack<NavKey>.replaceTop(key: NavKey) {
    if (size > 1) removeLastOrNull()
    push(key)
}

/** The tab whose page is showing, or was under the pages opened since: the one the bar keeps lit. */
fun List<NavKey>.currentTab(): Tab = lastOrNull { it is Tab } as? Tab ?: TodayTab
