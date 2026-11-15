package dev.justinsmith.bjj.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import dev.justinsmith.bjj.design.theme.LocalContentColor
import dev.justinsmith.bjj.design.theme.Sizes
import dev.justinsmith.bjj.design.theme.Type

/** Text in the app's styles. Its colour is the content colour where it stands unless [color] says otherwise. */
@Composable
fun BjjText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    val ink = if (color.isSpecified) color else LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = ink, textAlign = textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A line icon, tinted like the text beside it. [description] is null for one that only decorates. */
@Composable
fun Icon(
    icon: ImageVector,
    description: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = Sizes.icon,
) {
    val ink = if (tint.isSpecified) tint else LocalContentColor.current
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = description,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(ink),
    )
}
