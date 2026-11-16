package dev.justinsmith.bjj.ui.videos

import dev.justinsmith.bjj.core.ItemKind
import dev.justinsmith.bjj.core.LinkedKind
import dev.justinsmith.bjj.core.services.Snapshot
import dev.justinsmith.bjj.core.services.VideoInput
import dev.justinsmith.bjj.core.testing.TestCore
import dev.justinsmith.bjj.core.testing.testCore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the bookmarks page is told to draw, from the starter library and what a test then stores. */
class VideosUiStateTest {
    private val t: TestCore = testCore()

    private suspend fun snapshot(): Snapshot = t.core.queries.snapshot()

    @Test
    fun `bookmarks name what they are about and are found by title note or that name`() = runTest {
        val starter = videosUiState(snapshot(), query = "").total
        val note = "frame the xiphoid"
        t.core.videos.add(VideoInput("Elbow escape details", "abcdefghijk", 0, LinkedKind.Position, "mount", note))
        t.core.videos.add(VideoInput("Open mat", "abcdefghijk", 0, LinkedKind.None, "", ""))
        val all = videosUiState(snapshot(), query = "")
        assertEquals(starter + 2, all.total)
        assertEquals(all.total, all.shown.size)
        assertNull(all.shown.first { it.video.title == "Open mat" }.about)
        assertTrue(all.about.any { it.item.kind == ItemKind.Transition })

        val byNote = videosUiState(snapshot(), query = " XIPHOID ")
        assertEquals(listOf("Elbow escape details"), byNote.shown.map { it.video.title })
        assertEquals("Mount", byNote.shown.single().about?.name)
        assertTrue(videosUiState(snapshot(), query = "mount").shown.any { it.video.title == "Elbow escape details" })
        assertEquals(all.total, videosUiState(snapshot(), query = "zzzz").total)
        assertTrue(videosUiState(snapshot(), query = "zzzz").shown.isEmpty())
    }
}
