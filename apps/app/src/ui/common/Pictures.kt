package dev.justinsmith.bjj.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.core.Position
import dev.justinsmith.bjj.core.Transition
import dev.justinsmith.bjj.core.VideoBookmark
import dev.justinsmith.bjj.core.endsTheRoll
import dev.justinsmith.bjj.core.formatTime
import dev.justinsmith.bjj.design.components.BjjText
import dev.justinsmith.bjj.design.components.ToggleChip
import dev.justinsmith.bjj.design.components.Wrap
import dev.justinsmith.bjj.design.graphics.PositionArtwork
import dev.justinsmith.bjj.design.graphics.TapArtwork
import dev.justinsmith.bjj.design.graphics.TransitionArrow
import dev.justinsmith.bjj.design.graphics.hasPositionArtwork
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type
import dev.justinsmith.bjj.design.theme.typeColor
import dev.justinsmith.bjj.resources.Res
import dev.justinsmith.bjj.resources.page_tap
import dev.justinsmith.bjj.resources.page_video
import dev.justinsmith.bjj.ui.video.VideoPlayer
import dev.justinsmith.bjj.ui.video.rememberOnline
import org.jetbrains.compose.resources.stringResource

/** The widest a picture at the top of a page gets, and the narrower one on a review card. */
val PICTURE_WIDTH = 576.dp
val CARD_PICTURE_WIDTH = 416.dp

/** Whether a technique can be drawn: both its ends have a picture, or it starts at one and ends in a tap. */
fun hasTransitionArtwork(transition: Transition, positionById: Map<String, Position>): Boolean {
    val from = positionById[transition.fromId] ?: return false
    if (!hasPositionArtwork(from.id)) return false
    if (endsTheRoll(transition.type)) return true
    return positionById[transition.toId]?.let { hasPositionArtwork(it.id) } == true
}

/**
 * A technique drawn as its two ends: the position it starts from and the one it reaches, or a tap for a
 * submission. Draws nothing when either end has no picture, such as a position the user added.
 */
@Composable
fun TransitionArtwork(transition: Transition, positionById: Map<String, Position>, modifier: Modifier = Modifier) {
    if (!hasTransitionArtwork(transition, positionById)) return
    val from = positionById.getValue(transition.fromId)
    // Null when the technique ends the roll: its picture is a tap instead.
    val to = if (endsTheRoll(transition.type)) null else positionById[transition.toId]
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            PositionArtwork(from.id)
            Caption(from.name)
        }
        TransitionArrow(BjjTheme.palette.typeColor(transition.type), Modifier.width(ARROW_WIDTH))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            if (to != null) PositionArtwork(to.id) else TapArtwork()
            Caption(to?.name ?: stringResource(Res.string.page_tap))
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
                VideoPlayer(playing.youtubeId, playing.startSeconds, title)
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
