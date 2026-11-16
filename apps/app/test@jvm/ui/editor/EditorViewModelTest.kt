package dev.justinsmith.bjj.ui.editor

import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.id
import dev.justinsmith.bjj.core.services.ConceptInput
import dev.justinsmith.bjj.nav.Editor
import dev.justinsmith.bjj.ui.ViewModelTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The editor's ViewModel over the starter content. */
class EditorViewModelTest : ViewModelTest() {
    @Test
    fun `the editor leaves once an item is saved or deleted and never calls a deleted item missing`() = runTest {
        val missing = EditorViewModel(Editor(ItemKind.Concept, "nothing"), appData, t.core.items)
        assertEquals(EditorUiState.Missing, watch(missing.uiState)())

        val adding = EditorViewModel(Editor(ItemKind.Concept), appData, t.core.items)
        val added = watch(adding.uiState)
        val fields = assertIs<EditorFields.ConceptFields>(assertIs<EditorUiState.Ready>(added()).fields)
        assertNull(fields.stored)
        adding.saveConcept(ConceptInput("Grips", "", emptyList(), "", fields.nextOrder, ""))
        val saved = assertIs<EditorOutcome.Saved>(assertIs<EditorUiState.Ready>(added()).outcome)
        assertEquals("Grips", t.storage.concepts.get(saved.id)?.name)

        val editing = EditorViewModel(Editor(ItemKind.Position, "mount"), appData, t.core.items)
        val edited = watch(editing.uiState)
        val before = assertIs<EditorUiState.Ready>(edited())
        assertEquals("Mount", before.name)
        assertTrue(before.cascade > 0)
        editing.delete()
        assertEquals(EditorOutcome.Deleted, assertIs<EditorUiState.Ready>(edited()).outcome)
        assertNull(t.storage.positions.get("mount"))
    }
}
