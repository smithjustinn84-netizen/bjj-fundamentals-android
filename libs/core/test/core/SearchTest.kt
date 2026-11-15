package dev.justinsmith.bjj.core

import dev.justinsmith.bjj.core.testing.concept
import dev.justinsmith.bjj.core.testing.position
import dev.justinsmith.bjj.core.testing.transition
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTest {
    private val lib = buildLibrary(
        listOf(concept(id = "frames", name = "Frames", summary = "Hold space with bones")),
        listOf(position(id = "mount", name = "Mount", summary = "Sitting on their chest")),
        listOf(
            transition("upa", "Bridge and roll escape", TransitionType.Escape, "mount", steps = listOf("Trap the arm")),
            transition("armbar", "Armbar from mount", TransitionType.Submission, "mount", notes = "Coach says frames"),
        ),
        emptyList(),
    )

    private fun ids(query: String, limit: Int = 10) = searchLibrary(lib, query, limit).map { it.id }

    @Test
    fun `finds nothing for an empty query`() {
        assertEquals(emptyList(), ids(""))
        assertEquals(emptyList(), ids("   "))
    }

    @Test
    fun `matches names ignoring case and says what each hit is`() {
        val hit = SearchHit(ItemKind.Transition, "armbar", "Armbar from mount", "Submission from Mount")
        assertEquals(listOf(hit), searchLibrary(lib, "ARMBAR", 10))
    }

    @Test
    fun `puts name matches before matches in the text`() {
        // "Mount" is the position's name, part of one technique's name, and only where the other starts.
        assertEquals(listOf("mount", "armbar", "upa"), ids("mount"))
        assertEquals(listOf("frames", "armbar"), ids("frames"))
    }

    @Test
    fun `needs every word anywhere in the item`() {
        assertEquals(listOf("upa"), ids("escape trap"))
        assertEquals(emptyList(), ids("escape frames"))
    }

    @Test
    fun `splits words on any kind of space`() {
        assertEquals(listOf("upa"), ids("escape trap"))
        assertEquals(listOf("upa"), ids("escape　trap"))
    }

    @Test
    fun `stops at the limit`() {
        assertEquals(listOf("mount", "armbar"), ids("mount", 2))
    }
}
