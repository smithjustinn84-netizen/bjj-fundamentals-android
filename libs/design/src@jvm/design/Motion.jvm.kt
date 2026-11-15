package dev.justinsmith.bjj.design

import androidx.compose.runtime.Composable

/** The desktop JVM only runs tests, which have no such setting. */
@Composable
actual fun reducedMotion(): Boolean = false
