package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.TextSizes
import dev.justinsmith.bjj.design.theme.Type

@Composable
private fun ChipBody(
    text: String,
    fill: Color,
    outline: Color,
    content: Color,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .drawBehind { block(fill, outline, Radius.pill) }
            .defaultMinSize(minHeight = Sizes.tap)
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        BjjText(text, style = Type.strong.copy(fontSize = TextSizes.sm), color = content)
    }
}

/** A pill that opens something: a concept, a position, a card. */
@Composable
fun Chip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: @Composable () -> Unit = {}) {
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    ChipBody(
        text,
        p.surface,
        p.borderControl,
        p.text,
        modifier.pressShrink(interactions, Radius.pill).clickable(interactions, indication = null, onClick = onClick),
        leading,
    )
}

/** A pill that is on or off: a technique practised, a place the student got stuck. On, it is painted in. */
@Composable
fun ToggleChip(text: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    ChipBody(
        text,
        fill = if (checked) p.fill else p.surface,
        outline = if (checked) p.outline else p.borderControl,
        content = if (checked) p.onFill else p.text,
        modifier = modifier
            .pressShrink(interactions, Radius.pill)
            .toggleable(checked, interactions, indication = null, role = Role.Checkbox, onValueChange = onChange),
    )
}

/**
 * A switch between a few views or answers: a sunk track with the picked segment raised out of it. The segments
 * share the width they are given.
 */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val p = BjjTheme.palette
    Row(
        modifier
            .drawBehind { block(p.well, Color.Transparent, Radius.lg, line = 0.dp) }
            .padding(SEGMENT_INSET)
            .semantics { selectableGroup() },
        horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
    ) {
        for (option in options) {
            val picked = option == selected
            val interactions = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .weight(1f)
                    .pressShrink(interactions)
                    .drawBehind { if (picked) block(p.raised, p.outline, Radius.md) }
                    .selectable(picked, interactions, indication = null, role = Role.Tab, onClick = {
                        onSelect(option)
                    })
                    .defaultMinSize(minHeight = Sizes.tap)
                    .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
                contentAlignment = Alignment.Center,
            ) {
                BjjText(
                    label(option),
                    style = Type.strong.copy(fontWeight = if (picked) FontWeight.ExtraBold else FontWeight.Medium),
                    color = if (picked) p.text else p.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

private val SEGMENT_INSET = 3.dp
private val SEGMENT_GAP = 2.dp

/** A box that is ticked or not, with what it means beside it. */
@Composable
fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val p = BjjTheme.palette
    Row(
        modifier
            .fillMaxWidth()
            .pressShrink(interactions)
            .toggleable(checked, interactions, indication = null, role = Role.Checkbox, onValueChange = onChange)
            .defaultMinSize(minHeight = Sizes.tap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(CHECK_BOX).drawBehind {
                block(if (checked) p.fill else p.bg, p.borderControl, Radius.sm)
                if (checked) {
                    // A tick, as two strokes across the box.
                    val w = size.width
                    val stroke = Stroke(w * TICK_WIDTH)
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.22f, w * 0.52f)
                        lineTo(w * 0.43f, w * 0.72f)
                        lineTo(w * 0.78f, w * 0.3f)
                    }
                    drawPath(path, p.onFill, style = stroke)
                }
            },
        )
        BjjText(text)
    }
}

private val CHECK_BOX = 22.dp
private const val TICK_WIDTH = 0.12f
