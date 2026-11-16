package dev.justinsmith.bjj.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.formatTime
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.ToggleChip
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.graphics.ArtworkFrame
import dev.justinsmith.bjj.design.graphics.PositionArtwork
import dev.justinsmith.bjj.design.graphics.TapArtwork
import dev.justinsmith.bjj.design.graphics.TransitionArrow
import dev.justinsmith.bjj.design.reducedMotion
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Motion
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.page_tap
import dev.justinsmith.bjj.resources.page_video
import dev.justinsmith.bjj.ui.video.VideoPlayer
import dev.justinsmith.bjj.ui.video.rememberOnline
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** The widest a picture at the top of a page gets, and the narrower one on a review card. */
val PICTURE_WIDTH = 576.dp
val CARD_PICTURE_WIDTH = 416.dp

/** A technique drawn as its two ends: the position it starts from and the one it reaches, or a tap. */
@Composable
fun TransitionArtwork(picture: TechniquePicture, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            PositionArtwork(picture.fromId)
            Caption(picture.fromName)
        }
        TransitionArrow(BjjTheme.palette.typeColor(picture.type), Modifier.width(ARROW_WIDTH))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            if (picture.toId != null) PositionArtwork(picture.toId) else TapArtwork()
            Caption(picture.toName ?: stringResource(Res.string.page_tap))
        }
    }
}

private val ARROW_WIDTH = 36.dp

@Composable
private fun Caption(text: String) {
    BjjText(
        text,
        Modifier.fillMaxWidth(),
        style = Type.small,
        color = BjjTheme.palette.muted,
        textAlign = TextAlign.Center,
    )
}

/**
 * The picture at the top of a study item: its bookmarked video when it has one and there is a connection to
 * play it, otherwise [drawing]. With several bookmarks the first plays and the rest are a tap away.
 */
@Composable
fun ItemPicture(
    videos: List<VideoBookmark>,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = PICTURE_WIDTH,
    drawing: @Composable () -> Unit,
) {
    val online = rememberOnline()
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            if (online && videos.isNotEmpty()) {
                var chosenId by rememberSaveable { mutableStateOf<String?>(null) }
                // Falls back to the first when nothing is chosen yet or the chosen bookmark has been deleted.
                val playing = videos.firstOrNull { it.id == chosenId } ?: videos.first()
                val title = stringResource(Res.string.page_video, playing.title)
                VideoOverDrawing(playing, title, drawing)
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
            } else {
                drawing()
            }
        }
    }
}

/**
 * [video]'s player in the space it will take, over [drawing] on paper. The player is a WebView, slow to build
 * and blank until YouTube answers, so it is not made while the page is still arriving and it stays hidden
 * until its page has loaded. Even then YouTube takes a moment to draw itself, and the player is see-through
 * until it has, so the drawing stays underneath for good rather than being taken away at a guess. A drawing
 * that is not the video's shape sits in the middle of the paper, and an item with no drawing shows the paper.
 */
@Composable
internal fun VideoOverDrawing(video: VideoBookmark, title: String, drawing: @Composable () -> Unit) {
    // Keyed by the video, so picking another one brings the drawing back while its player loads.
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
        val shown by animateFloatAsState(if (ready) 1f else 0f, if (still) snap() else tween(Motion.BASE))
        Box(Modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT), contentAlignment = Alignment.Center) {
            ArtworkFrame(Modifier.fillMaxSize()) {}
            Box(Modifier.shrunkToFit()) { drawing() }
            if (settled) {
                // Inside the paper's ink line, which stays as the player's frame.
                val inside = RoundedCornerShape(Radius.md)
                Box(Modifier.fillMaxSize().padding(Sizes.line).clip(inside).alpha(shown)) {
                    VideoPlayer(video.youtubeId, video.startSeconds, title, onReady = { ready = true })
                }
            }
        }
    }
}

/**
 * Lays its content out as wide as it would be anyway and, when that comes out taller than the space there is,
 * draws it smaller so the whole of it shows in the middle. A drawing sizes itself from its width, so one
 * taller than a video would otherwise spill over the text below.
 */
private fun Modifier.shrunkToFit(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
    val scale = minOf(1f, constraints.maxHeight / placeable.height.coerceAtLeast(1).toFloat())
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeWithLayer(
            x = (constraints.maxWidth - placeable.width) / 2,
            y = (constraints.maxHeight - placeable.height) / 2,
        ) {
            scaleX = scale
            scaleY = scale
        }
    }
}

/** How long a page takes to arrive (`pageEnter` in `App.kt`): building a player during it makes it stutter. */
private const val PAGE_SETTLE_MS = Motion.BASE.toLong()

/** The shape of YouTube's player. */
private const val VIDEO_ASPECT = 16f / 9f
