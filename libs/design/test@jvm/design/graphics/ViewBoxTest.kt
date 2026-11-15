package dev.justinsmith.bjj.design.graphics

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewBoxTest {
    @Test
    fun `the wide window is 16 by 9 and the tall one 4 by 3`() {
        assertEquals(16f / 9f, DiagramShape.Wide.viewBox.aspectRatio)
        assertEquals(4f / 3f, DiagramShape.Tall.viewBox.aspectRatio)
    }

    @Test
    fun `a canvas of the same shape shows the view box corner to corner`() {
        val mapping = viewBoxMapping(DiagramShape.Wide.viewBox, canvasWidth = 640f, canvasHeight = 360f)

        assertEquals(2f, mapping.scale)
        assertEquals(Offset(0f, 0f), mapping.toCanvas(40f, 28f))
        assertEquals(Offset(640f, 360f), mapping.toCanvas(360f, 208f))
        // The mat line is 12 above the bottom of the window.
        assertEquals(Offset(320f, 336f), mapping.toCanvas(200f, 196f))
    }

    @Test
    fun `a canvas too wide centres the view box across it`() {
        val mapping = viewBoxMapping(DiagramShape.Tall.viewBox, canvasWidth = 400f, canvasHeight = 195f)

        assertEquals(1f, mapping.scale)
        assertEquals(Offset(70f, 0f), mapping.toCanvas(70f, 13f))
        assertEquals(Offset(330f, 195f), mapping.toCanvas(330f, 208f))
    }

    @Test
    fun `a canvas too tall centres the view box down it`() {
        val mapping = viewBoxMapping(DiagramShape.Tall.viewBox, canvasWidth = 130f, canvasHeight = 197.5f)

        assertEquals(0.5f, mapping.scale)
        assertEquals(Offset(0f, 50f), mapping.toCanvas(70f, 13f))
        assertEquals(Offset(130f, 147.5f), mapping.toCanvas(330f, 208f))
    }
}
