package dev.justinsmith.bjj.ui.video

import androidx.compose.runtime.Composable

/** The JVM only runs tests, which have no player to show: they see the drawings. */
@Composable
actual fun rememberOnline(): Boolean = false
