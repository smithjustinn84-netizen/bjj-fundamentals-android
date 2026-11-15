package dev.justinsmith.bjj.ui.video

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.justinsmith.bjj.core.embedUrl

/**
 * The player is YouTube's own page in a WebView. It is wrapped in a page of ours so the frame arrives with a
 * referrer, which YouTube's player asks for before it will play. Keyed by the video, so picking another one
 * tears the old player down and it stops.
 */
@SuppressLint("SetJavaScriptEnabled") // YouTube's player is a script; the page loaded is ours and fixed.
@Composable
actual fun VideoPlayer(youtubeId: String, startSeconds: Int, title: String, modifier: Modifier) {
    key(youtubeId, startSeconds) {
        AndroidView(
            modifier = modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    contentDescription = title
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    loadDataWithBaseURL(
                        PAGE_ORIGIN,
                        playerPage(embedUrl(youtubeId, startSeconds)),
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

private fun playerPage(src: String): String =
    """
    <!doctype html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="referrer" content="strict-origin-when-cross-origin">
    <style>html, body { margin: 0; height: 100%; background: #000; } iframe { border: 0; width: 100%; height: 100%; }</style>
    </head>
    <body>
    <iframe src="$src" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
    </body>
    </html>
    """.trimIndent()

/** The address our wrapping page claims: the app's id as a host, which is what the player sees as its referrer. */
private const val PAGE_ORIGIN = "https://dev.justinsmith.bjj"

private const val VIDEO_ASPECT = 16f / 9f
