package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import dev.justinsmith.bjj.design.theme.Breakpoints
import dev.justinsmith.bjj.design.theme.Spacing

/** The window's width, read live so a layout follows a resize, a fold or a turn. */
@Composable
fun windowWidth(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

/** Whether the bar is a rail down the left: a tablet, an unfolded foldable, a phone on its side. */
@Composable
fun hasRail(): Boolean = windowWidth() >= Breakpoints.rail

/** Whether there is room for two columns, or two things side by side. */
@Composable
fun isWide(): Boolean = windowWidth() >= Breakpoints.wide

/** Whether the rail has room for the brand and a label beside each icon. */
@Composable
fun hasWideRail(): Boolean = windowWidth() >= Breakpoints.railWide

/**
 * A list that runs down the page on a phone and in two columns where there is room, filled row by row so the
 * reading order stays the list's own.
 */
@Composable
fun <T> Columns(
    items: List<T>,
    modifier: Modifier = Modifier,
    spacing: Dp = Spacing.s2,
    item: @Composable (T) -> Unit,
) {
    val perRow = if (isWide()) 2 else 1
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        for (row in items.chunked(perRow)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.Top) {
                for (entry in row) Box(Modifier.weight(1f)) { item(entry) }
                // A last row with one thing in it keeps that thing a column wide.
                repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Two things side by side where there is room, the first above the second where there is not.
 *
 * Both widths compose the same tree and only place it differently. What a page saves (a half-filled form, an open
 * picker) is saved by its place in the tree, so a tree that changed shape with the width would bring it back in the
 * wrong place after the window turns or is resized.
 */
@Composable
fun SideBySide(first: @Composable () -> Unit, second: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val wide = isWide()
    Layout({
        Box { first() }
        Box { second() }
    }, modifier.fillMaxWidth()) { (a, b), constraints ->
        val gap = Spacing.s4.roundToPx()
        val width = constraints.maxWidth
        val each = if (wide) (width - gap) / 2 else width
        val cell = Constraints(maxWidth = each, maxHeight = constraints.maxHeight)
        val one = a.measure(cell)
        val two = b.measure(cell)
        if (wide) {
            layout(width, maxOf(one.height, two.height)) {
                one.place(0, 0)
                two.place(width - each, 0)
            }
        } else {
            layout(width, one.height + gap + two.height) {
                one.place(0, 0)
                two.place(0, one.height + gap)
            }
        }
    }
}
