package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.justinsmith.bjj.design.theme.BjjTheme
import dev.justinsmith.bjj.design.theme.LocalContentColor
import dev.justinsmith.bjj.design.theme.Radius
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Spacing
import dev.justinsmith.bjj.design.theme.TextSizes
import dev.justinsmith.bjj.design.theme.Type

/** What a button is for, which decides its colours. A screen has at most one [Cta]. */
enum class ButtonKind { Plain, Primary, Cta, Danger }

enum class ButtonSize { Small, Normal, Large }

private class ButtonColors(val fill: Color, val outline: Color, val content: Color)

@Composable
private fun ButtonKind.colors(): ButtonColors {
    val p = BjjTheme.palette
    return when (this) {
        ButtonKind.Plain -> ButtonColors(p.surface, p.borderControl, p.text)
        ButtonKind.Primary -> ButtonColors(p.fill, p.outline, p.onFill)
        ButtonKind.Cta -> ButtonColors(p.cta, p.outline, p.onCta)
        ButtonKind.Danger -> ButtonColors(p.surface, p.borderControl, p.danger)
    }
}

private val ButtonSize.padding: PaddingValues
    get() = when (this) {
        ButtonSize.Small -> PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ButtonSize.Normal -> PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ButtonSize.Large -> PaddingValues(horizontal = 22.dp, vertical = 11.dp)
    }

private fun textStyle(kind: ButtonKind, size: ButtonSize): TextStyle {
    val loud = kind == ButtonKind.Primary || kind == ButtonKind.Cta || size == ButtonSize.Large
    val base = Type.button.copy(fontWeight = if (loud) FontWeight.ExtraBold else FontWeight.Bold)
    return when (size) {
        ButtonSize.Small -> base.copy(fontSize = TextSizes.sm)
        ButtonSize.Normal -> base
        ButtonSize.Large -> base.copy(fontSize = TextSizes.lg)
    }
}

/** A button: paper with an ink line and an ink edge it sinks onto when pressed. */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Plain,
    size: ButtonSize = ButtonSize.Normal,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = kind.colors()
    val interactions = remember { MutableInteractionSource() }
    // The one loud button stands taller off the page.
    val edge: Dp = if (kind == ButtonKind.Cta) Sizes.edgeLift else Sizes.edge
    Row(
        modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressBlock(interactions, colors.fill, colors.outline, Radius.md, edge)
            .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Sizes.tap)
            .padding(size.padding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            if (icon != null) Icon(icon, description = null, size = Sizes.iconSmall)
            BjjText(text, style = textStyle(kind, size), maxLines = 1)
        }
    }
}

private const val DISABLED_ALPHA = 0.5f

/** A button that looks like a link: for the quiet thing beside a real button. */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val interactions = remember { MutableInteractionSource() }
    val color = if (danger) BjjTheme.palette.danger else BjjTheme.palette.accent
    Row(
        modifier
            .pressShrink(interactions)
            .clickable(interactions, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Sizes.tap)
            .padding(horizontal = Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BjjText(text, style = Type.strong.copy(textDecoration = TextDecoration.Underline), color = color)
    }
}
