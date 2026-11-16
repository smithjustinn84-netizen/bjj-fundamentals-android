package dev.justinsmith.bjj.ui.learn

import dev.justinsmith.bjj.core.CardKind
import dev.justinsmith.bjj.core.Grade
import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.PositionRole
import dev.justinsmith.bjj.core.StudyItem
import dev.justinsmith.bjj.core.TransitionType
import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.design.components.Progress
import dev.justinsmith.bjj.nav.ConceptDetail
import dev.justinsmith.bjj.nav.PositionDetail
import dev.justinsmith.bjj.nav.TransitionDetail
import dev.justinsmith.bjj.ui.ViewModelTest
import dev.justinsmith.bjj.ui.common.OtherEnd
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The ViewModels of the pages behind the Learn tab, over the starter content. */
class LearnViewModelsTest : ViewModelTest() {
    @Test
    fun `a list page finds what is searched for and adds the starter items it is missing`() = runTest {
        val concepts = ConceptsViewModel(appData, t.core.items)
        val state = watch(concepts.uiState)
        val all = assertIs<ConceptsUiState.Ready>(state())
        assertTrue(all.missingStarter.isEmpty())
        assertTrue(all.search.hits.isEmpty())
        concepts.search("frames")
        assertTrue(assertIs<ConceptsUiState.Ready>(state()).search.hits.any { it.id == "frames" })

        t.core.items.deleteItem(ItemKind.Concept, "frames")
        val without = assertIs<ConceptsUiState.Ready>(state())
        assertEquals(all.concepts.size - 1, without.concepts.size)
        assertEquals(1, without.missingStarter.size)
        concepts.addStarter()
        assertEquals(all.concepts.map { it.id }, assertIs<ConceptsUiState.Ready>(state()).concepts.map { it.id })
    }

    @Test
    fun `an item's page follows what is stored and says when the item is gone`() = runTest {
        val state = watch(ConceptDetailViewModel(ConceptDetail("posture"), appData, videos).uiState)
        val ready = assertIs<ConceptDetailUiState.Ready>(state())
        assertEquals("posture", ready.concept.id)
        assertEquals(Progress.New, ready.progress)
        assertEquals("safety", ready.previous?.id)
        assertEquals("base", ready.next?.id)

        t.core.reviews.recordReview(StudyItem(CardKind.Concept, "posture"), Grade.Good)
        assertEquals(Progress.Learning, assertIs<ConceptDetailUiState.Ready>(state()).progress)
        t.core.items.deleteItem(ItemKind.Concept, "posture")
        assertEquals(ConceptDetailUiState.Missing, state())
        assertEquals(
            PositionDetailUiState.Missing,
            watch(PositionDetailViewModel(PositionDetail("nowhere"), appData, videos).uiState)(),
        )
        assertEquals(
            TransitionDetailUiState.Missing,
            watch(TransitionDetailViewModel(TransitionDetail("nothing"), appData, videos).uiState)(),
        )
    }

    @Test
    fun `a position's page lists what each side can do and opens on the side asked for`() = runTest {
        val key = PositionDetail("mount", PositionRole.Bottom)
        val ready =
            assertIs<PositionDetailUiState.Ready>(watch(PositionDetailViewModel(key, appData, videos).uiState)())
        assertEquals(PositionRole.Bottom, ready.openOn)
        assertTrue(ready.hasTechniques)
        assertTrue(ready.options.getValue(PositionRole.Bottom).all { it.type == TransitionType.Escape })
        assertTrue(ready.options.getValue(PositionRole.Top).isNotEmpty())
        assertTrue(ready.waysIn.all { it.otherEnd is OtherEnd.From })
        assertEquals(ready.options.values.sumOf { it.size }, ready.total)
    }
}
