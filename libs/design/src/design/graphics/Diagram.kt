package dev.justinsmith.bjj.design.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes

/** The part of a drawing's own space that is shown, as an SVG `viewBox`: its top left corner and its size. */
data class ViewBox(val x: Float, val y: Float, val width: Float, val height: Float) {
    val aspectRatio: Float get() = width / height
}

/** How a [ViewBox] lands on a canvas: a point in the drawing goes to `point * scale + offset`. */
data class ViewBoxMapping(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun toCanvas(x: Float, y: Float): Offset = Offset(x * scale + offsetX, y * scale + offsetY)
}

/**
 * Fits [viewBox] inside a canvas of the given size, as large as it goes without stretching, and centres it: what
 * a browser does with an SVG's `viewBox`.
 */
fun viewBoxMapping(viewBox: ViewBox, canvasWidth: Float, canvasHeight: Float): ViewBoxMapping {
    val scale = minOf(canvasWidth / viewBox.width, canvasHeight / viewBox.height)
    return ViewBoxMapping(
        scale = scale,
        offsetX = (canvasWidth - viewBox.width * scale) / 2 - viewBox.x * scale,
        offsetY = (canvasHeight - viewBox.height * scale) / 2 - viewBox.y * scale,
    )
}

/**
 * Every scene is drawn in the same space with the mat at y = 196 and the action centred on x = 200, so figures
 * are one size everywhere. [Wide] is a 16:9 window on it, for concepts; [Tall] is a closer 4:3 window for
 * position pictures. [matFromAbove] is the mat seen from overhead, sized to sit just inside the window.
 */
enum class DiagramShape(val viewBox: ViewBox, internal val matFromAbove: ViewBox) {
    Wide(
        viewBox = ViewBox(x = 40f, y = 28f, width = 320f, height = 180f),
        matFromAbove = ViewBox(x = 64f, y = 40f, width = 272f, height = 156f),
    ),
    Tall(
        viewBox = ViewBox(x = 70f, y = 13f, width = 260f, height = 195f),
        matFromAbove = ViewBox(x = 82f, y = 24f, width = 236f, height = 178f),
    ),
}

/** [Side] is the usual mat line under the figures; [Above] is a mat seen from overhead. */
enum class DiagramView { Side, Above }

/** Where the mat is, for scenes to stand their figures on. */
private const val MAT_Y = 196f

/** The middle of every scene. */
private const val SCENE_CENTRE_X = 200f

// The mat line and the shadow under the scene, in the diagram's units.
private const val MAT_LEFT = 60f
private const val MAT_RIGHT = 340f
private const val MAT_WIDTH = 2.5f
private const val SHADOW_BELOW_MAT = 2f
private const val SHADOW_RADIUS_X = 120f
private const val SHADOW_RADIUS_Y = 6f

/** How round the corners of the mat seen from above are, and how faintly it is printed. */
private const val MAT_CORNER = 6f
private const val MAT_FROM_ABOVE_OPACITY = 0.45f

/**
 * The sheet of paper a drawing sits on: the art paper inside the printed line. In the dark theme the line is
 * dimmed with the paper, as the web app's filter covers the frame's border too.
 */
@Composable
fun ArtworkFrame(modifier: Modifier = Modifier, radius: Dp = Radius.md, content: @Composable () -> Unit) {
    val palette = BjjTheme.palette
    val outline = if (palette.isDark) dimForDark(palette.outline) else palette.outline
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(artPalette().paper)
            .border(Sizes.line, outline, shape)
            .padding(Sizes.line),
    ) {
        content()
    }
}

/**
 * A framed scene on paper, as wide as it is allowed and as tall as its [shape] makes it. [draw] works in the
 * diagram's own units, whatever size the picture is shown at. Decorative: say what it shows through [modifier].
 */
@Composable
fun Diagram(
    shape: DiagramShape,
    modifier: Modifier = Modifier,
    view: DiagramView = DiagramView.Side,
    draw: DiagramScope.() -> Unit,
) {
    val art = artPalette()
    ArtworkFrame(modifier.fillMaxWidth().aspectRatio(shape.viewBox.aspectRatio)) {
        Canvas(Modifier.fillMaxSize()) {
            val mapping = viewBoxMapping(shape.viewBox, size.width, size.height)
            withTransform({
                translate(mapping.offsetX, mapping.offsetY)
                scale(mapping.scale, mapping.scale, pivot = Offset.Zero)
            }) {
                val scope = DiagramScope(this, art, shape.viewBox)
                scope.mat(shape, view)
                scope.draw()
            }
        }
    }
}

private fun DiagramScope.mat(shape: DiagramShape, view: DiagramView) {
    when (view) {
        DiagramView.Side -> {
            val shadow = ellipsePathData(SCENE_CENTRE_X, MAT_Y + SHADOW_BELOW_MAT, SHADOW_RADIUS_X, SHADOW_RADIUS_Y)
            fill(svgPath(shadow), art.shadow)
            stroke(svgPath("M$MAT_LEFT $MAT_Y L$MAT_RIGHT $MAT_Y"), art.mat, MAT_WIDTH, cap = StrokeCap.Round)
        }

        DiagramView.Above -> {
            val mat = with(shape.matFromAbove) { svgPath(roundRectPathData(x, y, width, height, MAT_CORNER)) }
            // One layer, so the line does not show the fill through its inner half.
            layer(MAT_FROM_ABOVE_OPACITY) {
                fill(mat, art.giLightFar)
                stroke(mat, art.mat, MAT_WIDTH)
            }
        }
    }
}
