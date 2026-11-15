package dev.justinsmith.bjj.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * YouTube's player for one video, starting at [startSeconds]. The one thing in the app that contacts another
 * site: shown only for a study item's bookmarked video, on its page and its review card, and only when the
 * device is online. Everywhere else a bookmark is a plain link.
 */
@Composable
expect fun VideoPlayer(youtubeId: String, startSeconds: Int, title: String, modifier: Modifier = Modifier)
