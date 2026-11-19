@file:Suppress("detekt.MagicNumber") // Drawing coordinates, copied from the web app's icons.

package dev.justinsmith.bjj.design.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.graphics.circlePathData
import dev.justinsmith.bjj.design.graphics.roundRectPathData

/** The grid every icon is drawn on. */
private const val ICON_GRID = 24f

/** The width of an icon's line, on that grid. */
private const val ICON_LINE = 1.75f

/** One line icon from its SVG paths: round ends and corners, nothing filled, in black for the caller to tint. */
private fun icon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = ICON_GRID.dp,
        defaultHeight = ICON_GRID.dp,
        viewportWidth = ICON_GRID,
        viewportHeight = ICON_GRID,
    )
    for (path in paths) {
        builder.addPath(
            pathData = addPathNodes(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_LINE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

/**
 * The web app's line icons (`icons.tsx`), path for path. Always decorative: put the meaning in nearby text.
 * They are black; tint them with the colour of the text they sit in.
 */
object BjjIcons {
    val Concepts: ImageVector by lazy {
        icon(
            "Concepts",
            "M4 5.5c2.5-1 5.5-1 8 .5 2.5-1.5 5.5-1.5 8-.5v13c-2.5-1-5.5-1-8 .5-2.5-1.5-5.5-1.5-8-.5z",
            "M12 6v13",
        )
    }
    val Map: ImageVector by lazy {
        icon(
            "Map",
            circlePathData(cx = 6f, cy = 6f, r = 2.5f),
            circlePathData(cx = 18f, cy = 8f, r = 2.5f),
            circlePathData(cx = 9f, cy = 18f, r = 2.5f),
            "m8.4 6.5 7.1 1M6.6 8.4l1.8 7.2M16.3 9.9l-5.6 6.3",
        )
    }
    val Review: ImageVector by lazy {
        icon(
            "Review",
            roundRectPathData(x = 4f, y = 8f, width = 13f, height = 11f, r = 2f),
            "M8 5h10a2 2 0 0 1 2 2v9",
        )
    }
    val Video: ImageVector by lazy {
        icon("Video", roundRectPathData(x = 3f, y = 6f, width = 18f, height = 12f, r = 3f), "M10.5 9.5v5l4-2.5z")
    }
    val Backup: ImageVector by lazy { icon("Backup", "M12 4v10m0 0-3.5-3.5M12 14l3.5-3.5", "M5 16v2.5h14V16") }
    val Belt: ImageVector by lazy {
        icon(
            "Belt",
            roundRectPathData(x = 3f, y = 8f, width = 18f, height = 5f, r = 1f),
            "M10 8v5M14 8v5M10.5 13 8 20M13.5 13l2.5 7",
        )
    }
    val Home: ImageVector by lazy { icon("Home", "m4 11 8-6.5 8 6.5", "M6 9.5V19h12V9.5") }
    val Log: ImageVector by lazy {
        icon(
            "Log",
            roundRectPathData(x = 4.5f, y = 5f, width = 15f, height = 15f, r = 2f),
            "M8 3v4M16 3v4M4.5 10h15M8.5 14.5l2.3 2.3 4.7-4.6",
        )
    }
    val Flame: ImageVector by lazy {
        icon("Flame", "M12 3c1 3.5 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3.2 2-4 .2 1.6 1 2.4 1.8 2.6C10.5 9 11 5.5 12 3z")
    }
    val Check: ImageVector by lazy { icon("Check", "m5 12.5 4.5 4.5L19 7.5") }
    val Arrow: ImageVector by lazy { icon("Arrow", "M5 12h14m0 0-5-5m5 5-5 5") }
    val Back: ImageVector by lazy { icon("Back", "M19 12H5m0 0 5-5m-5 5 5 5") }
    val More: ImageVector by lazy {
        icon(
            "More",
            circlePathData(cx = 5f, cy = 12f, r = 1f),
            circlePathData(cx = 12f, cy = 12f, r = 1f),
            circlePathData(cx = 19f, cy = 12f, r = 1f),
        )
    }
    val Close: ImageVector by lazy { icon("Close", "M6 6l12 12M18 6 6 18") }
    val Chevron: ImageVector by lazy { icon("Chevron", "m9 6 6 6-6 6") }
    val Plus: ImageVector by lazy { icon("Plus", "M12 5v14M5 12h14") }
    val Search: ImageVector by lazy {
        icon("Search", circlePathData(cx = 11f, cy = 11f, r = 6.5f), "m15.8 15.8 4.2 4.2")
    }
    val List: ImageVector by lazy { icon("List", "M9 6h11M9 12h11M9 18h11", "M4.5 6h.01M4.5 12h.01M4.5 18h.01") }
}
