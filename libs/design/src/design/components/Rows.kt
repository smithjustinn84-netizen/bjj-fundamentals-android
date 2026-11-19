@file:OptIn(ExperimentalLayoutApi::class)

package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type

/** How well a card is known, as far as a list needs to say. */
enum class Progress { New, Learning, Learned }

/** A dot saying how well a card is known: hollow for new, then the learning and learned colours. */
@Composable
fun ProgressDot(progress: Progress, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    val color = when (progress) {
        Progress.New -> Color.Transparent
        Progress.Learning -> p.warn
        Progress.Learned -> p.ok
    }
    val ring = if (progress == Progress.New) p.border else color
    Box(
        modifier.size(Sizes.dot).drawBehind {
            val line = DOT_LINE.toPx()
            drawCircle(color)
            drawCircle(ring, radius = size.minDimension / 2 - line / 2, style = Stroke(line))
        },
    )
}

private val DOT_LINE = 2.dp

/** A card's progress in words, for the one item a page is about. */
@Composable
fun StatusPill(text: String, progress: Progress, modifier: Modifier = Modifier) {
    val p = BjjTheme.palette
    val color = when (progress) {
        Progress.New -> p.muted
        Progress.Learning -> p.warn
        Progress.Learned -> p.ok
    }
    Row(
        modifier
            .drawBehind { block(Color.Transparent, p.border, Radius.pill, line = Sizes.hairline) }
            .padding(horizontal = Spacing.s2, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressDot(progress)
        BjjText(text, style = Type.small, color = color)
    }
}

/** A row that is one link: its name, a quieter line saying what it is, and a mark where it is tapped. */
@Composable
fun RowLink(
    name: String,
    onClick: () -> Unit,
    chevron: ImageVector,
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
    meta: @Composable FlowRowScope.() -> Unit = {},
) {
    val interactions = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressShrink(interactions)
            .clickable(interactions, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Sizes.tap)
            .padding(vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BjjText(name, style = Type.strong)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
                itemVerticalAlignment = Alignment.CenterVertically,
                content = meta,
            )
        }
        Icon(chevron, description = null, tint = BjjTheme.palette.muted)
    }
}

/** A list's row as a block of its own: a picture, a name, a line about it, and how well it is known. */
@Composable
fun Tile(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String = "",
    picture: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val interactions = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressBlock(interactions, outline = BjjTheme.palette.outline, radius = Radius.lg)
            .clickable(interactions, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = TILE_HEIGHT)
            .padding(horizontal = Spacing.s3, vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (picture != null) Box(Modifier.width(Sizes.thumb)) { picture() }
        Column(Modifier.weight(1f).padding(start = if (picture == null) Spacing.s2 else 0.dp)) {
            BjjText(name, style = Type.strong)
            if (detail.isNotEmpty()) BjjText(detail, style = Type.small, color = BjjTheme.palette.muted, maxLines = 2)
        }
        trailing()
    }
}

private val TILE_HEIGHT = 68.dp

/** Things side by side that wrap onto the next line when they run out of room: chips, tags, buttons. */
@Composable
fun Wrap(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A list of points, each behind a bullet, or behind its number when [numbered]. */
@Composable
fun Bullets(items: List<String>, modifier: Modifier = Modifier, numbered: Boolean = false) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                BjjText(
                    if (numbered) "${index + 1}." else "•",
                    Modifier.width(MARKER_WIDTH),
                    style = Type.strong,
                    color = if (numbered) BjjTheme.palette.accent else BjjTheme.palette.muted,
                    textAlign = TextAlign.End,
                )
                BjjText(item, Modifier.weight(1f))
            }
        }
    }
}

private val MARKER_WIDTH = 22.dp

/** One of the two links at the foot of a page: the item before this one, or the one after. */
@Composable
fun PagerLink(
    direction: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    Column(
        modifier
            .pressShrink(interactions, Radius.lg)
            .drawBehind { block(p.surface, p.borderControl, Radius.lg) }
            .clickable(interactions, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        BjjText(direction, style = Type.small, color = p.muted)
        BjjText(name, style = Type.strong, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start)
    }
}
