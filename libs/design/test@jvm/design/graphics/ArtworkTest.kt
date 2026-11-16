@file:OptIn(ExperimentalTestApi::class)

package dev.justinsmith.bjj.design.graphics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.BeltRank
import dev.justinsmith.bjj.core.MAX_STRIPES
import dev.justinsmith.bjj.core.seed.starterContent
import dev.justinsmith.bjj.design.theme.BjjTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val PICTURE = "picture"

/** Wide enough that the frame's line and round corners are well clear of [INSIDE_FRAME]. */
private val PICTURE_WIDTH = 320.dp

/** A pixel inside the frame's top left corner, where every scene leaves the paper bare. */
private const val INSIDE_FRAME = 8

class ArtworkTest {
    private val starter = starterContent(now = 0)

    /** Draws each of [ids] with [artwork], in light and in dark, and checks it came out on the right paper. */
    private fun drawsOnPaper(ids: List<String>, artwork: @Composable (id: String, modifier: Modifier) -> Unit) {
        for (dark in listOf(false, true)) {
            runComposeUiTest {
                var id by mutableStateOf(ids.first())
                setContent {
                    BjjTheme(dark) { Box(Modifier.width(PICTURE_WIDTH)) { artwork(id, Modifier.testTag(PICTURE)) } }
                }
                val paper = if (dark) DimmedArtPalette.paper else PlainArtPalette.paper
                for (next in ids) {
                    id = next
                    waitForIdle()
                    val pixels = onNodeWithTag(PICTURE).captureToImage().toPixelMap()
                    assertEquals(paper.toArgb(), pixels[INSIDE_FRAME, INSIDE_FRAME].toArgb(), "$next dark=$dark")
                    val inks = (0 until pixels.width).map { pixels[it, pixels.height / 2] }.toSet()
                    assertTrue(inks.size > 1, "$next draws something across its middle")
                }
            }
        }
    }

    @Test
    fun `every starter concept has a picture that draws in light and dark`() {
        val ids = starter.concepts.map { it.id }
        assertEquals(emptyList(), ids.filterNot(::hasConceptArtwork))
        drawsOnPaper(ids) { id, modifier -> ConceptArtwork(id, modifier) }
    }

    @Test
    fun `every starter position has a picture that draws in light and dark`() {
        val ids = starter.positions.map { it.id }
        assertEquals(emptyList(), ids.filterNot(::hasPositionArtwork))
        drawsOnPaper(ids) { id, modifier -> PositionArtwork(id, modifier) }
    }

    @Test
    fun `an item the user added has no picture and draws nothing`() = runComposeUiTest {
        assertFalse(hasConceptArtwork("my-own-concept"))
        assertFalse(hasPositionArtwork("my-own-position"))
        setContent {
            BjjTheme(dark = false) {
                Box(Modifier.testTag(PICTURE)) {
                    ConceptArtwork("my-own-concept")
                    PositionArtwork("my-own-position")
                }
            }
        }
        val bounds = onNodeWithTag(PICTURE).fetchSemanticsNode().size
        assertEquals(0, bounds.width)
        assertEquals(0, bounds.height)
    }

    @Test
    fun `a concept picture is 16 by 9 and a position picture 4 by 3`() = runComposeUiTest {
        setContent {
            BjjTheme(dark = false) {
                Box(Modifier.width(PICTURE_WIDTH)) {
                    ConceptArtwork("base", Modifier.testTag("concept"))
                    PositionArtwork("mount", Modifier.testTag("position"))
                }
            }
        }
        val concept = onNodeWithTag("concept").fetchSemanticsNode().size
        val position = onNodeWithTag("position").fetchSemanticsNode().size
        assertEquals(320 to 180, concept.width to concept.height)
        assertEquals(320 to 240, position.width to position.height)
    }

    @Test
    fun `the belt and the marks draw for every rank in light and dark`() {
        for (dark in listOf(false, true)) {
            runComposeUiTest {
                var rank by mutableStateOf(BeltRank.White)
                var stripes by mutableIntStateOf(0)
                setContent {
                    BjjTheme(dark) {
                        Box(Modifier.testTag(PICTURE)) {
                            BeltGraphic(rank, stripes)
                            BrandMark()
                            Enso(Modifier.width(PICTURE_WIDTH))
                        }
                    }
                }
                for (next in BeltRank.entries) {
                    rank = next
                    stripes = (stripes + 1) % (MAX_STRIPES + 1)
                    waitForIdle()
                    onNodeWithTag(PICTURE).captureToImage()
                }
            }
        }
    }
}
