package dev.justinsmith.bjj.platform

import androidx.compose.runtime.Composable

/** The JVM only runs tests, which have no system bars. */
@Composable
actual fun SystemBars(dark: Boolean) = Unit
