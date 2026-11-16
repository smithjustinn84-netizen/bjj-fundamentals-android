package dev.justinsmith.bjj.ui.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The JVM only runs tests, which have no player: the space a video takes, named so a test can find it, and
 * ready as soon as it is shown.
 */
@Composable
actual fun VideoPlayer(youtubeId: String, startSeconds: Int, title: String, onReady: () -> Unit, modifier: Modifier) {
    val ready by rememberUpdatedState(onReady)
    LaunchedEffect(Unit) { ready() }
    Box(modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT).semantics { contentDescription = title })
}

private const val VIDEO_ASPECT = 16f / 9f
