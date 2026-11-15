package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Sizes

// The look: printed like a woodblock. Flat colour, an ink line round everything you can press or open, and a
// flat ink edge underneath where another app would put a soft shadow. A thing you press sinks onto its edge.

/** A filled, outlined block of the given corner [radius], the outline drawn inside its bounds. */
fun DrawScope.block(fill: Color, outline: Color, radius: Dp, line: Dp = Sizes.line) {
    val corner = CornerRadius(radius.toPx())
    val width = line.toPx()
    drawRoundRect(fill, cornerRadius = corner)
    if (width > 0f) {
        val inset = width / 2
        drawRoundRect(
            color = outline,
            topLeft = Offset(inset, inset),
            size = Size(size.width - width, size.height - width),
            cornerRadius = CornerRadius((radius.toPx() - inset).coerceAtLeast(0f)),
            style = Stroke(width),
        )
    }
}

/**
 * Paper with an ink line round it and a flat ink [edge] underneath: a card, a tile, a picture's frame. The edge
 * is drawn below the bounds, so leave room for it (the padding comes with the modifier).
 */
@Composable
fun Modifier.woodblock(
    fill: Color = BjjTheme.palette.surface,
    outline: Color = BjjTheme.palette.outline,
    radius: Dp = dev.justinsmith.bjj.design.theme.Radius.lg,
    edge: Dp = Sizes.edge,
): Modifier {
    val ink = BjjTheme.palette.outline
    return padding(bottom = edge).drawBehind {
        if (edge > 0.dp) {
            drawRoundRect(ink, topLeft = Offset(0f, edge.toPx()), cornerRadius = CornerRadius(radius.toPx()))
        }
        block(fill, outline, radius)
    }
}

/**
 * The same block for something pressed: it sinks onto its edge while held, and wears a ring while the keyboard
 * is on it. Put it before the modifier that makes it clickable, sharing [interactions].
 */
@Composable
fun Modifier.pressBlock(
    interactions: InteractionSource,
    fill: Color = BjjTheme.palette.surface,
    outline: Color = BjjTheme.palette.borderControl,
    radius: Dp = dev.justinsmith.bjj.design.theme.Radius.md,
    edge: Dp = Sizes.edge,
): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val palette = BjjTheme.palette
    return padding(bottom = edge)
        .graphicsLayer { translationY = if (pressed) edge.toPx() else 0f }
        .drawBehind {
            if (!pressed && edge > 0.dp) {
                drawRoundRect(
                    palette.outline,
                    topLeft = Offset(0f, edge.toPx()),
                    cornerRadius = CornerRadius(radius.toPx()),
                )
            }
            block(fill, outline, radius)
            if (focused) focusRing(palette.accent, radius)
        }
}

/** The ring round whatever the keyboard is on, a little outside its bounds. */
internal fun DrawScope.focusRing(color: Color, radius: Dp) {
    val width = Sizes.focusRing.toPx()
    val gap = width * FOCUS_GAP
    drawRoundRect(
        color = color,
        topLeft = Offset(-gap, -gap),
        size = Size(size.width + gap * 2, size.height + gap * 2),
        cornerRadius = CornerRadius(radius.toPx() + gap),
        style = Stroke(width),
    )
}

/** How far outside its bounds the focus ring sits, in ring widths. */
private const val FOCUS_GAP = 1.5f

/**
 * For something tapped that has no edge to sink onto (a chip, a row, a tile): it shrinks a little while held,
 * and wears a ring while the keyboard is on it.
 */
@Composable
fun Modifier.pressShrink(
    interactions: InteractionSource,
    radius: Dp = dev.justinsmith.bjj.design.theme.Radius.md,
): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val accent = BjjTheme.palette.accent
    return graphicsLayer {
        val scale = if (pressed) PRESSED_SCALE else 1f
        scaleX = scale
        scaleY = scale
    }.drawBehind { if (focused) focusRing(accent, radius) }
}

private const val PRESSED_SCALE = 0.97f
