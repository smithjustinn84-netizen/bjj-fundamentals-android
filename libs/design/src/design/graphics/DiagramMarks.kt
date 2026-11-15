package dev.justinsmith.bjj.design.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import dev.justinsmith.bjj.core.Point
import dev.justinsmith.bjj.core.arrowParts

// The marks a scene is annotated with. Each is one class in the web app's `graphics.css` (`dgm-arrow`,
// `dgm-ring`, ...): the widths, dashes and opacities below are that stylesheet's, in the diagram's units.

private const val ARROW_WIDTH = 3f
private const val RING_WIDTH = 2f
private val RING_DASH = floatArrayOf(5f, 4f)
private const val GUIDE_WIDTH = 1.5f
private val GUIDE_DASH = floatArrayOf(4f, 5f)
private const val GUIDE_OPACITY = 0.5f
private const val ZONE_WIDTH = 2f
private const val ZONE_FILL_OPACITY = 0.16f
private const val BONE_WIDTH = 4f
private const val DOT_LINE_WIDTH = 1.5f
private const val VERDICT_WIDTH = 4f
private const val GHOST_OPACITY = 0.28f

/** A red arrow showing movement or force. [bend] bows it sideways, positive to the left of travel. */
fun DiagramScope.arrow(from: Point, to: Point, bend: Int = 0) {
    val parts = arrowParts(from, to, bend.toDouble())
    val path = Path().apply {
        moveTo(parts.shaft.start)
        with(parts.shaft) { quadraticTo(control.x.toFloat(), control.y.toFloat(), end.x.toFloat(), end.y.toFloat()) }
        moveTo(parts.headLeft)
        lineTo(parts.shaft.end)
        lineTo(parts.headRight)
    }
    stroke(path, art.belt, ARROW_WIDTH, StrokeCap.Round, StrokeJoin.Round)
}

/** A dashed red line along the SVG path [d]: look here. */
fun DiagramScope.ring(d: String) = stroke(svgPath(d), art.belt, RING_WIDTH, dash = RING_DASH)

/** A dashed red ellipse. */
fun DiagramScope.ring(cx: Int, cy: Int, rx: Int, ry: Int) =
    ring(ellipsePathData(cx.toFloat(), cy.toFloat(), rx.toFloat(), ry.toFloat()))

/** A dashed red circle round the thing to look at. */
fun DiagramScope.ring(cx: Int, cy: Int, r: Int) = ring(cx, cy, r, r)

/** A faint dashed ink line along the SVG path [d]: a line to measure against. */
fun DiagramScope.guide(d: String) = stroke(svgPath(d), art.ink, GUIDE_WIDTH, dash = GUIDE_DASH, alpha = GUIDE_OPACITY)

/** A faint dashed ink line from one point to another. */
fun DiagramScope.guide(x1: Int, y1: Int, x2: Int, y2: Int) = guide("M$x1 $y1 L$x2 $y2")

private fun DiagramScope.zonePath(path: Path) {
    fill(path, art.highlight, ZONE_FILL_OPACITY)
    stroke(path, art.highlight, ZONE_WIDTH, join = StrokeJoin.Round, dash = RING_DASH)
}

/** A highlighted area with corners at [points], written as an SVG polygon's: "200,122 140,195 260,195". */
fun DiagramScope.zone(points: String) = zonePath(svgPath(polygonPathData(points)))

/** A highlighted rectangle with corners rounded by [rx]. */
fun DiagramScope.zone(x: Int, y: Int, width: Int, height: Int, rx: Int) =
    zonePath(svgPath(roundRectPathData(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), rx.toFloat())))

/** A bone doing structural work, along the SVG path [d]: a frame, an underhook. */
fun DiagramScope.bone(d: String) = stroke(svgPath(d), art.highlight, BONE_WIDTH, StrokeCap.Round, StrokeJoin.Round)

/** A red spot marking a point of contact. */
fun DiagramScope.dot(cx: Int, cy: Int, r: Int) =
    disc(Offset(cx.toFloat(), cy.toFloat()), r.toFloat(), fill = art.belt, line = art.paper, lineWidth = DOT_LINE_WIDTH)

/** A tick, along the SVG path [d]: do this. */
fun DiagramScope.good(d: String) = stroke(svgPath(d), art.giDark, VERDICT_WIDTH, StrokeCap.Round, StrokeJoin.Round)

/** A cross, along the SVG path [d]: not this. */
fun DiagramScope.bad(d: String) = stroke(svgPath(d), art.belt, VERDICT_WIDTH, StrokeCap.Round, StrokeJoin.Round)

/** Draws [block] faintly: where someone was before they moved. */
fun DiagramScope.ghost(block: DiagramScope.() -> Unit) = layer(GHOST_OPACITY) { block() }
