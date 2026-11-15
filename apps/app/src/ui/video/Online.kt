package dev.justinsmith.bjj.ui.video

import androidx.compose.runtime.Composable

/** Whether the device believes it has a connection, kept up to date. Offline, a video gives way to its drawing. */
@Composable
expect fun rememberOnline(): Boolean
