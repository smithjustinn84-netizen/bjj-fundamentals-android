package dev.justinsmith.bjj.design.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * What a scene draws with: a canvas already set up in the diagram's own units, and the art palette. These are
 * the few strokes everything is made of; the marks a scene uses by name (`figure`, `arrow`, `ring`, ...) are
 * extensions built on them, one per mark in the web app's `graphics.css`. Caps and joins default to SVG's.
 */
class DiagramScope internal constructor(
    private val canvas: DrawScope,
    val art: ArtPalette,
    private val viewBox: ViewBox,
) {
    /** Paints the inside of [path]. */
    fun fill(path: Path, color: Color, alpha: Float = 1f) {
        canvas.drawPath(path, color, alpha)
    }

    /** Draws a line of [width] along [path]; [dash] is lengths of line and gap in turn. */
    fun stroke(
        path: Path,
        color: Color,
        width: Float,
        cap: StrokeCap = StrokeCap.Butt,
        join: StrokeJoin = StrokeJoin.Miter,
        dash: FloatArray? = null,
        alpha: Float = 1f,
    ) {
        val effect = dash?.let { PathEffect.dashPathEffect(it) }
        canvas.drawPath(path, color, alpha, Stroke(width, cap = cap, join = join, pathEffect = effect))
    }

    /** A filled circle with a line of [lineWidth] round it, centred on its edge. */
    fun disc(centre: Offset, radius: Float, fill: Color, line: Color, lineWidth: Float) {
        canvas.drawCircle(fill, radius, centre)
        canvas.drawCircle(line, radius, centre, style = Stroke(lineWidth))
    }

    /**
     * Draws [block] as one sheet and then lays it down at [alpha], so that shapes overlapping inside it do not
     * show through each other.
     */
    fun layer(alpha: Float, block: () -> Unit) {
        val bounds = Rect(viewBox.x, viewBox.y, viewBox.x + viewBox.width, viewBox.y + viewBox.height)
        canvas.drawIntoCanvas {
            it.saveLayer(bounds, Paint().apply { this.alpha = alpha })
            block()
            it.restore()
        }
    }
}
