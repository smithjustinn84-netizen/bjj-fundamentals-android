package dev.justinsmith.bjj.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import dev.justinsmith.bjj.design.theme.Breakpoints

/** The window's width, read live so a layout follows a resize, a fold or a turn. */
@Composable
fun windowWidth(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

/** Whether the bar is a rail down the left: a tablet, an unfolded foldable, a phone on its side. */
@Composable
fun hasRail(): Boolean = windowWidth() >= Breakpoints.rail

/** Whether there is room for two columns, or two things side by side. */
@Composable
fun isWide(): Boolean = windowWidth() >= Breakpoints.wide
