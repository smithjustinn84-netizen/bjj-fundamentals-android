package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.Type

/** A card: paper with an ink line round it and an ink edge underneath. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().woodblock().padding(Spacing.cardPad),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        content = content,
    )
}

/** A hairline across the page: between the sections of a card, between the rows of a list. */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(Sizes.hairline).background(BjjTheme.palette.border))
}

/** A page's title. */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    BjjText(text, modifier.semantics { heading() }, style = Type.h1)
}

/** A section's heading, with room beside it for one small button. */
@Composable
fun SectionHead(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BjjText(title, Modifier.weight(1f).semantics { heading() }, style = Type.h2)
        action()
    }
}

/** A heading and what is under it. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHead(title)
        content()
    }
}

/** The small capitals over a title or a card. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = BjjTheme.palette.muted) {
    BjjText(text.uppercase(), modifier, style = Type.eyebrow, color = color)
}

/** The quiet capitals over a group in a list. */
@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    BjjText(
        text.uppercase(),
        modifier.padding(top = Spacing.s4).semantics { heading() },
        style = Type.eyebrow,
        color = BjjTheme.palette.muted,
    )
}

/** A soft block for the user's notes, or a drill: text set apart on the accent's pale ground. */
@Composable
fun SoftBlock(text: String, modifier: Modifier = Modifier) {
    BjjText(
        text,
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(BjjTheme.palette.accentSoft)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
    )
}

/** A solid block naming a type or a state, in its colour. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier, onColor: Color = BjjTheme.palette.onStatus) {
    BjjText(
        text.uppercase(),
        modifier.clip(
            RoundedCornerShape(Radius.sm),
        ).background(color).padding(horizontal = 7.dpTag, vertical = 2.dpTag),
        style = Type.tag,
        color = onColor,
    )
}

private val Int.dpTag get() = androidx.compose.ui.unit.Dp(toFloat())

/** What went wrong, or what just went right, said in a tinted block. */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    val color = if (isError) BjjTheme.palette.danger else BjjTheme.palette.ok
    BjjText(
        text,
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(color.copy(alpha = NOTICE_TINT))
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
        color = color,
    )
}

private const val NOTICE_TINT = 0.12f

/** A bar filling as something is learned: an ink-lined well with the learned colour poured in. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val palette = BjjTheme.palette
    Box(
        modifier.fillMaxWidth().height(Sizes.bar).drawBehind {
            block(palette.well, palette.outline, Radius.pill)
            val line = Sizes.line.toPx()
            val width = (size.width - line * 2) * fraction.coerceIn(0f, 1f)
            if (width > 0f) {
                drawRoundRect(
                    palette.ok,
                    topLeft = androidx.compose.ui.geometry.Offset(line, line),
                    size = androidx.compose.ui.geometry.Size(width, size.height - line * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                )
            }
        },
    )
}
