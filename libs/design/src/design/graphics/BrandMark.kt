package dev.justinsmith.bjj.design.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.theme.BjjTheme

// The logo, in the units of the web app's drawing (BrandMark.tsx): 34 wide and 22 tall.
private const val MARK_WIDTH = 34f
private const val MARK_HEIGHT = 22f

/** The two tails hanging from the knot. */
private const val MARK_TAILS_PATH = "m15.5 9-4.5 10 3.8 1.5L18 11zM18.5 9l4.5 10-3.8 1.5L16 11z"

private const val MARK_BAND_Y = 5f
private const val MARK_BAND_HEIGHT = 6f
private val MARK_BAND_PATH =
    roundRectPathData(x = 1f, y = MARK_BAND_Y, width = 32f, height = MARK_BAND_HEIGHT, r = 1.5f)
private val MARK_KNOT_PATH = roundRectPathData(x = 13.5f, y = 3f, width = 7f, height = 10f, r = 2f)

/** The red rank bar: a square of the band. */
private const val MARK_BAR_X = 24f

/**
 * The app's logo: a small tied belt in the text colour with a red rank bar, so it follows the theme. As big as
 * the web app's unless [modifier] sets a width.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val palette = BjjTheme.palette
    Canvas(modifier.width(MARK_WIDTH.dp).aspectRatio(MARK_WIDTH / MARK_HEIGHT)) {
        scale(size.width / MARK_WIDTH, pivot = Offset.Zero) {
            drawPath(svgPath(MARK_TAILS_PATH), palette.text)
            drawPath(svgPath(MARK_BAND_PATH), palette.text)
            drawRect(palette.belt, Offset(MARK_BAR_X, MARK_BAND_Y), Size(MARK_BAND_HEIGHT, MARK_BAND_HEIGHT))
            drawPath(svgPath(MARK_KNOT_PATH), palette.text)
        }
    }
}
