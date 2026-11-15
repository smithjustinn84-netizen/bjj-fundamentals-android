package dev.justinsmith.bjj.platform

import androidx.compose.runtime.Composable

/**
 * Keeps the system's status and navigation icons readable over the app's theme, which may not be the device's:
 * dark icons on the light paper, light ones on the dark.
 */
@Composable
expect fun SystemBars(dark: Boolean)
