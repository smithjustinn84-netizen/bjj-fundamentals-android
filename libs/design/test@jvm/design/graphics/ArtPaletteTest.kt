package dev.justinsmith.bjj.design.graphics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtPaletteTest {
    @Test
    fun `dimming takes the paper down as the web filter does`() {
        // brightness(0.8) contrast(1.06) saturate(0.94) on #f5efdd, worked by hand.
        assertEquals(Color(0xFFC8C3B5).toArgb(), dimForDark(PlainArtPalette.paper).toArgb())
    }

    @Test
    fun `dimming leaves black nearly black and keeps white off white`() {
        // Black: brightness leaves 0 and contrast pushes it below 0, where it is clamped.
        assertEquals(Color.Black.toArgb(), dimForDark(Color.Black).toArgb())
        // White: 0.8 after brightness, then (0.8 - 0.5) * 1.06 + 0.5 = 0.818; a grey has no saturation to lose.
        assertEquals(Color(0xFFD1D1D1).toArgb(), dimForDark(Color.White).toArgb())
    }

    @Test
    fun `dimming pulls a colour towards its own grey`() {
        val dimmed = dimForDark(PlainArtPalette.belt)
        val plain = PlainArtPalette.belt
        assertEquals(true, dimmed.red < plain.red)
        assertEquals(true, dimmed.red - dimmed.blue < plain.red - plain.blue)
    }

    @Test
    fun `dimming keeps how see-through a colour is`() {
        assertEquals(PlainArtPalette.shadow.alpha, dimForDark(PlainArtPalette.shadow).alpha)
    }

    @Test
    fun `the dimmed palette is every colour dimmed once`() {
        assertEquals(PlainArtPalette.map(::dimForDark), DimmedArtPalette)
        assertEquals(dimForDark(PlainArtPalette.giDark), DimmedArtPalette.giDark)
    }
}
