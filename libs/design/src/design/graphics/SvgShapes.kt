package dev.justinsmith.bjj.design.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import dev.justinsmith.bjj.core.Point

// The web app's drawings are SVG. Its paths are kept as their `d` strings, and its circles and rectangles are
// turned into the path SVG itself defines for them, so a dashed outline starts where the browser starts it.

/** The path for an SVG `d` string. */
internal fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** An SVG `<ellipse>` as path data: from its rightmost point, clockwise. */
internal fun ellipsePathData(cx: Float, cy: Float, rx: Float, ry: Float): String {
    val across = rx + rx
    return "M${cx + rx} $cy a$rx $ry 0 1 1 ${-across} 0 a$rx $ry 0 1 1 $across 0 z"
}

/** An SVG `<circle>` as path data. */
internal fun circlePathData(cx: Float, cy: Float, r: Float): String = ellipsePathData(cx, cy, r, r)

/** An SVG `<rect>` with corners rounded by [r] as path data: from the left end of its top edge, clockwise. */
internal fun roundRectPathData(x: Float, y: Float, width: Float, height: Float, r: Float): String {
    val across = width - 2 * r
    val down = height - 2 * r
    val corner = "a$r $r 0 0 1"
    return "M${x + r} $y h$across $corner $r $r v$down $corner ${-r} $r h${-across} $corner ${-r} ${-r} " +
        "v${-down} $corner $r ${-r} z"
}

/** An SVG `<polygon>`'s `points` ("200,122 140,195 260,195") as path data. */
internal fun polygonPathData(points: String): String =
    points.trim().split(' ').joinToString(separator = " L", prefix = "M", postfix = " z") { it.replace(',', ' ') }

// Core's geometry is in doubles, as the web app's is; a canvas takes floats.

internal fun Point.toOffset() = Offset(x.toFloat(), y.toFloat())

internal fun Path.moveTo(point: Point) = moveTo(point.x.toFloat(), point.y.toFloat())

internal fun Path.lineTo(point: Point) = lineTo(point.x.toFloat(), point.y.toFloat())
