package dev.justinsmith.bjj.design.graphics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import dev.justinsmith.bjj.design.Easings
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion

// The brush circle, in the units of the web app's drawing (EmptyState.tsx): a square of 100.
private const val ENSO_SIZE = 100f
private const val ENSO_PATH = "M72 18C52 6 24 14 15 38c-9 25 6 50 32 53 27 3 46-20 40-45-2-9-7-16-13-21"
private const val ENSO_LINE = 5f
private const val ENSO_OPACITY = 0.8f

/**
 * How much line the brush lays down by the end of the animation. The web app hides the stroke behind a dash
 * this long and slides it away, so the circle (a little shorter) closes just before the animation ends.
 */
private const val ENSO_BRUSH_LENGTH = 260f

/**
 * A brushed ink circle that paints itself in, for a screen with nothing on it yet. With animations off it is
 * already painted. It fills the width it is given; decorative.
 */
@Composable
fun Enso(modifier: Modifier = Modifier) {
    val color = BjjTheme.palette.text
    val still = reducedMotion()
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(still) {
        if (still) progress.snapTo(1f) else progress.animateTo(1f, tween(Motion.DRAW_SLOW, easing = Easings.out))
    }
    val circle = remember { svgPath(ENSO_PATH) }
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val measure = PathMeasure().apply { setPath(circle, forceClosed = false) }
        val painted = Path()
        measure.getSegment(0f, minOf(measure.length, ENSO_BRUSH_LENGTH * progress.value), painted)
        scale(size.width / ENSO_SIZE, pivot = Offset.Zero) {
            drawPath(painted, color, ENSO_OPACITY, Stroke(ENSO_LINE, cap = StrokeCap.Round))
        }
    }
}
