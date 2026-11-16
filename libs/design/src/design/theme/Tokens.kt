package dev.justinsmith.bjj.design.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Every size, radius and duration a screen uses comes from here: never a raw dp or sp in a screen.

object Spacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp

    /** Between the page's content and the sides of a phone. */
    val pageGutter = 14.dp

    /** The same space on a wider window, where there is room to breathe. */
    val gutterWide = 20.dp

    /** Padding inside a card. */
    val cardPad = 20.dp
}

object Radius {
    val sm = 4.dp
    val md = 8.dp
    val lg = 14.dp

    /** Fully round ends, whatever the height. */
    val pill = 999.dp
}

object Sizes {
    /** The printed line's width. */
    val line = 1.5.dp

    /** Hairlines. */
    val hairline = 1.dp

    /** The flat ink edge under a card or button, and the taller one under whatever matters most. */
    val edge = 2.dp
    val edgeLift = 4.dp

    /** The smallest height of anything meant to be tapped. */
    val tap = 48.dp

    /** A line icon beside text, and the smaller one inside a tag or a small button. */
    val icon = 20.dp
    val iconSmall = 16.dp

    /** A list's picture, and the dot that says how well a card is known. */
    val thumb = 72.dp
    val dot = 10.dp

    /** A progress bar's height. */
    val bar = 12.dp

    /** The keyboard-focus ring's width and its gap from what it rings. */
    val focusRing = 2.dp

    /** The bottom bar's height, before the system's inset. */
    val tabBar = 60.dp

    /** The rail's width with icons alone, and with the brand and labels beside them. */
    val rail = 80.dp
    val railWide = 216.dp

    /** The widest the page's content gets, and the widest a page of prose or a form gets. */
    val contentMax = 1200.dp
    val readingMax = 680.dp
}

/** The window widths a layout changes at. */
object Breakpoints {
    /** The bottom bar becomes a rail down the left. */
    val rail: Dp = 600.dp

    /** Lists run in two columns, and pages with two things to show put them side by side. */
    val wide: Dp = 840.dp

    /** The rail widens to show the brand and labels. */
    val railWide: Dp = 1200.dp
}

object TextSizes {
    val xs = 12.sp
    val sm = 14.sp
    val md = 16.sp
    val lg = 18.sp
    val xl = 32.sp
    val display = 34.sp
}

/** Durations in milliseconds. */
object Motion {
    const val FAST = 120
    const val BASE = 220
    const val SLOW = 480

    /** Drawing a line in: map arrows, then the slower brush stroke of the ensō. */
    const val DRAW = 700
    const val DRAW_SLOW = 900

    /** One loop of the dashes flowing along a highlighted map arrow. */
    const val FLOW = 600

    /** A number counting up to its value on Today. */
    const val COUNT_UP = 700

    /** Half a breath of a placeholder pulsing while its content loads: darkening, or lightening again. */
    const val PULSE = 900
}
