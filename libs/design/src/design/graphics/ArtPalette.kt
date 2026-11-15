package dev.justinsmith.bjj.design.graphics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.justinsmith.bjj.design.theme.BjjTheme

/**
 * The colours every diagram is drawn in (`--art-*` in the web app's `tokens.css`). Diagrams share one sheet of
 * paper, so there is one value for both themes; the dark theme takes the whole sheet down a little instead.
 */
@Immutable
data class ArtPalette(
    val paper: Color,
    val ink: Color,
    val giLight: Color,
    /** The light gi's limbs on the far side of the body. */
    val giLightFar: Color,
    val giDark: Color,
    val giDarkFar: Color,
    val skin: Color,
    val hair: Color,
    val belt: Color,
    val mat: Color,
    /** See-through: it darkens the paper under a scene. */
    val shadow: Color,
    val highlight: Color,
) {
    /** The same palette with [change] applied to every colour. */
    internal fun map(change: (Color) -> Color) = ArtPalette(
        paper = change(paper),
        ink = change(ink),
        giLight = change(giLight),
        giLightFar = change(giLightFar),
        giDark = change(giDark),
        giDarkFar = change(giDarkFar),
        skin = change(skin),
        hair = change(hair),
        belt = change(belt),
        mat = change(mat),
        shadow = change(shadow),
        highlight = change(highlight),
    )
}

val PlainArtPalette = ArtPalette(
    paper = Color(0xFFF5EFDD),
    ink = Color(0xFF26221D),
    giLight = Color(0xFFFBF7EA),
    giLightFar = Color(0xFFE2DAC4),
    giDark = Color(0xFF4A6594),
    giDarkFar = Color(0xFF384F78),
    skin = Color(0xFFE3BC98),
    hair = Color(0xFF2F2924),
    belt = Color(0xFFB8322A),
    mat = Color(0xFFCDC3AA),
    shadow = Color(red = 90, green = 70, blue = 40, alpha = 36),
    highlight = Color(0xFFD08A1C),
)

// The web app's `filter: brightness(0.8) contrast(1.06) saturate(0.94)` on a framed drawing in the dark theme.
private const val DIM_BRIGHTNESS = 0.8f
private const val DIM_CONTRAST = 1.06f
private const val DIM_SATURATION = 0.94f

/** Contrast stretches a channel away from the middle of its range. */
private const val MID_CHANNEL = 0.5f

// How much each channel counts towards a colour's brightness in the CSS `saturate` filter.
private const val LUMA_RED = 0.213f
private const val LUMA_GREEN = 0.715f
private const val LUMA_BLUE = 0.072f

private fun clampChannel(value: Float) = value.coerceIn(0f, 1f)

/**
 * [color] as it looks on a drawing in the dark theme: cream paper glares on a dark page. It is the web app's
 * filter worked out once for the colour, each step clamped as the browser clamps it, so no layer is needed to
 * dim a whole drawing. Every step is a weighted mix, so a see-through colour dimmed and then laid over dimmed
 * paper comes out as the filter would have left it; alpha is untouched.
 */
fun dimForDark(color: Color): Color {
    val contrasted = { channel: Float ->
        clampChannel((clampChannel(channel * DIM_BRIGHTNESS) - MID_CHANNEL) * DIM_CONTRAST + MID_CHANNEL)
    }
    val red = contrasted(color.red)
    val green = contrasted(color.green)
    val blue = contrasted(color.blue)
    val luma = LUMA_RED * red + LUMA_GREEN * green + LUMA_BLUE * blue
    val saturated = { channel: Float -> clampChannel(luma + (channel - luma) * DIM_SATURATION) }
    return Color(red = saturated(red), green = saturated(green), blue = saturated(blue), alpha = color.alpha)
}

val DimmedArtPalette = PlainArtPalette.map(::dimForDark)

/** The art palette for the current theme: as printed in the light theme, dimmed in the dark one. */
@Composable
@ReadOnlyComposable
fun artPalette(): ArtPalette = if (BjjTheme.palette.isDark) DimmedArtPalette else PlainArtPalette
