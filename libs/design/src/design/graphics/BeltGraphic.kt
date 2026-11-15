package dev.justinsmith.bjj.design.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.BeltRank
import dev.justinsmith.bjj.core.MAX_STRIPES
import dev.justinsmith.bjj.design.Easings
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjPalette
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion

// The tied belt, in the units of the web app's drawing (Belt.tsx): 200 wide and 84 tall.
private const val BELT_WIDTH = 200f
private const val BELT_HEIGHT = 84f

/** The widest the belt is shown, whatever room it is given. */
private val BELT_MAX_WIDTH = 200.dp

/** The two tails hanging from the knot. */
private const val TAILS_PATH = "M96 36 76 74l15 6 14-38zM104 36l20 38-15 6-14-38z"

/** The creases across the knot. */
private const val FOLD_PATH = "M94 24c4 3 8 3 12 0M94 38c4-3 8-3 12 0"
private const val FOLD_OPACITY = 0.6f

/** The line round every piece of cloth. */
private const val CLOTH_LINE = 1.5f

/** The belt's band: stripes and the rank bar cover its full height. */
private const val BAND_X = 4f
private const val BAND_Y = 22f
private const val BAND_WIDTH = 192f
private const val BAND_HEIGHT = 18f
private const val BAND_CORNER = 3f

private const val BAR_X = 146f
private const val BAR_WIDTH = 38f

/** Where the first stripe sits on the rank bar, how far apart stripes are, and how wide one is. */
private const val FIRST_STRIPE_X = 153f
private const val STRIPE_SPACING = 7f
private const val STRIPE_WIDTH = 3f

private const val KNOT_X = 88f
private const val KNOT_Y = 17f
private const val KNOT_WIDTH = 24f
private const val KNOT_HEIGHT = 28f
private const val KNOT_CORNER = 6f

/** How small something is when it starts to pop in. */
private const val POP_FROM_SCALE = 0.85f

/** Each rank's cloth; the rank bar is black except on the black belt, where it is red. */
private fun clothColor(rank: BeltRank, palette: BjjPalette): Color = when (rank) {
    BeltRank.White -> palette.beltWhite
    BeltRank.Blue -> palette.beltBlue
    BeltRank.Purple -> palette.beltPurple
    BeltRank.Brown -> palette.beltBrown
    BeltRank.Black -> palette.beltBlack
}

private fun barColor(rank: BeltRank, palette: BjjPalette): Color = when (rank) {
    BeltRank.White, BeltRank.Blue, BeltRank.Purple, BeltRank.Brown -> palette.beltBlack
    BeltRank.Black -> palette.belt
}

/**
 * How far something has popped in, from 0 (not there) to 1 (in place), passing a little over 1 on the way.
 * It starts when [shown] becomes true, after [delayMillis]; with animations off it is in place at once.
 */
@Composable
private fun popIn(shown: Boolean, durationMillis: Int, delayMillis: Int = 0): State<Float> {
    val still = reducedMotion()
    val progress = remember { Animatable(if (shown && still) 1f else 0f) }
    LaunchedEffect(shown, still) {
        when {
            !shown -> progress.snapTo(0f)
            still -> progress.snapTo(1f)
            else -> progress.animateTo(1f, tween(durationMillis, delayMillis, Easings.spring))
        }
    }
    return progress.asState()
}

/** Draws [block] growing from [POP_FROM_SCALE] about the middle of [box] and fading in, [progress] of the way. */
private fun DrawScope.popped(progress: Float, box: Rect, block: DrawScope.(alpha: Float) -> Unit) {
    val alpha = progress.coerceIn(0f, 1f)
    if (alpha == 0f) return
    scale(POP_FROM_SCALE + (1 - POP_FROM_SCALE) * progress, pivot = box.center) { block(alpha) }
}

private fun DrawScope.cloth(path: Path, color: Color, outline: Color, alpha: Float = 1f) {
    drawPath(path, color, alpha)
    drawPath(path, outline, alpha, Stroke(CLOTH_LINE, join = StrokeJoin.Round))
}

/**
 * A tied belt in the colour of [rank], with [stripes] earned stripes on its rank bar (at most [MAX_STRIPES]).
 * The knot and each new stripe pop in. Decorative: say what it shows through [modifier].
 */
@Composable
fun BeltGraphic(rank: BeltRank, stripes: Int, modifier: Modifier = Modifier) {
    val palette = BjjTheme.palette
    val knot = popIn(shown = true, durationMillis = Motion.SLOW)
    val stripePops = List(MAX_STRIPES) { index ->
        popIn(shown = index < stripes, durationMillis = Motion.BASE, delayMillis = Motion.SLOW)
    }
    Canvas(modifier.widthIn(max = BELT_MAX_WIDTH).fillMaxWidth().aspectRatio(BELT_WIDTH / BELT_HEIGHT)) {
        scale(size.width / BELT_WIDTH, pivot = Offset.Zero) {
            val clothColor = clothColor(rank, palette)
            cloth(svgPath(TAILS_PATH), clothColor, palette.beltOutline)
            val band = roundRectPathData(BAND_X, BAND_Y, BAND_WIDTH, BAND_HEIGHT, BAND_CORNER)
            cloth(svgPath(band), clothColor, palette.beltOutline)
            drawRect(barColor(rank, palette), Offset(BAR_X, BAND_Y), Size(BAR_WIDTH, BAND_HEIGHT))
            stripePops.forEachIndexed { index, pop ->
                val stripe = Rect(
                    Offset(FIRST_STRIPE_X + index * STRIPE_SPACING, BAND_Y),
                    Size(STRIPE_WIDTH, BAND_HEIGHT),
                )
                popped(pop.value, stripe) { alpha -> drawRect(palette.beltWhite, stripe.topLeft, stripe.size, alpha) }
            }
            val knotPath = svgPath(roundRectPathData(KNOT_X, KNOT_Y, KNOT_WIDTH, KNOT_HEIGHT, KNOT_CORNER))
            val knotBox = Rect(Offset(KNOT_X, KNOT_Y), Size(KNOT_WIDTH, KNOT_HEIGHT))
            popped(knot.value, knotBox) { alpha -> cloth(knotPath, clothColor, palette.beltOutline, alpha) }
            val fold = Stroke(CLOTH_LINE, cap = StrokeCap.Round)
            drawPath(svgPath(FOLD_PATH), palette.beltOutline, FOLD_OPACITY, fold)
        }
    }
}
