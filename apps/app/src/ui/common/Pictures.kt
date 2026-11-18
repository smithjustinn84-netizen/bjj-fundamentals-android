package dev.justinsmith.bjj.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.formatTime
import dev.justinsmith.bjj.design.components.ToggleChip
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.graphics.ArtworkFrame
import dev.justinsmith.bjj.design.graphics.artPalette
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.page_video
import dev.justinsmith.bjj.ui.video.VideoPlayer
import dev.justinsmith.bjj.ui.video.rememberOnline
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** The widest a video at the top of a page gets, and the narrower one on a review card. */
val PICTURE_WIDTH = 576.dp
val CARD_PICTURE_WIDTH = 416.dp

/**
 * The video at the top of a study item: its bookmarked video when it has one and there is a connection to play
 * it, otherwise nothing. With several bookmarks the first plays and the rest are a tap away.
 */
@Composable
fun ItemPicture(
    videos: List<VideoBookmark>,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = PICTURE_WIDTH,
) {
    val online = rememberOnline()
    // Nothing to show takes no space, so the card's spacing does not leave a gap where it would be.
    if (!online || videos.isEmpty()) return
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            var chosenId by rememberSaveable { mutableStateOf<String?>(null) }
            // Falls back to the first when nothing is chosen yet or the chosen bookmark has been deleted.
            val playing = videos.firstOrNull { it.id == chosenId } ?: videos.first()
            val title = stringResource(Res.string.page_video, playing.title)
            VideoOnPaper(playing, title)
            if (videos.size > 1) {
                Wrap {
                    for (video in videos) {
                        ToggleChip(
                            "${video.title} · ${formatTime(video.startSeconds)}",
                            checked = video.id == playing.id,
                            onChange = { chosenId = video.id },
                        )
                    }
                }
            }
        }
    }
}

/**
 * [video]'s player in the space it will take, on plain paper. The player is a WebView, slow to build and blank
 * until YouTube answers, so it is not made while the page is still arriving, and it stays see-through until
 * YouTube has drawn itself, so the paper shows instead of a black box, pulsing until the video is there.
 */
@Composable
internal fun VideoOnPaper(video: VideoBookmark, title: String) {
    // Keyed by the video, so picking another one brings the paper back while its player loads.
    key(video.id) {
        val still = reducedMotion()
        var settled by remember { mutableStateOf(still) }
        var ready by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!settled) {
                delay(PAGE_SETTLE_MS)
                settled = true
            }
        }
        val shown = animateFloatAsState(if (ready) 1f else 0f, if (still) snap() else tween(Motion.BASE))
        Box(Modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT)) {
            ArtworkFrame(Modifier.fillMaxSize()) { if (!ready) LoadingPulse() }
            if (settled) {
                // Inside the paper's ink line, which stays as the player's frame.
                val inside = RoundedCornerShape(Radius.md)
                Box(Modifier.fillMaxSize().padding(Sizes.line).clip(inside).graphicsLayer { alpha = shown.value }) {
                    VideoPlayer(video.youtubeId, video.startSeconds, title, onReady = { ready = true })
                }
            }
        }
    }
}

/**
 * The paper slowly darkening and lightening again, flat, while something loads on it. Still paper when
 * animations are off. Marked as a progress bar so a screen reader says it is loading.
 */
@Composable
private fun LoadingPulse() {
    val shade = artPalette().shadow
    val depth = pulseDepth()
    Box(
        Modifier
            .fillMaxSize()
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .drawBehind { drawRect(shade, alpha = depth()) },
    )
}

/** How dark the pulse is now, from 0 to 1, read while drawing so it does not recompose; 0 when animations are off. */
@Composable
private fun pulseDepth(): () -> Float {
    if (reducedMotion()) return { 0f }
    val depth = rememberInfiniteTransition()
        .animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.PULSE), RepeatMode.Reverse))
    return { depth.value }
}

/** How long a page takes to arrive (`pageEnter` in `App.kt`): building a player during it makes it stutter. */
private const val PAGE_SETTLE_MS = Motion.BASE.toLong()

/** The shape of YouTube's player. */
private const val VIDEO_ASPECT = 16f / 9f
